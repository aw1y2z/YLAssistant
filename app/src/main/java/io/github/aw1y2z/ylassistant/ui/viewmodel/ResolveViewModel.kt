package io.github.aw1y2z.ylassistant.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aw1y2z.ylassistant.data.download.DownloadManager
import io.github.aw1y2z.ylassistant.data.network.QuarkConstants
import io.github.aw1y2z.ylassistant.data.network.ShareLinkParser
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.data.network.UCConstants
import io.github.aw1y2z.ylassistant.data.network.model.DownloadLink
import io.github.aw1y2z.ylassistant.data.network.model.ShareFile
import io.github.aw1y2z.ylassistant.data.network.model.ShareSession
import io.github.aw1y2z.ylassistant.data.repository.QuarkAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.QuarkResolveRepository
import io.github.aw1y2z.ylassistant.data.repository.ShareResolveRepository
import io.github.aw1y2z.ylassistant.data.repository.UCAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.UCResolveRepository
import io.github.aw1y2z.ylassistant.data.repository.XunleiAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.XunleiResolveRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * 分享解析 ViewModel（下载闭环：夸克 / UC / 迅雷）。
 *
 * 输入链接 → 判定平台 → 建会话 → 列文件 → 取直链 → 入队下载。
 * 各平台凭证/仓库/请求头在此按 [SharePlatform] 收敛。
 */
