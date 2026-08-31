import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    // Compose Multiplatform。:shared:ui の Compose はここから来る
    alias(libs.plugins.compose.multiplatform)
    // バージョンはルートの buildscript classpath で固定しているので、ここでは版を指定しない
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.aboutLibraries)
    // iOS ターゲット（宣言は build-logic に 1 か所）
    id("rebuy.kmp.ios")
    // Android の土台（内訳は plugin 側）
    id("rebuy.android.base")
}

val uiPackage = "io.github.obaya884.rebuy.ui"

aboutLibraries {
    collect {
        // iOS の依存も集め、entry の targets で表示側が絞る（設計の全体像は
        // docs/仕様/15_アーキテクチャ定義書.md §6）。
        // **all = true にすると filterVariants の意味が完全一致から部分一致に変わる。**
        // 曖昧な語（android・ios など）を入れると意図しない構成まで拾うので、
        // 構成名をフルで名指しする
        all = true
        includeTargets = true
        filterVariants.set(
            setOf(
                "androidCompileClasspath",
                "androidRuntimeClasspath",
                "iosArm64CompileKlibraries",
                "iosSimulatorArm64CompileKlibraries",
            )
        )
    }
    export {
        // 置き場所は AboutLibraries が KMP 向けに示している場所。
        // **R.raw に出すほうの経路は止まらない**（15.2.0 に止める設定が無い）。
        // あちらはバリアント名の後段フィルタで all = true でも 0 件のままで、APK にも
        // 71 バイトの空の json が残るが、読む側が composeResources に移ったので誰も見ない。
        //
        // 生成物だが Room のスキーマ（shared/data/schemas）と同じくコミットする——
        // 依存を足し引きしたときに一覧の変化が diff に出るほうが、気づける。
        // 1 行 88KB では diff が読めないので prettyPrint も入れる
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
    }
}

// 生成物がリソースのソースディレクトリの中に出るので、読む側のタスクに依存を明示する。
// 宣言しないと順序が保証されず、**再生成より先にコピーが走って古い内容が APK に載る**。
// タスク名が変わったら named() が設定時に落ちるので、黙って外れることは無い
tasks.named("copyNonXmlValueResourcesForCommonMain") { dependsOn("exportLibraryDefinitions") }
tasks.named("convertXmlValueResourcesForCommonMain") { dependsOn("exportLibraryDefinitions") }

// 画面文言と drawable は commonMain/composeResources に置き、Compose Resources が
// 生成する Res 経由で参照する。生成先を明示しないと group / artifact 由来の
// package になり、import が読みにくくなる
compose.resources {
    packageOfResClass = "$uiPackage.resources"
    // :androidApp の instrumented テストが同じ文言で画面を突き合わせるため、
    // モジュールの外から Res を触れるようにする（既定は internal）
    publicResClass = true
}

