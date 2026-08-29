plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    // KMP モジュールには android 拡張が無いので、いま効くのは repositories だけ（T-28b で口を足す）
    id("rebuy.android.base")
}

kotlin {
    androidLibrary {
        namespace = "io.github.obaya884.rebuy.data"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        // ホストテストは既定で無効。開けないとユニットテストが 0 件のままビルドが緑になる
        withHostTestBuilder { }.configure { }
    }

    sourceSets {
        androidMain.dependencies {
            // DAO の戻り値が Flow なので、公開 API として上の層へ通す
            api(libs.kotlinx.coroutines.core)

            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)

            // dataModule（Koin の Module 型）を上の層へ公開する
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            // androidContext() は DataModule の内側だけで使う
            implementation(libs.koin.android)
        }

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
// KMP ライブラリプラグインの host test でだけ起きる
tasks.matching { it.name == "lintAnalyzeAndroidHostTest" || it.name == "generateAndroidHostTestLintModel" }
    .configureEach { dependsOn("kspAndroidHostTest") }
