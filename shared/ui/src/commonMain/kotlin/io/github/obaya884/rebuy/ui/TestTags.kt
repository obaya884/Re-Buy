package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.ui.screen.BottomNavigationItem

/**
 * インストルメンテーションテストから画面要素を引くためのタグ。
 *
 * プロダクト側とテスト側で同じ文字列を使うため、ここを唯一の定義とする。
 * `contentDescription` は読み上げの内容を変えてしまうので、テストのためだけに付けない。
 */
object TestTags {
    const val TOP_APP_BAR_TITLE = "top_app_bar_title"

    /** TopAppBar の戻る矢印。同時に 1 つしか出ないので画面ごとに分けない。 */
    const val BACK_BUTTON = "back_button"

    /** カテゴリー追加の FAB。画面を離れて戻ったときに ViewModel が作り直されるかを見るために使う。 */
    const val CATEGORY_EDIT_ADD_BUTTON = "category_edit_add_button"

    /** プール（画面 01）のアプリバーと CTA。 */
    const val POOL_ADD_BUTTON = "pool_add_button"
    const val POOL_SETTINGS_BUTTON = "pool_settings_button"
    const val POOL_START_SHOPPING_BUTTON = "pool_start_shopping_button"

    /** プールの行。**タップでカゴを出し入れする**ので、行ごとに掴めるようにする。 */
    fun poolRow(itemId: Int): String = "pool_row_$itemId"

    /** ボトムナビの項目。ラベル文字列は画面タイトルと衝突しうるので、テストからはこちらで掴む。 */
    fun bottomNavItem(item: BottomNavigationItem): String = "bottom_nav_${item.name}"
}
