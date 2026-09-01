package io.github.obaya884.rebuy.domain

import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.destination.DestinationDao
import kotlinx.coroutines.flow.Flow

class DestinationRepository(private val destinationDao: DestinationDao) {
    fun getAll(): Flow<List<Destination>> = destinationDao.getAllDestinations()

    /**
     * 名前を検証してから、並びの末尾に置く（データモデル定義書 §5・§6）。
     * 保存するのはトリム後の名前。
     */
    suspend fun insert(name: String): SaveResult =
        saveWithValidatedName(name, exceptId = NEW_RECORD_ID, destinationDao::existsName) { normalized ->
            destinationDao.insert(
                Destination(name = normalized, sortOrder = destinationDao.maxSortOrder() + 1)
            ).toInt()
        }

    suspend fun updateName(id: Int, newName: String): SaveResult =
        saveWithValidatedName(newName, exceptId = id, destinationDao::existsName) { normalized ->
            destinationDao.updateDestinationName(id, normalized)
            id
        }

    /**
     * 並び替えの保存（画面 09）。**渡された順に 1..n を振り直す**。
     *
     * 1 件だけ動かす API にしないのは、詰まった連番では 1 件の移動が周りを押し出すため
     * （5 番目を 2 番目へ動かすと 2〜4 番も動く）。全件を渡すほうが単純で、重複や歯抜けも
     * ここで揃う。
     */
    suspend fun updateOrder(orderedIds: List<Int>) {
        destinationDao.updateSortOrders(orderedIds)
    }

    /**
     * id で消す（画面 09b）。**紐づく品目は消えず**、外部キーの `SET_NULL` で「どこでも買えるもの」に戻る。
     *
     * 実体ではなく id で受けるのは、**開いている行を消す**ときに打ちかけの名前を
     * 持ち回らずに済むため（`ItemRepository.delete(id)` と同じ）。
     */
    suspend fun delete(id: Int) {
        destinationDao.deleteById(id)
    }

    /** 実体で消す。紐づく品目の扱いは [delete] と同じ。 */
    suspend fun delete(destination: Destination) {
        destinationDao.delete(destination)
    }
}
