plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    // バージョンはルートの buildscript classpath で固定しているので、ここでは版を指定しない
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.aboutLibraries)
}

repositories {
    google()
    mavenCentral()
}

android {
    // R と BuildConfig の FQN になる。package と揃えることで、
    // 画面文言を使う側が「どのモジュールのリソースか」を import で読める
    namespace = "io.github.obaya884.rebuy.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // ライブラリの BuildConfig には versionName が生えないので、自分で持たせる
        buildConfigField(
            "String",
            "VERSION_NAME",
            "\"${providers.gradleProperty("rebuy.versionName").get()}\""
        )
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

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
    api(composeBom)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    // OSS ライセンス表示
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.core)
    implementation(libs.aboutlibraries.compose.m3)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
}
