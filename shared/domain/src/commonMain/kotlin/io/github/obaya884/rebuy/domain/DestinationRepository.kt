package io.github.obaya884.rebuy.domain

import io.github.obaya884.rebuy.data.destination.Destination
import io.github.obaya884.rebuy.data.destination.DestinationDao
import kotlinx.coroutines.flow.Flow

class DestinationRepository(private val destinationDao: DestinationDao) {
    fun getAll(): Flow<List<Destination>> = destinationDao.getAllDestinations()

    /** 新しい行き先は並びの末尾に置く（データモデル定義書 §6）。 */
    suspend fun insert(name: String) {
        destinationDao.insert(
            Destination(name = name, sortOrder = destinationDao.maxSortOrder() + 1)
        )
    }

    suspend fun updateName(id: Int, newName: String) {
        destinationDao.updateDestinationName(id, newName)
    }

    suspend fun updateSortOrder(id: Int, newSortOrder: Int) {
        destinationDao.updateDestinationSortOrder(id, newSortOrder)
    }

    /** 紐づく品目は消えず、外部キーの `SET_NULL` で「どこでも買えるもの」に戻る。 */
    suspend fun delete(destination: Destination) {
        destinationDao.delete(destination)
    }
}
