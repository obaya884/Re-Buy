import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.language.base.plugins.LifecycleBasePlugin
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
 * **テストソースを持つ source set が 1 つでも「1 件も実行していない」なら落とす**（T-32）。
 *
 * KMP ライブラリプラグインはホストテストが既定で無効で、開け忘れると **0 件のまま緑**になる。
 * 段 3 では 2 回踏んだ——`withHostTest {}` の入れ忘れと、`testDebugUnitTest` が KMP 化で
 * 一致するモジュールを失った件。
 *
 * **期待件数は書かない。** 書くとその数字の更新漏れが起きる。代わりに source set ごとに
 * 「そこ由来のテストクラスが 1 つ以上走ったか」を見る。テストを持たない `:shared:domain` は
 * 自動的に外れ、依存の増減で数字が動くこともない。
 *
 * 設計の理由はどれも実測で決まっている。**外すと止まらなくなる。**
 *
 * - **テストタスクの `doLast` には置けない。** source set が外れると `NO-SOURCE` で
 *   タスクごと飛び、`doLast` も走らない
 * - **`check` にも繋ぐ。** `finalizedBy` だけだと、`withHostTest {}` の入れ忘れで
 *   テストタスク自体が生成されないときに検査ごと消える——止めたい事故の 1 つがそれ
 * - **タスクごとに自分の出力だけを見る。** `build/test-results` を丸ごと数えると、
 *   別のタスクが前に残した XML で通る
 * - **source set 単位で見る。** タスク単位の合計だと、`commonTest` の 106 件が丸ごと
 *   消えても `androidHostTest` の数件で緑になる
 * - **`skipped` を差し引く。** `tests` は `@Ignore` も数えるので、全件 `@Ignore` でも通る
 * - **iOS は macOS のときだけ。** Linux では Kotlin/Native のテストタスクが無効化されるので、
 *   検査だけが走って 0 件と判定してしまう（落とし穴 19 を検査の側で踏んだ）
 *
 * **守れないもの**（[テスト戦略定義書](../../../../docs/仕様/17_テスト戦略定義書.md) §6）:
 * `:androidApp` の instrumented、ターゲットや source set を増やしたときの追随、
 * モジュールが `rebuy.android.base` を付け忘れた場合。
 */
fun Project.failOnZeroTests() {
    // テストタスクと、それが拾う source set。ここに書いていないものは検査されない
    val isMacOs = providers.systemProperty("os.name").get().startsWith("Mac")
    val testTasks = buildMap {
        put("testAndroidHostTest", listOf("commonTest", "androidHostTest"))
        if (isMacOs) put("iosSimulatorArm64Test", listOf("commonTest", "iosTest"))
    }
    val projectPath = path

    testTasks.forEach { (testTask, sourceSetNames) ->
        val sourceSetDirs = sourceSetNames.associateWith {
            layout.projectDirectory.dir("src/$it").asFile
        }
        val resultsDir = layout.buildDirectory.dir("test-results/$testTask").get().asFile

        val verify = tasks.register(
            "verify${testTask.replaceFirstChar { it.uppercase() }}Executed"
        ) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "$testTask で 1 件も走っていない source set があれば落とす（T-32）"
            doLast {
                // 実際に 1 件以上走ったテストクラスの単純名。
                // iOS の name は "iosSimulatorArm64Test.<FQCN>"、入れ子クラスは "Outer$Inner"。
                // skipped を引くのは、tests が @Ignore も数えるため
                val testSuite =
                    Regex("""<testsuite name="([^"]+)"[^>]*tests="(\d+)"[^>]*skipped="(\d+)"""")
                val executed = if (!resultsDir.exists()) emptySet() else resultsDir.walkTopDown()
                    .filter { it.extension == "xml" }
                    .flatMap { testSuite.findAll(it.readText()) }
                    .filter { it.groupValues[2].toInt() - it.groupValues[3].toInt() > 0 }
                    .map { it.groupValues[1].substringAfterLast('.').substringBefore('$') }
                    .toSet()

                sourceSetDirs.forEach { (name, dir) ->
                    // このリポジトリは 1 ファイル 1 テストクラス
                    val declared = if (!dir.exists()) emptySet() else dir.walkTopDown()
                        .filter { it.extension == "kt" }
                        .map { it.nameWithoutExtension }
                        .toSet()
                    if (declared.isEmpty()) return@forEach
                    if (declared.none { it in executed }) {
                        throw GradleException(
                            "$projectPath:$testTask が src/$name のテストを 1 件も実行していない。" +
                                "source set の設定が外れていないか確かめること（T-32）"
                        )
                    }
                }
            }
        }

        // finalizedBy だけだとタスクが無いときに検査ごと消えるので check にも繋ぐ
        tasks.withType<AbstractTestTask>().configureEach {
            if (name == testTask) finalizedBy(verify)
        }
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(verify) }
    }
}

plugins.withId("com.android.application") { configureAndroidBase() }
plugins.withId("com.android.library") { configureAndroidBase() }
plugins.withId("com.android.kotlin.multiplatform.library") {
    configureKmpAndroidBase()
    failOnZeroTests()
}
