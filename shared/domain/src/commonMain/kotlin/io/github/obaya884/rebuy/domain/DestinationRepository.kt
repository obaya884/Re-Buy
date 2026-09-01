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

    suspend fun updateSortOrder(id: Int, newSortOrder: Int) {
        destinationDao.updateDestinationSortOrder(id, newSortOrder)
    }

    /** 紐づく品目は消えず、外部キーの `SET_NULL` で「どこでも買えるもの」に戻る。 */
    suspend fun delete(destination: Destination) {
        destinationDao.delete(destination)
    }
}