class ResolveViewModel(
    private val quarkAccountRepo: QuarkAccountRepository,
    private val ucAccountRepo: UCAccountRepository,
    private val xunleiAccountRepo: XunleiAccountRepository,
    private val quarkResolveRepo: QuarkResolveRepository,
    private val ucResolveRepo: UCResolveRepository,
    private val xunleiResolveRepo: XunleiResolveRepository,
    private val manager: DownloadManager,
    /** 解析后行为：true=解析成功自动下载全部文件，false=先选择文件（默认） */
    private val autoAllProvider: () -> Boolean = { false },
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Detail(val session: ShareSession, val files: List<ShareFile>) : UiState
        data class Error(val message: String) : UiState
    }

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var downloadLink by mutableStateOf<DownloadLink?>(null)
        private set

    var downloadError by mutableStateOf<String?>(null)
        private set

    /** 是否正在取直链（转存+取链，大文件可能较久），UI 据此显示进度提示 */
    var fetchLoading by mutableStateOf(false)
        private set

    /** 入队后由 UI 消费，切换到下载页并关闭弹层 */
    var downloadStarted by mutableStateOf(false)
        private set

    private var platform: SharePlatform = SharePlatform.QUARK
    private var session: ShareSession? = null

    /** 子目录导航栈（空 = 分享根目录；栈顶为当前目录的入口文件项） */
    var dirStack by mutableStateOf(listOf<ShareFile>())
        private set

    private fun repo(p: SharePlatform): ShareResolveRepository = when (p) {
        SharePlatform.QUARK -> quarkResolveRepo
        SharePlatform.UC -> ucResolveRepo
        SharePlatform.XUNLEI -> xunleiResolveRepo
    }

    private fun defaultDir(p: SharePlatform): String = when (p) {
        SharePlatform.QUARK -> QuarkConstants.DEFAULT_PDIR_FID
        SharePlatform.UC -> UCConstants.DEFAULT_PDIR_FID
        SharePlatform.XUNLEI -> ""
    }

    private suspend fun credentialFor(p: SharePlatform): String? = when (p) {
        SharePlatform.QUARK -> quarkAccountRepo.getFreshCookie()
        SharePlatform.UC -> ucAccountRepo.getFreshCookie()
        // 迅雷仓库内部走 provider 取 access_token，这里只需确认已登录，cred 传空
        SharePlatform.XUNLEI ->
            xunleiAccountRepo.getAccount()?.accessToken?.takeIf { it.isNotBlank() }?.let { "" }
    }

    /** 链接 → 判定平台 → 会话 → 根目录文件列表 */
    fun startResolve(link: String, pwd: String?) {
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            uiState = UiState.Loading
            dirStack = emptyList()
            val parsed = ShareLinkParser.parse(link)
                ?: run {
                    uiState = UiState.Error("无法识别分享链接")
                    return@launch
                }
            platform = parsed.platform
            val cred = credentialFor(platform)
            if (cred == null) {
                uiState = UiState.Error("请先登录${nameOf(platform)}")
                return@launch
            }
            repo(platform).createSession(link, pwd, cred)
                .onSuccess { s ->
                    session = s
                    loadFiles(s, cred)
                }
                .onFailure { uiState = UiState.Error(it.message ?: "解析失败") }
        }
    }

    private suspend fun loadFiles(s: ShareSession, cred: String) {
        repo(platform).listFiles(s, defaultDir(platform), cred)
            .onSuccess { files ->
                uiState = UiState.Detail(s, files)
                if (autoAllProvider()) downloadAllFiles(files)
            }
            .onFailure { uiState = UiState.Error(it.message ?: "获取文件列表失败") }
    }

    /** 进入分享内的子目录 */
    fun openDir(dir: ShareFile) {
        val s = session ?: return
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            uiState = UiState.Loading
            val cred = credentialFor(platform)
            if (cred == null) {
                uiState = UiState.Error("登录已失效，请重新登录")
                return@launch
            }
            repo(platform).listFiles(s, dir.fid, cred)
                .onSuccess { files ->
                    dirStack = dirStack + dir
                    uiState = UiState.Detail(s, files)
                }
                .onFailure { uiState = UiState.Error(it.message ?: "获取文件夹内容失败") }
        }
    }

    /** 返回上一级目录（已在根则不动，UI 据此决定是否关弹层） */
    fun goUp() {
        val s = session ?: return
        val target = dirStack.dropLast(1)
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            uiState = UiState.Loading
            val cred = credentialFor(platform)
            if (cred == null) {
                uiState = UiState.Error("登录已失效，请重新登录")
                return@launch
            }
            val fid = target.lastOrNull()?.fid ?: defaultDir(platform)
            repo(platform).listFiles(s, fid, cred)
                .onSuccess { files ->
                    dirStack = target
                    uiState = UiState.Detail(s, files)
                }
                .onFailure { uiState = UiState.Error(it.message ?: "返回上级失败") }
        }
    }

    /** 取直链（夸克/UC：转存或直接取；迅雷：转存→详情直链，已自清理临时文件） */
    fun fetchDownloadLink(file: ShareFile) {
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            fetchLoading = true
            try {
                downloadLink = null
                downloadError = null
                val s = session ?: run {
                    downloadError = "请先解析分享"
                    return@launch
                }
                val cred = credentialFor(platform)
                if (cred == null) {
                    downloadError = "登录已失效，请重新登录"
                    return@launch
                }
                try {
                    // 大文件服务端转存任务轮询较久，放宽到 60s
                    withTimeout(60_000L) {
                        repo(platform).getShareDownloadLink(s, file, cred)
                            .onSuccess { downloadLink = it }
                            .onFailure { downloadError = it.message ?: "获取下载链接失败" }
                    }
                } catch (_: TimeoutCancellationException) {
                    downloadError = "获取失败，请重试"
                }
            } finally {
                fetchLoading = false
            }
        }
    }

    /** 直链入队：按平台带凭证/请求头；夸克下载完成后清理临时转存目录。 */
    fun startDownload(link: DownloadLink) {
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            downloadLink = null
            val cred = credentialFor(platform)
            if (cred == null) {
                downloadError = "请先登录${nameOf(platform)}"
                return@launch
            }
            try {
                enqueueOne(link, cred)
                downloadStarted = true
            } catch (e: Exception) {
                downloadError = "入队失败：${e.message ?: e.javaClass.simpleName}"
                Log.e("YL_CRASH", "startDownload", e)
            }
        }
    }

    /** 自动下载：解析成功后把当前（根）目录下的全部文件逐个取直链并入队。 */
    fun downloadAllFiles(files: List<ShareFile>) {
        val s = session ?: return
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            downloadError = null
            val cred = credentialFor(platform)
            if (cred == null) {
                downloadError = "请先登录${nameOf(platform)}"
                return@launch
            }
            var ok = 0
            for (f in files) {
                if (f.isdir) continue
                val link = repo(platform).getShareDownloadLink(s, f, cred).getOrNull() ?: continue
                try {
                    enqueueOne(link, cred)
                    ok += 1
                } catch (e: Exception) {
                    Log.w("YL_DL", "auto enqueue failed: ${e.message}")
                }
            }
            if (ok > 0) downloadStarted = true
            else downloadError = "自动下载失败，请手动选择文件"
        }
    }

    /** 把一条直链入下载队列（共享给单文件/自动全部下载），夸克下载完成后清理临时转存目录。 */
    private suspend fun enqueueOne(link: DownloadLink, cred: String) {
        manager.enqueue(
            url = link.downloadUrl,
            fileName = link.filename,
            headers = headersFor(platform, cred),
            size = link.size,
            isHls = link.isHls,
            cleanupId = link.cleanupDirFid.orEmpty(),
        ) {
            if (platform == SharePlatform.QUARK) {
                link.cleanupDirFid?.let { quarkResolveRepo.cleanupTempDir(it, cred) }
            }
        }
    }

    private fun headersFor(p: SharePlatform, cred: String): Map<String, String> = when (p) {
        SharePlatform.QUARK -> mapOf(
            "Cookie" to cred,
            "User-Agent" to QuarkConstants.API_USER_AGENT,
            "Referer" to QuarkConstants.DOWNLOAD_REFERER,
            "Accept-Encoding" to "identity",
        )
        SharePlatform.UC -> mapOf(
            "Cookie" to cred,
            "User-Agent" to UCConstants.CLOUD_UA,
            "Referer" to UCConstants.DOWNLOAD_REFERER,
            "Origin" to UCConstants.WEB_ORIGIN,
        )
        // 迅雷直链自带 CDN 签名，不额外带请求头（加错 UA 会触发降级）
        SharePlatform.XUNLEI -> emptyMap()
    }

    private fun nameOf(p: SharePlatform): String = when (p) {
        SharePlatform.QUARK -> "夸克网盘"
        SharePlatform.UC -> "UC 网盘"
        SharePlatform.XUNLEI -> "迅雷网盘"
    }

    fun consumeDownloadStarted() {
        downloadStarted = false
    }

    fun consumeDownloadError() {
        downloadError = null
    }
}
