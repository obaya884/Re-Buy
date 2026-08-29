plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    // バージョンはルートの buildscript classpath で固定しているので、ここでは版を指定しない
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.aboutLibraries)
    // Android の土台（内訳は plugin 側）
    id("rebuy.android.base")
}

// KMP ライブラリプラグインは BuildConfig に非対応。設定画面が出すバージョンだけのために
// BuildConfig を使っていたので、gradle.properties から Kotlin のソースを生成して置き換える
val generateVersion = tasks.register("generateVersionKt") {
    val versionName = providers.gradleProperty("rebuy.versionName")
    val outputDir = layout.buildDirectory.dir("generated/version/kotlin")
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/obaya884/rebuy/ui/Version.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package io.github.obaya884.rebuy.ui

            /** `gradle.properties` の `rebuy.versionName` から生成している。直接編集しない。 */
            internal const val VERSION_NAME: String = "${versionName.get()}"
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    android {
        // R の FQN になる。package と揃えることで、
        // 画面文言を使う側が「どのモジュールのリソースか」を import で読める
        namespace = "io.github.obaya884.rebuy.ui"

        // KMP ライブラリでは Android リソースが既定で無効。有効にしないと R が生成されない
        androidResources {
            enable = true
        }
    }

    sourceSets {
        androidMain {
            kotlin.srcDir(generateVersion)

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

        // androidHostTest は withHostTest が動的に作るので型付きアクセサが無い
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
        }
    }
}
