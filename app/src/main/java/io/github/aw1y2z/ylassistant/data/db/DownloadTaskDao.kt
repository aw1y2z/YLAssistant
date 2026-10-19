package io.github.aw1y2z.ylassistant.data.db

import kotlinx.coroutines.flow.Flow

/**
 * 下载任务存储接口。原工程用 Room @Dao 实现，本工程工具链加不了 Room，
 * 由 InMemoryTaskDao（内存 + JSON 持久化）实现同一接口，DownloadManager 无需改动。
 */
interface DownloadTaskDao {

    fun observeAll(): Flow<List<DownloadTaskEntity>>

    suspend fun insert(task: DownloadTaskEntity): Long

    suspend fun get(id: Long): DownloadTaskEntity?

    suspend fun updateProgress(id: Long, status: Int, downloadedSize: Long, totalSize: Long)

    suspend fun updatePlan(id: Long, chunkCount: Int, totalSize: Long)

    suspend fun updateRequestHeaders(id: Long, encryptedHeaders: String)

    suspend fun markInterruptedAsPaused()

    suspend fun updateStatus(id: Long, status: Int)

    suspend fun updateError(id: Long, errorMsg: String)

    suspend fun complete(id: Long, status: Int, savePath: String)

    suspend fun delete(id: Long)
}
