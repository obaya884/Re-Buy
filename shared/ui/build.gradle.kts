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

// 画面文言と drawable は commonMain/composeResources に置き、Compose Resources が
// 生成する Res 経由で参照する。生成先を明示しないと group / artifact 由来の
// package になり、import が読みにくくなる
compose.resources {
    packageOfResClass = "$uiPackage.resources"
    // :androidApp の instrumented テストが同じ文言で画面を突き合わせるため、
    // モジュールの外から Res を触れるようにする（既定は internal）
    publicResClass = true
}

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
        // R の FQN になる。package と揃えておく
        namespace = uiPackage

        // KMP ライブラリでは Android リソースが既定で無効。有効にしないと R が生成されない。
        // 画面文言と drawable は Compose Resources へ移したので、R に残っているのは
        // AboutLibraries が生成する R.raw.aboutlibraries だけ。それを引く LicenseScreen が
        // composeResources 経由になる（ステップ 14）までは有効のままにする
        androidResources {
            enable = true
        }
    }

    sourceSets {
        // ステップ 12 で SettingScreen が commonMain へ行っても付け替えずに済むよう、
        // いま android ターゲットしか無いうちから commonMain に置く
        commonMain {
            kotlin.srcDir(generateVersionKt)

            dependencies {
                api(project(":shared:domain"))

                // uiModule を :androidApp へ公開する。koinViewModel() は画面の内側だけ
                api(libs.koin.core)
                // 上流の api 頼みにせず、この source set でも版を固定する
                implementation(project.dependencies.platform(libs.koin.bom))
                // viewModelOf。koin-android のものは Android 専用
                implementation(libs.koin.core.viewmodel)

                // ViewModel の基底。-ktx と -compose は Android 専用
                implementation(libs.androidx.lifecycle.viewmodel)

                // Compose Resources。iosMain 限定という下の規約の唯一の例外で、
                // リソースは commonMain に置く以上ここでしか宣言できない。
                // Android では androidx.compose の別名にすぎないので BOM とはぶつからない。
                // api なのは :androidApp の instrumented テストが同じ文言で画面を突き合わせるため。
                // implementation に落とすと getString / StringResource に届かずテストが通らない
                api(compose.components.resources)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.compose.viewmodel)

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
            // compose.material3 は自分より古い foundation を推移的に引くので、
            // いま使っていなくても compose.foundation を書いて版を 1.12.0 に固定する
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        // androidHostTest は withHostTest が動的に作るので型付きアクセサが無い。
        // koin-test の verify() は kotlin-reflect 依存で JVM 専用なのでここに残る
        getByName("androidHostTest").dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
        }
    }
}
