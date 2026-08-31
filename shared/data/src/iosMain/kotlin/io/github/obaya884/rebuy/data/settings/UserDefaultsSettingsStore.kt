package io.github.obaya884.rebuy.data.settings

import platform.Foundation.NSUserDefaults

/**
 * `NSUserDefaults` に載せる。アプリのサンドボックスに属し、アプリを消すまで残る。
 *
 * `standardUserDefaults` を使うので、キーはアプリ内で一意にしておく（`ThemeRepository` が
 * 接頭辞付きのキーを渡す）。
 */
class UserDefaultsSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }
}
