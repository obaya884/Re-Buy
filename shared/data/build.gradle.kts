plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    id("rebuy.android.base")
}

android {
    namespace = "io.github.obaya884.rebuy.data"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    // DAO の戻り値が Flow なので、公開 API として上の層へ通す
    api(libs.kotlinx.coroutines.core)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // dataModule（Koin の Module 型）を上の層へ公開する
    api(platform(libs.koin.bom))
    api(libs.koin.core)
    // androidContext() は DataModule の内側だけで使う
    implementation(libs.koin.android)

    testImplementation(libs.junit)
}
