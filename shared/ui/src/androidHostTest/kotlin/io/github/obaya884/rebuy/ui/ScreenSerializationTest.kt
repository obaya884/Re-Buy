package io.github.obaya884.rebuy.ui

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [Screen] のサブクラスが 1 つ残らず [screenSavedStateConfiguration] に登録されていることを固定する。
 *
 * backstack が持つのは `NavKey`（sealed でない interface）なので、保存・復元には
 * **開いた多相**の登録が要る。画面を足して `subclass(...)` を書き忘れると、その画面にいる状態で
 * プロセス保存が走った瞬間に `SerializationException` になる（Android も iOS も同じ。実測）。
 *
 * 同じ穴は `NavigationStateRestorationTest`（instrumented）も塞ぐが、あちらは実機が要って遅い。
 * ここは**画面を足した直後に、ユニットテストの速さで**気づけるようにするためにある。
 *
 * 1 件目は `Screen::class.sealedSubclasses` で列挙するので、**登録側と検査側で同じ表を
 * 二重に持たない**。画面を足せば自動的に検査対象に入る。
 * 2 件目だけは名前を並べて持つ——列挙どうしを突き合わせても「ルートごと消えた」は捕まらないため。
 * 列挙に reflection が要るので JVM 側に置いている。
 *
 * **引数付きのルート（`@Serializable data class ItemEdit(val id: Long)` など）を入れるときは、
 * この列挙を書き直すこと。** いまは `objectInstance` から実体を取る前提になっている。
 */
class ScreenSerializationTest {

    @Test
    fun Screenのサブクラスがすべて多相シリアライズに登録されている() {
        val subclasses = Screen::class.sealedSubclasses
        // data object でないルートが混ざるとこの検査が素通りするので、そこも止める
        assertEquals(emptyList(), subclasses.filter { it.objectInstance == null }.map { it.simpleName })

        val unregistered = subclasses.filter { subclass ->
            val route = checkNotNull(subclass.objectInstance)
            screenSavedStateConfiguration.serializersModule
                .getPolymorphic(NavKey::class, route) == null
        }

        assertEquals(emptyList(), unregistered.map { it.simpleName })
    }

    @Test
    fun ルートの顔ぶれが変わっていない() {
        // 上の検査は「登録されているか」しか見ない。ルートごと消えても素通りするので、
        // ここだけは名前を並べて固定する（消えたルート名が失敗メッセージに出る）
        assertEquals(
            setOf("Home", "Shopping", "Setting", "CategoryEdit", "ItemEdit", "License", "Theme"),
            Screen::class.sealedSubclasses.map { it.simpleName }.toSet()
        )
    }
}
