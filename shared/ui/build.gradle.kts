import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    // Compose Multiplatform。:shared:ui の Compose はここから来る
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

                // Compose Multiplatform。androidx の Compose ではなくこちらを使う。
                // Android では JetBrains の別名アーティファクトが androidx へ解決するので、
                // :androidApp が持つ androidx の BOM とはぶつからない。
                // :androidApp と instrumented テストが直に触る型を出すものを api にする
                //   compose.ui: BottomNavigationItem の ImageVector
                //   material3: 画面 Composable が受け取る SnackbarHostState
                // runtime と foundation はこの 2 つが api で推移的に引くので implementation
                implementation(compose.runtime)
                api(compose.ui)
                api(compose.material3)
                implementation(compose.foundation)
                implementation(compose.preview)

                // 画面文言と drawable。api なのは :androidApp の instrumented テストが
                // 同じ文言で画面を突き合わせるため。implementation に落とすと
                // getString / StringResource に届かずテストが通らない
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

                // Compose 本体は commonMain の CMP から来る。ここに残るのは
                // material-icons-core（Icons.Default.* / Icons.AutoMirrored.*）だけ。
                // CMP に対応物が無く、material3 も推移的には引かないので androidx から取る。
                // 版を決めるためだけに BOM を残している。アイコンを使う画面は
                // ステップ 13 で commonMain へ行くので、置き換え方はそこで決める。
                // sourceSets の中では platform() が生えないので project.dependencies から呼ぶ
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.compose.material.icons.core)

                // OSS ライセンス表示
                implementation(libs.aboutlibraries.core)
                implementation(libs.aboutlibraries.compose.core)
                implementation(libs.aboutlibraries.compose.m3)
            }
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
