package io.github.obaya884.rebuy.ui

import io.github.obaya884.rebuy.data.item.Item
import io.github.obaya884.rebuy.domain.CategoryRepository
import io.github.obaya884.rebuy.domain.DestinationRepository
import io.github.obaya884.rebuy.domain.ItemRepository
import io.github.obaya884.rebuy.domain.NameError
import io.github.obaya884.rebuy.domain.SaveResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 名前の検証のうち、DB を引く必要があるぶん（データモデル定義書 §5）——同種内の重複と、
 * **保存されるのがトリム後の名前であること**。トリム・空・上限そのものの規則は
 * `:shared:domain` の `NameRuleTest` が見る。
 *
 * 3 種類とも `saveWithValidatedName` の同じ経路を通るので、規則ごとに代表を選ぶ。
 * ただし**「自分自身は重複に数えない」だけは 3 種類とも見る**——Repository が
 * `exceptId` に何を渡すかは経路ごとの配線なので、1 か所落ちても他が緑になるため。
 * **DAO の SQL そのものはここでは見ていない**（`FakeDatabase` が条件を再実装している）。
 * 実 SQL の網は `AppDatabaseIosTest`。
 */
class NameValidationRepositoryTest {

    /** 保存できたことだけを見る。id は経路ごとに違うので、ここでは問わない。 */
    private fun assertIsSaved(result: SaveResult) {
        assertTrue(result is SaveResult.Saved, "保存できていない: $result")
    }

    private val db = FakeDatabase()
    private val itemRepository = ItemRepository(db.itemDao)
    private val categoryRepository = CategoryRepository(db.categoryDao)
    private val destinationRepository = DestinationRepository(db.destinationDao)

    @Test
    fun 空の名前は弾かれ保存されない() = runTest {
        assertEquals(
            SaveResult.Rejected(NameError.BLANK),
            categoryRepository.insert("　 ")
        )
        assertEquals(emptyList(), db.storedCategories)
    }

    @Test
    fun 上限を超える名前は弾かれ保存されない() = runTest {
        assertEquals(
            SaveResult.Rejected(NameError.TOO_LONG),
            destinationRepository.insert("あ".repeat(31))
        )
        assertEquals(emptyList(), db.storedDestinations)
    }

    /**
     * **トリム後の名前を保存する経路は 6 つある**（3 種類 × 追加・改名）。検証の手順は
     * `saveWithValidatedName` に寄せてあるが、**書き込みのラムダは呼び出し側に 6 か所ある**
     * ので、そこで生の名前を渡す変異は経路ごとにしか捕まらない。
     */
    @Test
    fun 追加ではトリム後の名前が保存される() = runTest {
        db.seed(categories = listOf(category(id = 1)))

        categoryRepository.insert("  カテゴリーA　")
        destinationRepository.insert("  行き先A　")
        itemRepository.insert(Item(name = "  アイテムA　", categoryId = 1))

        assertEquals("カテゴリーA", db.storedCategories.single { it.id != 1 }.name)
        assertEquals("行き先A", db.storedDestinations.single().name)
        // 名前以外は渡されたまま
        val item = db.storedItems.single()
        assertEquals("アイテムA", item.name)
        assertEquals(1, item.categoryId)
    }

    @Test
    fun 改名でもトリム後の名前が保存される() = runTest {
        db.seed(
            items = listOf(item(id = 1)),
            categories = listOf(category(id = 1)),
            destinations = listOf(destination(id = 1))
        )

        itemRepository.updateName(1, "  アイテムA　")
        categoryRepository.updateName(1, "  カテゴリーA　")
        destinationRepository.updateName(1, "  行き先A　")

        assertEquals("アイテムA", db.storedItem(1).name)
        assertEquals("カテゴリーA", db.storedCategories.single().name)
        assertEquals("行き先A", db.storedDestinations.single().name)
    }

    /** トリム後に一致すれば重複。入力の空白の差では逃げられない。 */
    @Test
    fun トリムして同じになる名前は重複() = runTest {
        db.seed(items = listOf(item(id = 1, name = "アイテムA")))

        assertEquals(
            SaveResult.Rejected(NameError.DUPLICATE),
            itemRepository.insert(Item(name = " アイテムA "))
        )
        assertEquals(1, db.storedItems.size)
    }

    /** **正規化はしない**（§5）。大文字小文字・かなカナは別の名前として通る。 */
    @Test
    fun 大文字小文字とかなカナは別の名前() = runTest {
        db.seed(items = listOf(item(id = 1, name = "abc"), item(id = 2, name = "あいて")))

        assertIsSaved(itemRepository.insert(Item(name = "ABC")))
        assertIsSaved(itemRepository.insert(Item(name = "アイテ")))
    }

    @Test
    fun 品目は自分自身と同じ名前に改名できる() = runTest {
        db.seed(items = listOf(item(id = 1, name = "アイテムA"), item(id = 2, name = "アイテムB")))

        assertIsSaved(itemRepository.updateName(1, "アイテムA"))
        assertEquals(
            SaveResult.Rejected(NameError.DUPLICATE),
            itemRepository.updateName(1, "アイテムB")
        )
    }

    @Test
    fun カテゴリーは自分自身と同じ名前に改名できる() = runTest {
        db.seed(categories = listOf(category(id = 1, name = "カテゴリーA"), category(id = 2)))

        assertIsSaved(categoryRepository.updateName(1, "カテゴリーA"))
        assertEquals(
            SaveResult.Rejected(NameError.DUPLICATE),
            categoryRepository.updateName(1, "カテゴリー2")
        )
    }

    @Test
    fun 行き先は自分自身と同じ名前に改名できる() = runTest {
        db.seed(destinations = listOf(destination(id = 1, name = "行き先A"), destination(id = 2)))

        assertIsSaved(destinationRepository.updateName(1, "行き先A"))
        assertEquals(
            SaveResult.Rejected(NameError.DUPLICATE),
            destinationRepository.updateName(1, "行き先2")
        )
    }

    /** 種類をまたぐ重複は見ない。カテゴリーと同じ名前の行き先は作れる。 */
    @Test
    fun 別の種類の同じ名前とはぶつからない() = runTest {
        categoryRepository.insert("共通の名前")

        assertIsSaved(destinationRepository.insert("共通の名前"))
    }
}
