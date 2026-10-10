plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("rebuy.android.base")
}

android {
    namespace = "io.github.obaya884.rebuy"

    defaultConfig {
        applicationId = "io.github.obaya884.rebuy"
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = providers.gradleProperty("rebuy.versionName").get()

        // DB と設定値をメモリ上に差し替えた Application で走らせる（T-21）
        testInstrumentationRunner = "io.github.obaya884.rebuy.ReBuyTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        // :shared:data の withDeviceTest に同じ端末の定義がある。変えるときは両方を揃える
        // （名前がずれると pixel6Api35Check が片方を拾わなくなる）
        managedDevices {
            localDevices {
                create("pixel6Api35") {
                    device = "Pixel 6"
                    apiLevel = 35
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(project(":shared:ui"))

    // startKoin と androidContext
    implementation(libs.koin.android)

    // MainActivity の setContent
    implementation(libs.androidx.activity.compose)

    // Test
    // TestReBuyApplication が in-memory の AppDatabase を組み立てる
    androidTestImplementation(libs.androidx.room.runtime)
    // ReBuyTestRunner の親
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // LicenseLibrariesTest が画面と同じ Libs で JSON を読む。:shared:ui では
    // implementation なので推移的には来ない
    androidTestImplementation(libs.aboutlibraries.core)
}
