plugins {
    alias(libs.plugins.android.library)
    id("rebuy.android.base")
}

android {
    namespace = "io.github.obaya884.rebuy.domain"
}

dependencies {
    // エンティティと DAO を上の層へ通すので api
    api(project(":shared:data"))

    // domainModule を上の層へ公開する
    api(libs.koin.core)
}
