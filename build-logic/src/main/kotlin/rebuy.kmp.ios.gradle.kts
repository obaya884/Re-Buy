import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// :shared:* が持つ iOS ターゲットをここで揃える。モジュール側に書くと、
// ターゲットを 1 つ書き忘れてもビルドは通り、そのターゲットだけリンク時に落ちる。
// Android では一切現れない壊れ方なので、宣言を 1 か所にする。
//
// iosX64（Intel Mac のシミュレータ）は持たない。開発機も GitHub の macOS ランナーも
// arm64 なので、要るようになったらここに 1 行足す

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        iosArm64()
        iosSimulatorArm64()
    }
}
