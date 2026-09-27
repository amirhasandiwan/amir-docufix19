package com.example.data.repository

import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val totalCount: Flow<Int> = historyDao.getTotalCount()
    val totalSizeBytes: Flow<Long?> = historyDao.getTotalSizeBytes()

    fun getRecentHistory(limit: Int): Flow<List<HistoryEntity>> =
        historyDao.getRecentHistory(limit)

    fun getHistoryByType(type: String): Flow<List<HistoryEntity>> =
        historyDao.getHistoryByType(type)

    fun getFavorites(): Flow<List<HistoryEntity>> =
        historyDao.getFavorites()

    fun searchHistory(query: String): Flow<List<HistoryEntity>> =
        historyDao.searchHistory(query)

    suspend fun insert(entity: HistoryEntity): Long =
        historyDao.insert(entity)

    suspend fun insertAll(entities: List<HistoryEntity>) =
        historyDao.insertAll(entities)

    suspend fun update(entity: HistoryEntity) =
        historyDao.update(entity)

    suspend fun delete(entity: HistoryEntity) {
        val file = File(entity.filePath)
        if (file.exists()) {
            file.delete()
        }
        historyDao.delete(entity)
    }

    suspend fun deleteById(id: Long, filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
        historyDao.deleteById(id)
    }

    suspend fun clearAll() = historyDao.clearAll()
}
