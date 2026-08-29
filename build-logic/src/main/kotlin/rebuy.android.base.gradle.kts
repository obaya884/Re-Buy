import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// 各モジュールの Android の土台をここに集める。内訳は下の 2 つの関数を見ること。
// モジュール側で書き忘れても効くので、モジュールを増やしたときのずれが起きない。
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

// KMP モジュールには compileOptions が無い。jvmTarget を書かないとバイトコード版が
// ビルド環境の JDK で決まり、手元と CI で別物が出る
fun Project.configureKmpAndroidBase() = extensions.configure<KotlinMultiplatformExtension> {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        compileSdk = sdkVersion("compileSdk")
        minSdk = sdkVersion("minSdk")
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        // ホストテストは既定で無効。開けないとユニットテストが 0 件のままビルドが緑になる
        withHostTest { }
    }
}

plugins.withId("com.android.application") { configureAndroidBase() }
plugins.withId("com.android.library") { configureAndroidBase() }
plugins.withId("com.android.kotlin.multiplatform.library") { configureKmpAndroidBase() }
