package io.github.obaya884.rebuy.data.settings

import android.content.Context

private const val PREFERENCES_NAME = "rebuy_settings"

/**
 * `SharedPreferences` に載せる。**ファイル名を変えると設定を見失う**ので固定する。
 *
 * `apply()` は非同期に書き出すが、以後の読み出しは同じインスタンスのメモリ上の値を返すので、
 * 選んだ直後の読み出しでも新しい値が見える。
 */
class SharedPreferencesSettingsStore(context: Context) : SettingsStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}
