package io.github.aw1y2z.ylassistant.data.db

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 内存 + JSON 文件持久化，替代 Room（本工程工具链无法引入 KSP/kapt）。
 *
 * 语义对齐原 Room DAO：
 * - observeAll() 以 createTime 降序（与原 `ORDER BY createTime DESC` 一致）；
 * - insert 自增 id；
 * - 冷启动 load 后立即把遗留 PENDING/DOWNLOADING/FINALIZING 置为 PAUSED（断点续传前提）；
 * - 进度高频更新不落盘（避免频繁 IO），仅结构性变化（增删/完成/状态）持久化。
 */
class InMemoryTaskDao(context: Context) : DownloadTaskDao {

    private val file = File(context.applicationContext.filesDir, "download_tasks.json")
    private val lock = Any()
    private val store = LinkedHashMap<Long, DownloadTaskEntity>()
    private val _tasks = MutableStateFlow<List<DownloadTaskEntity>>(emptyList())

    init {
        load()
        markInterruptedAsPausedSync()
        refresh()
    }

    override fun observeAll(): Flow<List<DownloadTaskEntity>> = _tasks.asStateFlow()

    override suspend fun insert(task: DownloadTaskEntity): Long {
        val id = if (task.id == 0L) nextAvailableId() else task.id
        synchronized(lock) { store[id] = task.copy(id = id) }
        persist()
        refresh()
        return id
    }

    override suspend fun get(id: Long): DownloadTaskEntity? =
        synchronized(lock) { store[id]?.copy() }

    override suspend fun updateProgress(id: Long, status: Int, downloadedSize: Long, totalSize: Long) {
        synchronized(lock) {
            val cur = store[id]
            if (cur != null) store[id] = cur.copy(status = status, downloadedSize = downloadedSize, totalSize = totalSize)
        }
        refresh()
    }

    override suspend fun updatePlan(id: Long, chunkCount: Int, totalSize: Long) {
        synchronized(lock) {
            val cur = store[id]
            if (cur != null) store[id] = cur.copy(chunkCount = chunkCount, plannedTotalSize = totalSize)
        }
        refresh()
    }

    override suspend fun updateRequestHeaders(id: Long, encryptedHeaders: String) {
        synchronized(lock) {
            val cur = store[id]
            if (cur != null) store[id] = cur.copy(requestHeadersJson = encryptedHeaders)
        }
        refresh()
    }

    override suspend fun markInterruptedAsPaused() {
        markInterruptedAsPausedSync()
    }

    override suspend fun updateStatus(id: Long, status: Int) {
        synchronized(lock) {
            val cur = store[id]
            if (cur != null) store[id] = cur.copy(status = status)
        }
        persist()
        refresh()
    }

    override suspend fun updateError(id: Long, errorMsg: String) {
        synchronized(lock) {
            val cur = store[id]
            if (cur != null) store[id] = cur.copy(errorMsg = errorMsg)
        }
        refresh()
    }

    override suspend fun complete(id: Long, status: Int, savePath: String) {
        synchronized(lock) {
            val cur = store[id]
            if (cur != null) store[id] = cur.copy(status = status, savePath = savePath, errorMsg = "")
        }
        persist()
        refresh()
    }

    override suspend fun delete(id: Long) {
        synchronized(lock) { store.remove(id) }
        persist()
        refresh()
    }

    // ---------- 内部 ----------

    private fun nextAvailableId(): Long = synchronized(lock) {
        (store.keys.maxOrNull() ?: 0L) + 1
    }

    private fun refresh() {
        val snapshot = synchronized(lock) {
            store.values.toList().sortedByDescending { it.createTime }
        }
        _tasks.value = snapshot
    }

    private fun markInterruptedAsPausedSync() {
        synchronized(lock) {
            store.keys.toList().forEach { id ->
                val e = store[id] ?: return@forEach
                if (e.status == DownloadTaskEntity.STATUS_DOWNLOADING ||
                    e.status == DownloadTaskEntity.STATUS_PENDING ||
                    e.status == DownloadTaskEntity.STATUS_FINALIZING
                ) {
                    store[id] = e.copy(status = DownloadTaskEntity.STATUS_PAUSED)
                }
            }
        }
        persist()
    }

    private fun persist() {
        val snapshot = synchronized(lock) { store.values.toList() }
        runCatching {
            val arr = JSONArray()
            for (e in snapshot) arr.put(toJson(e))
            file.writeText(arr.toString())
        }
    }

    private fun load() {
        runCatching {
            if (!file.exists()) return
            val arr = JSONArray(file.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val e = DownloadTaskEntity(
                    id = o.optLong("id"),
                    url = o.optString("url"),
                    fileName = o.optString("fileName"),
                    totalSize = o.optLong("totalSize"),
                    downloadedSize = o.optLong("downloadedSize"),
                    status = o.optInt("status"),
                    errorMsg = o.optString("errorMsg"),
                    savePath = o.optString("savePath"),
                    requestHeadersJson = o.optString("requestHeadersJson"),
                    chunkCount = o.optInt("chunkCount"),
                    plannedTotalSize = o.optLong("plannedTotalSize"),
                    cleanupId = o.optString("cleanupId"),
                    isHls = o.optBoolean("isHls"),
                    createTime = o.optLong("createTime")
                )
                synchronized(lock) { store[e.id] = e }
            }
        }
    }

    private fun toJson(e: DownloadTaskEntity): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("url", e.url)
        put("fileName", e.fileName)
        put("totalSize", e.totalSize)
        put("downloadedSize", e.downloadedSize)
        put("status", e.status)
        put("errorMsg", e.errorMsg)
        put("savePath", e.savePath)
        put("requestHeadersJson", e.requestHeadersJson)
        put("chunkCount", e.chunkCount)
        put("plannedTotalSize", e.plannedTotalSize)
        put("cleanupId", e.cleanupId)
        put("isHls", e.isHls)
        put("createTime", e.createTime)
    }
}
