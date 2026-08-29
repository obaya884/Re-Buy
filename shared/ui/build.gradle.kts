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
    // 画面文言が入る R をアプリと同じ FQN で持つ。android.nonTransitiveRClass=true なので
    // ここを分けると本番 9 ファイルと instrumented 2 ファイルの import が変わる
    namespace = "io.github.obaya884.rebuy"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
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

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    api(libs.koin.compose.viewmodel)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    // Compose
    api(composeBom)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.compose.material.icons.core)

    // OSS ライセンス表示
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.core)
    implementation(libs.aboutlibraries.compose.m3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
}
