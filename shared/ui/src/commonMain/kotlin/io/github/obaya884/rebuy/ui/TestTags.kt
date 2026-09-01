package io.github.obaya884.rebuy.ui

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

    /** 登録シート（画面 02）。 */
    const val REGISTER_NAME_FIELD = "register_name_field"
    const val REGISTER_SUBMIT = "register_submit"
    const val REGISTER_SUBMIT_AND_CONTINUE = "register_submit_and_continue"
    /** 02 と 06 が共通で使う部品（チップ列・02b のダイアログ）。 */
    const val ITEM_FORM_NEW_CATEGORY_CHIP = "item_form_new_category_chip"
    const val ITEM_FORM_NEW_DESTINATION_CHIP = "item_form_new_destination_chip"
    const val ITEM_FORM_DIALOG_NAME_FIELD = "item_form_dialog_name_field"
    const val ITEM_FORM_DIALOG_CREATE = "item_form_dialog_create"

    /** シートのチップ。名前は行にも出るので、文言では掴めない（プールと同じ理由）。 */
    fun itemFormCategoryChip(categoryId: Int): String = "item_form_chip_category_$categoryId"
    fun itemFormDestinationChip(destinationId: Int): String = "item_form_chip_destination_$destinationId"

    /** 買い物開始シート（画面 03）。 */
    fun shoppingStartRow(destinationId: Int): String = "shopping_start_row_$destinationId"
    const val SHOPPING_START_ALL_ROW = "shopping_start_row_all"

    /** 買い物モード（画面 04）。 */
    fun shoppingRow(itemId: Int): String = "shopping_row_$itemId"
    const val SHOPPING_PROGRESS = "shopping_progress"
    const val SHOPPING_ANYWHERE_SECTION = "shopping_anywhere_section"
    const val SHOPPING_FINISH_BUTTON = "shopping_finish_button"
    const val SHOPPING_LEAVE_CONFIRM = "shopping_leave_confirm"
    const val SHOPPING_LEAVE_CANCEL = "shopping_leave_cancel"

    /** 気づいたものを足すシート（画面 05）。 */
    const val SHOPPING_ADD_NOTICED_ROW = "shopping_add_noticed_row"
    const val ADD_NOTICED_SEARCH_FIELD = "add_noticed_search_field"
    const val ADD_NOTICED_SECTION_UNADDED = "add_noticed_section_unadded"
    const val ADD_NOTICED_SECTION_ELSEWHERE = "add_noticed_section_elsewhere"
    const val ADD_NOTICED_REGISTER = "add_noticed_register"
    fun addNoticedRow(itemId: Int): String = "add_noticed_row_$itemId"

    /** 品目編集シート（画面 06）。 */
    const val ITEM_SHEET_NAME_FIELD = "item_sheet_name_field"
    const val ITEM_SHEET_SAVE = "item_sheet_save"
    const val ITEM_SHEET_DELETE = "item_sheet_delete"
    const val ITEM_SHEET_DELETE_CONFIRM = "item_sheet_delete_confirm"
    const val ITEM_SHEET_CATEGORY_NONE_CHIP = "item_sheet_chip_category_none"
    const val ITEM_SHEET_DESTINATION_NONE_CHIP = "item_sheet_chip_destination_none"
}
