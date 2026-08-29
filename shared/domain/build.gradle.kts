plugins {
    alias(libs.plugins.android.library)
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "io.github.obaya884.rebuy.domain"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // エンティティと DAO を上の層へ通すので api
    api(project(":shared:data"))

    // domainModule を上の層へ公開する
    api(libs.koin.core)
}
