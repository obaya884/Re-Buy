package io.github.obaya884.rebuy.data.settings

/**
 * DB の外に置く設定値の保存先（データモデル定義書 §9）。
 *
 * **Room を使わない。** 置くのは「テーマの選択」のような単票の設定で、関連も履歴も
 * 持たない。プラットフォームの素の仕組み（Android は `SharedPreferences`、iOS は
 * `NSUserDefaults`）にそのまま載せる。
 *
 * 変更の通知はここでは持たない。読み書きは同期で、**流すのは上の層**（`ThemeRepository`）。
 *
 * **`expect class` ではなく interface にしてある**（上の層のテストで fake を差せるように）。
 */
interface SettingsStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}
