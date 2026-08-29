pluginManagement {
    // convention plugin（T-28a）。included build なのでルートの buildscript classpath は見ない
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "ReBuy"
include(":androidApp")
include(":shared:data")
include(":shared:domain")
include(":shared:ui")
