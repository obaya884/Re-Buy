package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.item.ItemStatus
import io.github.obaya884.rebuy.ui.resources.Res
import io.github.obaya884.rebuy.ui.resources.license_title
import io.github.obaya884.rebuy.ui.resources.manage_category_title
import io.github.obaya884.rebuy.ui.resources.manage_destination_title
import io.github.obaya884.rebuy.ui.resources.pool_title
import io.github.obaya884.rebuy.ui.resources.setting_title
import io.github.obaya884.rebuy.ui.resources.shopping_title
import io.github.obaya884.rebuy.ui.resources.shopping_title_all
import io.github.obaya884.rebuy.ui.resources.theme_title
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

// 画面を描くテストが共有する読み出しと種。**`commonTest` の `TestData.kt` には置けない**——
// タイトルは Compose Resources を読むので、Android のホスト段（素の JVM）で初期化されると落ちる
// （テスト戦略定義書 §1）。[oneItem] だけはその制約を受けないが、使う場所が同じなので同居させている。

/** Compose Resources の読み出しは suspend なので、テスト側で待ち合わせる。 */
internal fun string(resource: StringResource, vararg args: Any): String =
    runBlocking { getString(resource, *args) }

/**
 * 画面のタイトル。
 *
 * **画面の同定に使うので、互いに異なることに依存している。** 同じ語になる画面が出たら、
 * 遷移先を取り違えても全件緑になるので、そのときは画面ごとの `testTag` に切り替えること。
 */
internal object ScreenTitle {
    val pool = string(Res.string.pool_title)
    val shoppingAll = string(Res.string.shopping_title_all)
    val setting = string(Res.string.setting_title)
    val theme = string(Res.string.theme_title)
    val categoryManage = string(Res.string.manage_category_title)
    val destinationManage = string(Res.string.manage_destination_title)

    /** 07 の行ラベルと同じキー（`license_title`）。同じ語なので 1 つにまとめている。 */
    val license = string(Res.string.license_title)

    /** 行き先を選んだ買い物モード。全件モードは [shoppingAll]。 */
    fun shopping(destinationName: String) = string(Res.string.shopping_title, destinationName)
}

/** 品目を 1 件だけ置く。ステータスを変えると通る分岐が変わるので、各テストが明示する。 */
internal fun oneItem(status: ItemStatus): FakeDatabase.() -> Unit =
    { seed(items = listOf(item(id = 1, name = "アイテム1", status = status))) }
