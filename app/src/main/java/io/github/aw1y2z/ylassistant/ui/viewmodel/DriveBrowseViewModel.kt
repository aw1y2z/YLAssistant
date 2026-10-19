package io.github.aw1y2z.ylassistant.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aw1y2z.ylassistant.data.download.DownloadManager
import io.github.aw1y2z.ylassistant.data.network.QuarkApi
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.data.network.UCApi
import io.github.aw1y2z.ylassistant.data.network.XunleiApi
import io.github.aw1y2z.ylassistant.data.network.model.ShareFile
import io.github.aw1y2z.ylassistant.data.repository.QuarkAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.QuarkResolveRepository
import io.github.aw1y2z.ylassistant.data.repository.UCAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.UCResolveRepository
import io.github.aw1y2z.ylassistant.data.repository.XunleiAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.XunleiResolveRepository
import kotlinx.coroutines.launch

/**
 * 个人网盘浏览：列目录 → 进入文件夹 / 回退 → 文件直下入队。
 * 三平台按 [activePlatform] 收敛；未登录的平台不露文件区。
 */
class DriveBrowseViewModel(
    private val quarkAccountRepo: QuarkAccountRepository,
    private val ucAccountRepo: UCAccountRepository,
    private val xunleiAccountRepo: XunleiAccountRepository,
    private val quarkApi: QuarkApi,
    private val ucApi: UCApi,
    private val xunleiApi: XunleiApi,
    private val quarkResolveRepo: QuarkResolveRepository,
    private val ucResolveRepo: UCResolveRepository,
    private val xunleiResolveRepo: XunleiResolveRepository,
    private val manager: DownloadManager,
) : ViewModel() {

    var activePlatform: SharePlatform = SharePlatform.QUARK
        private set

    /** 面包屑：与 [dirFids] 对齐，index0 = 根目录 */
    var crumbNames: List<String> by mutableStateOf(listOf("根目录"))
        private set

    var files: List<ShareFile> by mutableStateOf(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var message: String? by mutableStateOf(null)
        private set

    private var currentDir: String = PlatformSupport.rootDir(SharePlatform.QUARK)
    private val dirFids = mutableListOf(currentDir)

    /** 切平台：重置到该平台的根目录并刷新 */
    fun selectPlatform(p: SharePlatform) {
        if (p == activePlatform) {
            refresh()
            return
        }
        activePlatform = p
        currentDir = PlatformSupport.rootDir(p)
        dirFids.clear()
        dirFids.add(currentDir)
        crumbNames = listOf("根目录")
        files = emptyList()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            loading = true
            message = null
            try {
                when (activePlatform) {
                    SharePlatform.QUARK -> {
                        val cred = quarkAccountRepo.getFreshCookie()
                        if (cred == null) { message = "夸克登录已失效"; return@launch }
                        quarkApi.getFileList(currentDir, cred)?.let { files = it.sortedBy { f -> !f.isdir || f.fsize == 0L } }
                            ?: run { message = "获取目录失败" }
                    }
                    SharePlatform.UC -> {
                        val cred = ucAccountRepo.getFreshCookie()
                        if (cred == null) { message = "UC 登录已失效"; return@launch }
                        ucApi.getFileList(currentDir, cred)?.let { files = it.sortedBy { f -> !f.isdir || f.fsize == 0L } }
                            ?: run { message = "获取目录失败" }
                    }
                    SharePlatform.XUNLEI -> {
                        val acc = xunleiAccountRepo.getAccount()
                        if (acc == null) { message = "迅雷登录已失效"; return@launch }
                        xunleiApi.getFiles(currentDir, acc.accessToken, acc.deviceId, acc.captchaToken)
                            ?.let { files = it.sortedBy { f -> !f.isdir || f.fsize == 0L } }
                            ?: run { message = "获取目录失败" }
                    }
                }
            } catch (e: Exception) {
                message = "获取目录失败：${e.message ?: e.javaClass.simpleName}"
                Log.e("YL_CRASH", "browse refresh", e)
            } finally {
                loading = false
            }
        }
    }

    /** 进入子文件夹 */
    fun enterFolder(f: ShareFile) {
        if (!f.isdir) return
        dirFids.add(f.fid)
        currentDir = f.fid
        crumbNames = crumbNames + f.fname
        files = emptyList()
        refresh()
    }

    /** 回退到上级目录 */
    fun upOneLevel() {
        jumpTo(dirFids.size - 2)
    }

    /** 面包屑点击：跳到第 [index] 层（0 = 根） */
    fun jumpTo(index: Int) {
        if (index < 0 || index >= dirFids.size) return
        while (dirFids.size > index + 1) dirFids.removeAt(dirFids.size - 1)
        crumbNames = crumbNames.take(index + 1)
        currentDir = dirFids[index]
        files = emptyList()
        refresh()
    }

    /** 文件直下：取直链后入队，进度见下载页 */
    fun download(f: ShareFile) {
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            message = null
            try {
                enqueueFor(activePlatform, f)
            } catch (e: Exception) {
                message = "下载失败：${e.message ?: e.javaClass.simpleName}"
                Log.e("YL_CRASH", "browse download", e)
            }
        }
    }

    /** 取直链并入队（挂起函数体里直接调 suspend 的 manager.enqueue） */
    private suspend fun enqueueFor(p: SharePlatform, f: ShareFile) {
        when (p) {
            SharePlatform.QUARK -> {
                val cred = quarkAccountRepo.getFreshCookie()
                if (cred == null) { message = "夸克登录已失效"; return }
                val dl = quarkResolveRepo.getDownloadLink(f.fid, cred).getOrNull()
                    ?: run { message = "获取下载链接失败"; return }
                manager.enqueue(dl.downloadUrl, dl.filename, PlatformSupport.headers(p, cred), dl.size, dl.isHls, dl.cleanupDirFid.orEmpty())
            }
            SharePlatform.UC -> {
                val cred = ucAccountRepo.getFreshCookie()
                if (cred == null) { message = "UC 登录已失效"; return }
                val dl = ucResolveRepo.getDownloadLink(f.fid, cred).getOrNull()
                    ?: run { message = "获取下载链接失败"; return }
                manager.enqueue(dl.downloadUrl, dl.filename, PlatformSupport.headers(p, cred), dl.size, dl.isHls, dl.cleanupDirFid.orEmpty())
            }
            SharePlatform.XUNLEI -> {
                val acc = xunleiAccountRepo.getAccount()
                if (acc == null) { message = "迅雷登录已失效"; return }
                val dl = xunleiResolveRepo.getDownloadLink(f.fid, "").getOrNull()
                    ?: run { message = "获取下载链接失败"; return }
                manager.enqueue(dl.downloadUrl, dl.filename, emptyMap(), dl.size, dl.isHls, dl.cleanupDirFid.orEmpty())
            }
        }
    }
}
