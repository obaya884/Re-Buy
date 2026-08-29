plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    // repositories・compileSdk・minSdk・jvmTarget・ホストテストの有効化を入れる
    id("rebuy.android.base")
}

kotlin {
    androidLibrary {
        namespace = "io.github.obaya884.rebuy.data"
    }

    sourceSets {
        androidMain.dependencies {
            // DAO の戻り値が Flow なので、公開 API として上の層へ通す
            api(libs.kotlinx.coroutines.core)

            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)

            // dataModule（Koin の Module 型）を上の層へ公開する。
            // sourceSets の中では platform() が生えないので project.dependencies から呼ぶ
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            // androidContext() は DataModule の内側だけで使う
            implementation(libs.koin.android)
        }

        // androidHostTest は withHostTestBuilder が動的に作るので型付きアクセサが無い
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
        }
    }
}

// ksp arg の room.schemaLocation は KMP では効かない
room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    // ターゲットごとに書く必要がある。書き忘れてもビルドは通り、そのターゲットだけリンク時に落ちる
    add("kspAndroid", libs.androidx.room.compiler)
}

// AGP の lint タスクが KSP の生成先を入力に取るのに依存を宣言しないので、自分で繋ぐ。
// KMP ライブラリプラグインの host test でだけ起きる。kspAndroidHostTest には processor が
// 1 つも登録されていない（Room を回すのは kspAndroid だけ）が、空の生成先だけは作られる。
// AGP 9.3.2 で確認。AGP を上げたら外して試すこと
// lint タスクは評価後に登録されるので tasks.named では引けない。名前が変わってマッチが 0 件に
// なっても黙って通ってしまうが、そのときは元の Property has implicit dependency に戻るだけで気づける
tasks.matching { it.name == "lintAnalyzeAndroidHostTest" || it.name == "generateAndroidHostTestLintModel" }
    .configureEach { dependsOn("kspAndroidHostTest") }
