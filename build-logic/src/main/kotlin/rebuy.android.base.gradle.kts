import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.tasks.testing.AbstractTestTask
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

/**
 * テストソースを持つモジュールで、テストが 1 件も走らずに緑になるのを止める（T-32）。
 *
 * KMP ライブラリプラグインはホストテストが既定で無効で、開け忘れると **0 件のまま緑**になる。
 * 段 3 では 2 回踏んだ——`withHostTest {}` の入れ忘れと、`testDebugUnitTest` が KMP 化で
 * 一致するモジュールを失った件。いま件数を守っているのは「人が数えていること」だけ。
 *
 * **モジュールごとの期待件数は書かない。** 書くとその数字の更新漏れが起きる。
 * 「テストソースがあるのに 0 件」だけを見れば、テストを持たない `:shared:domain` は
 * 自動的に外れる。
 *
 * **テストタスク自身の `doLast` には置けない。** source set が外れると `NO-SOURCE` で
 * タスクごと飛び、`doLast` も走らずに緑になる——止めたい形がそのまま素通りする（実測）。
 * `finalizedBy` なら飛んだときも走る。`afterSuite` は設定キャッシュで使えないので、
 * 実行後に JUnit XML を読む。Gradle は `NO-SOURCE` のときタスクの出力を消すので、
 * 前回の実行結果が残って緑になることは無い。
 */
fun Project.failOnZeroTests() {
    // テストタスクと、それが拾うべき source set の対応。**タスクごとに見る**——
    // build/test-results を丸ごと数えると、別のタスクが前に残した XML で通ってしまう（実測）。
    //
    // **iOS は macOS のときだけ見る。** Linux では Kotlin/Native のテストタスクが
    // 「このホストでは走らない」と無効化されるので、検査だけが走って 0 件と判定してしまう
    // （落とし穴 19 を検査の側で踏んだ。CI で実測）
    val isMacOs = System.getProperty("os.name").startsWith("Mac")
    val testTasks = buildMap {
        put("testAndroidHostTest", listOf("commonTest", "androidHostTest"))
        if (isMacOs) put("iosSimulatorArm64Test", listOf("commonTest", "iosTest"))
    }
    val projectPath = path

    testTasks.forEach { (testTask, sourceSets) ->
        // configure 時に評価するとモジュール側の sourceSets {} より先に走りうるので provider に包む
        val hasTestSources = provider {
            sourceSets
                .map { layout.projectDirectory.dir("src/$it").asFile }
                .any { dir -> dir.walkTopDown().any { it.extension == "kt" } }
        }
        val resultsDir = layout.buildDirectory.dir("test-results/$testTask")

        val verify = tasks.register(
            "verify${testTask.replaceFirstChar { it.uppercase() }}Executed"
        ) {
            description = "$testTask がテストソースを持つのに 1 件も実行していないなら落とす（T-32）"
            doLast {
                if (!hasTestSources.get()) return@doLast
                val dir = resultsDir.get().asFile
                val executed = if (!dir.exists()) 0 else dir.walkTopDown()
                    .filter { it.extension == "xml" }
                    .sumOf { file ->
                        Regex("""<testsuite [^>]*tests="(\d+)"""")
                            .findAll(file.readText())
                            .sumOf { it.groupValues[1].toInt() }
                    }
                check(executed > 0) {
                    "$projectPath:$testTask は ${sourceSets.joinToString(" / ")} に" +
                        "テストを持つのに 1 件も実行していない。" +
                        "source set の設定が外れていないか確かめること（T-32）"
                }
            }
        }

        // named() ではなく matching()。そのターゲットを持たないモジュールでも落ちないようにする
        tasks.matching { it.name == testTask }.configureEach { finalizedBy(verify) }
    }
}

plugins.withId("com.android.application") { configureAndroidBase() }
plugins.withId("com.android.library") { configureAndroidBase() }
plugins.withId("com.android.kotlin.multiplatform.library") {
    configureKmpAndroidBase()
    failOnZeroTests()
}
