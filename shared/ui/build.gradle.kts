import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    // iosMain の Compose。androidMain は androidx の BOM のままにする
    alias(libs.plugins.compose.multiplatform)
    // バージョンはルートの buildscript classpath で固定しているので、ここでは版を指定しない
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.aboutLibraries)
    // iOS ターゲット（宣言は build-logic に 1 か所）
    id("rebuy.kmp.ios")
    // Android の土台（内訳は plugin 側）
    id("rebuy.android.base")
}

val uiPackage = "io.github.obaya884.rebuy.ui"

// KMP ライブラリプラグインは BuildConfig に非対応。設定画面が出すバージョンだけのために
// BuildConfig を使っていたので、gradle.properties から Kotlin のソースを生成して置き換える
val generateVersionKt = tasks.register("generateVersionKt") {
    group = "build"
    description = "rebuy.versionName から Version.kt を生成する"
    val versionName = providers.gradleProperty("rebuy.versionName")
    val outputDir = layout.buildDirectory.dir("generated/version/kotlin")
    // doLast の中からスクリプトの val を直接見ると、設定キャッシュがスクリプト参照を
    // 直列化できずに落ちる。ここでローカルに束ねる
    val packageName = uiPackage
    val packagePath = packageName.replace('.', '/')
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("$packagePath/Version.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package $packageName

            /** `gradle.properties` の `rebuy.versionName` から生成している。直接編集しない。 */
            internal const val VERSION_NAME: String = "${versionName.get()}"
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    // iOS 側へ出す framework。baseName が Swift から見える名前になる。
    // ターゲットの宣言は rebuy.kmp.ios にある。
    // debug のみ。release は実機配布が要る段 4 で足す（そのとき Gradle のヒープを上げる。経緯は log_23）
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework(listOf(NativeBuildType.DEBUG)) {
            baseName = "ReBuyUi"
            // static。Xcode 側は framework の埋め込みコピーが要らなくなる
            isStatic = true
        }
    }

    android {
        // R の FQN になる。package と揃えることで、
        // 画面文言を使う側が「どのモジュールのリソースか」を import で読める
        namespace = uiPackage

        // KMP ライブラリでは Android リソースが既定で無効。有効にしないと R が生成されない
        androidResources {
            enable = true
        }
    }

    sourceSets {
        // ステップ 12 で SettingScreen が commonMain へ行っても付け替えずに済むよう、
        // いま android ターゲットしか無いうちから commonMain に置く
        commonMain { kotlin.srcDir(generateVersionKt) }

        androidMain {
            dependencies {
                api(project(":shared:domain"))

                // uiModule を :androidApp へ公開する。koinViewModel() は画面の内側だけ
                api(libs.koin.core)
                implementation(libs.koin.compose.viewmodel)

                // Lifecycle
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.lifecycle.viewmodel.ktx)

                // Navigation。Screen が NavKey を継ぎ、Navigator も NavKey を公開する
                api(libs.androidx.navigation3.runtime)
                implementation(libs.androidx.navigation3.ui)
                implementation(libs.androidx.lifecycle.viewmodel.navigation3)
                implementation(libs.kotlinx.serialization.json)

                // Compose
                // 公開シグネチャに出る型があるものを api にする
                //   compose.ui: BottomNavigationItem の ImageVector
                //   material3: 画面 Composable が受け取る SnackbarHostState
                // sourceSets の中では platform() が生えないので project.dependencies から呼ぶ
                api(project.dependencies.platform(libs.androidx.compose.bom))
                api(libs.androidx.compose.ui)
                implementation(libs.androidx.compose.foundation)
                implementation(libs.androidx.compose.ui.tooling.preview)
                api(libs.androidx.compose.material3)
                implementation(libs.androidx.compose.material.icons.core)

                // OSS ライセンス表示
                implementation(libs.aboutlibraries.core)
                implementation(libs.aboutlibraries.compose.core)
                implementation(libs.aboutlibraries.compose.m3)
            }
        }

        iosMain.dependencies {
            // compose.*（Compose Multiplatform）は iosMain にだけ置く。androidMain や
            // commonMain に置いても通ってしまい、:androidApp の androidx BOM とずれる
            // （落とし穴 12）。逆に androidx を iosMain へ置く方向は解決に失敗するので気づける。
            // ステップ 12 で画面が commonMain へ行けば :shared:ui は CMP 一本になり、この規約は消える
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.ui)
        }

        // androidHostTest は withHostTest が動的に作るので型付きアクセサが無い
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
        }
    }
}
