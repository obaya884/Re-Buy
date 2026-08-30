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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

    }

    sourceSets {
        // Adds exported schema location as test app assets.
        named("androidTest") {
            // :shared:data が出力するスキーマを RoomMigrationTest の assets として渡す。
            // config cache が有効なので project(":shared:data").projectDir とは書かない
            assets.directories.add("$rootDir/shared/data/schemas")
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
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.room.testing)
    // LicenseLibrariesTest が画面と同じ Libs で JSON を読む。:shared:ui では
    // implementation なので推移的には来ない
    androidTestImplementation(libs.aboutlibraries.core)
}
