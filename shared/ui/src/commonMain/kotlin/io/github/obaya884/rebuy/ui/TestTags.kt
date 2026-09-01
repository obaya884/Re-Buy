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

    /**
     * 絞り込みチップ。**カテゴリー名や行き先名は行にも出る**ので、文言で掴むと
     * チップと行のどちらを指すか決まらない。
     */
    const val POOL_CHIP_ALL = "pool_chip_all"
    const val POOL_CHIP_ANYWHERE = "pool_chip_anywhere"
    fun poolCategoryChip(categoryId: Int): String = "pool_chip_category_$categoryId"
    fun poolDestinationChip(destinationId: Int): String = "pool_chip_destination_$destinationId"

    /** 登録シート（画面 02）と新規作成ダイアログ（02b）。 */
    const val REGISTER_NAME_FIELD = "register_name_field"
    const val REGISTER_SUBMIT = "register_submit"
    const val REGISTER_SUBMIT_AND_CONTINUE = "register_submit_and_continue"
    const val REGISTER_NEW_CATEGORY_CHIP = "register_new_category_chip"
    const val REGISTER_NEW_DESTINATION_CHIP = "register_new_destination_chip"
    const val REGISTER_DIALOG_NAME_FIELD = "register_dialog_name_field"
    const val REGISTER_DIALOG_CREATE = "register_dialog_create"

    /** シートのチップ。名前は行にも出るので、文言では掴めない（プールと同じ理由）。 */
    fun registerCategoryChip(categoryId: Int): String = "register_chip_category_$categoryId"
    fun registerDestinationChip(destinationId: Int): String = "register_chip_destination_$destinationId"

    /** ボトムナビの項目。ラベル文字列は画面タイトルと衝突しうるので、テストからはこちらで掴む。 */
    fun bottomNavItem(item: BottomNavigationItem): String = "bottom_nav_${item.name}"
}
