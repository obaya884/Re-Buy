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

    const val HOME_ITEM_EDIT_BUTTON = "home_item_edit_button"
    const val HOME_CATEGORY_EDIT_BUTTON = "home_category_edit_button"
    const val HOME_SETTINGS_BUTTON = "home_settings_button"

    /** ボトムナビの項目。ラベル文字列は画面タイトルと衝突しうるので、テストからはこちらで掴む。 */
    fun bottomNavItem(item: BottomNavigationItem): String = "bottom_nav_${item.name}"
}
