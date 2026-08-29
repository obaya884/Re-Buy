import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// 4 モジュールで同じ Android の土台（リポジトリ・SDK レベル・Java 互換・ホストテストの有効化）を
// ここに集める。モジュール側で書き忘れても効くので、モジュールを増やしたときのずれが起きない。
//
// このプラグインは AGP を適用しない。適用されたのを見て設定するだけなので、モジュール側の
// alias(libs.plugins.android.application) / .library は引き続き要る。
// precompiled script plugin の plugins {} に id を書くと、その版を build-logic の
// implementation 依存として要求することになり、「実行時はルート側の版を使う」ための
// compileOnly と両立しない。そのため plugins.withId で「適用されたら設定する」形にする

repositories {
    google()
    mavenCentral()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun sdkVersion(name: String): Int = libs.findVersion(name).get().requiredVersion.toInt()

// AGP 9 の CommonExtension は型引数を持たないので、application と library を 1 つの型で扱える。
// Gradle の拡張検索は登録型の上位型にも一致するため、どちらのモジュールでもこれ 1 つで拾える
fun Project.configureAndroidBase() = extensions.configure<CommonExtension> {
    compileSdk = sdkVersion("compileSdk")
    defaultConfig.minSdk = sdkVersion("minSdk")
    compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    compileOptions.targetCompatibility = JavaVersion.VERSION_17
}

// KMP モジュールでは android 拡張が project ではなく kotlin 拡張の下に居るので、
// CommonExtension では引けない。ターゲットを直接設定する。
// jvmTarget をここで指定するのが重要——KMP には compileOptions が無く、書かないと
// バイトコード版がビルド環境の JDK で決まる（手元と CI で別物が出る）
fun Project.configureKmpAndroidBase() = extensions.configure<KotlinMultiplatformExtension> {
    val android = (this as ExtensionAware).extensions
        .getByName("androidLibrary") as KotlinMultiplatformAndroidLibraryTarget
    android.compileSdk = sdkVersion("compileSdk")
    android.minSdk = sdkVersion("minSdk")
    android.compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
    // ホストテストは既定で無効。開けないとユニットテストが 0 件のままビルドが緑になる。
    // 設定はすべて既定でよいのでラムダは空。呼ぶこと自体が有効化になる
    android.withHostTestBuilder { }.configure { }
}

plugins.withId("com.android.application") { configureAndroidBase() }
plugins.withId("com.android.library") { configureAndroidBase() }
plugins.withId("com.android.kotlin.multiplatform.library") { configureKmpAndroidBase() }
