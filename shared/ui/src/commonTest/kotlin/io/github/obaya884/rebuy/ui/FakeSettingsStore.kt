package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.settings.SettingsStore

/**
 * `SettingsStore` の代わりに使うインメモリの実体。寄せているのは「未設定なら null」と
 * 「書いたものがそのまま読める」の 2 点だけ。
 */
class FakeSettingsStore(initial: Map<String, String> = emptyMap()) : SettingsStore {
    private val values = initial.toMutableMap()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    /** 何も設定していない端末へ戻す。 */
    fun clear() {
        values.clear()
    }
}
