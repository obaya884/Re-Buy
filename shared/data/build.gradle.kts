plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    // iOS ターゲット（宣言は build-logic に 1 か所）
    id("rebuy.kmp.ios")
    // Android の土台（内訳は plugin 側）
    id("rebuy.android.base")
}

kotlin {
    android {
        namespace = "io.github.obaya884.rebuy.data"

        // 本体はリソースを持たない。device test の assets（下の androidComponents）が
        // これを開かないと null になる
        androidResources { enable = true }

        // このモジュールの instrumented は、検証対象と同じモジュールに置く（T-47）。
        // 端末の定義は :androidApp と対で、同じものを繰り返す。タスク名は
        // pixel6Api35AndroidDeviceTest で、:androidApp の pixel6Api35DebugAndroidTest とは
        // 名前が違う——両方を 1 回で回すのは両者に共通の pixel6Api35Check
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            managedDevices {
                localDevices {
                    create("pixel6Api35") {
                        device = "Pixel 6"
                        apiLevel = 35
                        systemImageSource = "aosp-atd"
                    }
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // DAO の戻り値が Flow なので、公開 API として上の層へ通す
            api(libs.kotlinx.coroutines.core)

            // Room。KMP では room-ktx は room-runtime に統合されている
            implementation(libs.androidx.room.runtime)
            // driver は自分で渡す。同梱 SQLite を全ターゲットで使う
            implementation(libs.androidx.sqlite.bundled)

            // dataModule（Koin の Module 型）を上の層へ公開する。
            // sourceSets の中では platform() が生えないので project.dependencies から呼ぶ
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
        }

        androidMain.dependencies {
            // androidContext() は platformDataModule の内側だけで使う
            implementation(libs.koin.android)
        }

        commonTest.dependencies {
            // ターゲットごとに適切な実装（Android は JUnit4、iOS は kotlin.test.native）へ解決される
            implementation(kotlin("test"))
        }

        named("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.junit)
            // RoomMigrationTest の MigrationTestHelper
            implementation(libs.androidx.room.testing)
            // DataModuleTest の androidContext()
            implementation(libs.koin.android)
        }
    }
}

// Room の出力先と、RoomMigrationTest が assets として読む場所は同じでなければならない
val schemasDir = "$projectDir/schemas"

// ksp arg の room.schemaLocation は KMP では効かない
room {
    schemaDirectory(schemasDir)
}

// RoomMigrationTest の MigrationTestHelper はスキーマをテスト APK の assets から読む。
// Room の Gradle プラグインは KMP の device test には入れてくれない（APK を開いて実測）
androidComponents {
    onVariants { variant ->
        variant.deviceTests.values.forEach { deviceTest ->
            checkNotNull(deviceTest.sources.assets) { "device test に assets の置き場が無い" }
                .addStaticSourceDirectory(schemasDir)
        }
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    // ターゲットごとに書く必要がある。ksp(...) 一発では効かず、
    // **書き忘れてもそのターゲットを建てない限りビルドは緑のまま**。落ちるのは
    // そのターゲットのコンパイル時（`Expected ... has no actual declaration`）で、
    // CI では `linkDebugFrameworkIosArm64` がそこまで連れて行く（T-50）
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

// AGP の lint タスクが KSP の生成先を入力に取るのに依存を宣言しないので、自分で繋ぐ。
// KMP ライブラリプラグインの host test でだけ起きる。kspAndroidHostTest には processor が
// 1 つも登録されていない（Room を回すのは kspAndroid だけ）が、空の生成先だけは作られる。
// AGP 9.3.2 で確認。AGP を上げたら外して試すこと
// lint タスクは評価後に登録されるので tasks.named では引けない。名前が変わってマッチが 0 件に
// なっても黙って通ってしまうが、そのときは元の Property has implicit dependency に戻るだけで気づける
tasks.matching { it.name == "lintAnalyzeAndroidHostTest" || it.name == "generateAndroidHostTestLintModel" }
    .configureEach { dependsOn("kspAndroidHostTest") }
