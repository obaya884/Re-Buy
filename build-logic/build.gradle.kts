plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // 実行時に効くのはルートの buildscript classpath 側なので compileOnly。
    // 版がずれるとコンパイルは通って実行時に NoSuchMethodError になるので、catalog と同じ版で宣言する
    compileOnly(libs.android.gradle.plugin)
    // KMP ターゲットの設定に KotlinMultiplatformExtension と JvmTarget を使う
    compileOnly(libs.kotlin.gradle.plugin)
}