// KMP ライブラリプラグインは BuildConfig に非対応。設定画面が出すバージョンだけのために
// BuildConfig を使っていたので、gradle.properties から Kotlin のソースを生成して置き換える
val generateVersionKt = tasks.register("generateVersionKt") {
    group = "build"
    description = "rebuy.versionName から Version.kt を生成する"
    val versionName = providers.gradleProperty("rebuy.versionName")
    val outputDir = layout.buildDirectory.dir("generated/version/kotlin")
    // doLast の中からスクリプトの val を直接見ると、設定キャッシュがスクリプト参照を
    // 直列化できずに落ちる。ここでローカルに束ねる
    val packageName = uiPackage
    val packagePath = packageName.replace('.', '/')
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("$packagePath/Version.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package $packageName

            /** `gradle.properties` の `rebuy.versionName` から生成している。直接編集しない。 */
            internal const val VERSION_NAME: String = "${versionName.get()}"
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    // iOS 側へ出す framework。baseName が Swift から見える名前になる。
    // ターゲットの宣言は rebuy.kmp.ios にある。
    // debug のみ。release は実機配布が要る段 4 で足す（そのとき Gradle のヒープを上げる。経緯は log_23）
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework(listOf(NativeBuildType.DEBUG)) {
            baseName = "ReBuyUi"
            // static。Xcode 側は framework の埋め込みコピーが要らなくなる
            isStatic = true
        }
    }

    android {
        // R の FQN になる。package と揃えておく
        namespace = uiPackage

        // KMP ライブラリでは Android リソースが既定で無効。**Compose Resources も道連れになる**
        // ——Android では assets 経由で載るが、CMP はその配線を variant.sources.assets に
        // 繋いでおり、ここを無効にすると assets ごと null になって APK から消える。
        // ビルドは緑のまま画面が全部落ちるので、R を引く場所が無くなっても有効のままにする
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateVersionKt)

            dependencies {
                api(project(":shared:domain"))

                // uiModule を :androidApp へ公開する。koinViewModel() は画面の内側だけ
                api(libs.koin.core)
                // 上流の api 頼みにせず、この source set でも版を固定する
                implementation(project.dependencies.platform(libs.koin.bom))
                // viewModelOf。koin-android のものは Android 専用
                implementation(libs.koin.core.viewmodel)

                // ViewModel の基底。-ktx と -compose は Android 専用
                implementation(libs.androidx.lifecycle.viewmodel)

                // Compose Multiplatform。androidx の Compose ではなくこちらを使う。
                // Android では JetBrains の別名アーティファクトが androidx へ解決するので、
                // :androidApp が持つ androidx の BOM とはぶつからない。
                // :androidApp と instrumented テストが直に触る型を出すものを api にする
                //   compose.ui: BottomNavigationItem の ImageVector
                //   material3: 画面 Composable が受け取る SnackbarHostState
                // runtime と foundation はこの 2 つが api で推移的に引くので implementation
                implementation(compose.runtime)
                api(compose.ui)
                api(compose.material3)
                implementation(compose.foundation)
                implementation(compose.preview)

                // 画面文言と drawable。api なのは :androidApp の instrumented テストが
                // 同じ文言で画面を突き合わせるため。implementation に落とすと
                // getString / StringResource に届かずテストが通らない
                api(compose.components.resources)

                // OSS ライセンス表示。json の読み込みが Res.readBytes になったので
                // Android 専用 API（produceLibraries(R.raw...)）は要らなくなった
                implementation(libs.aboutlibraries.core)
                implementation(libs.aboutlibraries.compose.core)
                implementation(libs.aboutlibraries.compose.m3)

                // Icons.Default.* / Icons.AutoMirrored.*。CMP に対応物が無いので JetBrains の
                // 凍結版を引く。Android に載る実体は JB 版が要求する androidx の
                // material-icons-core で、BOM を外したことで 1.7.8 から 1.7.6 に下がる。
                // 1.7.6 と 1.7.8 のソースは著作権表記の年しか違わないので表示は変わらない（log_23）
                implementation(libs.jetbrains.compose.material.icons.core)

                // 画面が koinViewModel() で ViewModel を取る
                implementation(libs.koin.compose.viewmodel)

                // Navigation 3。Screen が NavKey を継ぎ、Navigator も NavKey を公開する
                api(libs.androidx.navigation3.runtime)
                // NavDisplay。androidx は navigation3-ui の iOS 向けを publish していないので
                // JetBrains のフォークを使う。android バリアントは androidx の実装への
                // リダイレクトなので、Android に載るものは変わらない
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.androidx.lifecycle.viewmodel.navigation3)
                // Screen の @Serializable と NavKey の多相シリアライズ。
                // json は使っていない（保存形式は savedstate が決める）ので core だけ
                implementation(libs.kotlinx.serialization.core)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        // 画面を動かすテスト。**commonTest ではなくここに置く**（理由は
        // ../../docs/仕様/17_テスト戦略定義書.md §1）
        iosTest.dependencies {
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        // androidHostTest は withHostTest が動的に作るので型付きアクセサが無い。
        // koin-test の verify() は kotlin-reflect 依存で JVM 専用なのでここに残る
        getByName("androidHostTest").dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
            // ScreenSerializationTest の sealedSubclasses / objectInstance。koin-test が
            // 推移的に引いてもいるが、それが外れた日に理由の分からない
            // KotlinReflectionNotSupportedError にならないよう明示する
            implementation(kotlin("reflect"))
        }
    }
}
