import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// :shared:* が持つ iOS ターゲットをここで揃える。モジュール側に列挙すると、
// ターゲットを 1 つ書き忘れてもビルドは通り、そのターゲットだけリンク時に落ちる。
// Android では一切現れない壊れ方なので、宣言を 1 か所にする。
//
// iosX64 は持たない（理由は log_23）。要るようになったらここに 1 行足す。
// **足したら 17 §5 の `-x` も 1 本増やす**——手元の `./gradlew build` は Release の
// リンクを名指しで外しているので、増えたターゲットのぶんだけ黙って数分伸びる。
//
// plugins.withId で包まない。KMP でないモジュールに誤って適用したら、拡張が無いと言って
// 落ちてほしい——黙って効かない状態を作らないのがこのプラグインの趣旨なので。
// そのぶん plugins {} での適用順が KMP プラグインより後であることに依存する

extensions.configure<KotlinMultiplatformExtension> {
    iosArm64()
    iosSimulatorArm64()
}
