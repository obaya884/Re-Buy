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
    // いまのプラグインは AGP の型しか使わない。KGP は T-28b で KMP のターゲット定義を書くときに要る
    compileOnly(libs.kotlin.gradle.plugin)
}
