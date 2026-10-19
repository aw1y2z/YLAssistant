package io.github.aw1y2z.ylassistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.aw1y2z.ylassistant.data.db.DownloadTaskEntity
import io.github.aw1y2z.ylassistant.data.download.DownloadManager
import io.github.aw1y2z.ylassistant.data.download.DownloadStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 下载页 ViewModel：任务列表（Flow → StateFlow）+ 实时统计 + 操作转发。
 */
class DownloadViewModel(private val manager: DownloadManager) : ViewModel() {

    val tasks: StateFlow<List<DownloadTaskEntity>> = manager.tasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** 实时下载统计：任务 id → 速度/剩余时间/线程数 */
    val stats: StateFlow<Map<Long, DownloadStats>> = manager.stats

    fun enqueue(url: String, fileName: String, headers: Map<String, String> = emptyMap()) {
        viewModelScope.launch(AppCoroutineExceptionHandler) { manager.enqueue(url, fileName, headers) }
    }

    fun pause(id: Long) = manager.pause(id)

    fun resume(id: Long) = manager.start(id)

    fun remove(id: Long, deleteLocal: Boolean = false) = manager.remove(id, deleteLocal)

    fun pauseAll() {
        tasks.value.filter {
            it.status == DownloadTaskEntity.STATUS_DOWNLOADING ||
                it.status == DownloadTaskEntity.STATUS_PENDING
        }.forEach { manager.pause(it.id) }
    }

    fun resumeAll() {
        tasks.value.filter {
            it.status == DownloadTaskEntity.STATUS_PAUSED ||
                it.status == DownloadTaskEntity.STATUS_FAILED
        }.forEach { manager.start(it.id) }
    }

    fun removeAll(deleteLocal: Boolean = false) {
        tasks.value.toList().forEach { manager.remove(it.id, deleteLocal) }
    }
}


