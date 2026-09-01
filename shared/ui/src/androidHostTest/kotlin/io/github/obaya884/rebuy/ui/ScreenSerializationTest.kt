package io.github.obaya884.rebuy.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.descriptors.elementNames
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
 * **ルートを足したら [routes] に 1 行足す。** 引数付きのルート（`Shopping`）があるので
 * `objectInstance` では実体を作れず、表を手で持つしかない。表の側が欠けたときは
 * [ルートの顔ぶれが変わっていない] が `sealedSubclasses` との差として落とす。
 */
class ScreenSerializationTest {

    /** 登録の検査に使うルートの実体。引数付きのルートは**両極を並べる**。 */
    private val routes: List<Screen> = listOf(
        Screen.Pool,
        Screen.Shopping(destinationId = 1),
        Screen.Shopping(destinationId = null),
        Screen.Setting,
        Screen.CategoryEdit,
        Screen.License,
        Screen.Theme
    )

    @Test
    fun Screenのサブクラスがすべて多相シリアライズに登録されている() {
        val unregistered = routes.filter { route ->
            screenSavedStateConfiguration.serializersModule
                .getPolymorphic(NavKey::class, route) == null
        }

        assertEquals(emptyList(), unregistered.map { it::class.simpleName })
    }

    /**
     * **引数を持つルートは、引数まで保存の対象に入っていること。**
     *
     * 登録の検査は「その型を書けるか」しか見ないので、`destinationId` が
     * コンストラクタから外れても（`@Transient` が付いても）素通りする。
     * 復元したら全件モードに化ける、という壊れ方になる。
     */
    @Test
    fun 引数を持つルートは引数も保存される() {
        val serializer = screenSavedStateConfiguration.serializersModule
            .getPolymorphic(NavKey::class, Screen.Shopping(destinationId = 1))

        assertEquals(
            listOf("destinationId"),
            serializer?.descriptor?.elementNames?.toList()
        )
    }

    @Test
    fun ルートの顔ぶれが変わっていない() {
        // 上の検査は [routes] しか見ない。ルートを足して表に書き忘れても、消えても素通りするので、
        // 実際のサブクラスと突き合わせたうえで名前も並べて固定する
        val declared = Screen::class.sealedSubclasses.map { it.simpleName }.toSet()

        assertEquals(declared, routes.map { it::class.simpleName }.toSet())
        assertEquals(
            setOf("Pool", "Shopping", "Setting", "CategoryEdit", "License", "Theme"),
            declared
        )
    }
}
