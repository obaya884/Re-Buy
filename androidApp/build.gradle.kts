plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "io.github.obaya884.rebuy.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.obaya884.rebuy"
        minSdk = 31
        targetSdk = 35
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
            assets.directories.add("$projectDir/../shared/data/schemas")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    implementation(libs.androidx.core.ktx)

    // Koin（startKoin と androidContext）
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // Activity（MainActivity の setContent）
    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)

    /**
     * Test
     */
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.room.testing)
}
