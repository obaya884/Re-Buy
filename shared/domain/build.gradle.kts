plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    // iOS ターゲット（宣言は build-logic に 1 か所）
    id("rebuy.kmp.ios")
    // Android の土台（内訳は plugin 側）
    id("rebuy.android.base")
}

kotlin {
    android {
        namespace = "io.github.obaya884.rebuy.domain"
    }

    sourceSets {
        androidMain.dependencies {
            // エンティティと DAO を上の層へ通すので api
            api(project(":shared:data"))

            // domainModule を上の層へ公開する
            api(libs.koin.core)
        }
    }
}
