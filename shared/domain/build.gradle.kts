plugins {
    alias(libs.plugins.android.library)
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "io.github.obaya884.rebuy.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // エンティティと DAO を上の層へ通すので api
    api(project(":shared:data"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
}
