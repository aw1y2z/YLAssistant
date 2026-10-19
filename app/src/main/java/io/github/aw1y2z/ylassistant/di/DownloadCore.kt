package io.github.aw1y2z.ylassistant.di

import android.content.Context
import io.github.aw1y2z.ylassistant.data.db.InMemoryQuarkAccountStore
import io.github.aw1y2z.ylassistant.data.db.InMemoryTaskDao
import io.github.aw1y2z.ylassistant.data.db.InMemoryUcAccountStore
import io.github.aw1y2z.ylassistant.data.db.InMemoryXunleiAccountStore
import io.github.aw1y2z.ylassistant.data.download.ChunkDownloader
import io.github.aw1y2z.ylassistant.data.download.DownloadManager
import io.github.aw1y2z.ylassistant.data.network.HttpClients
import io.github.aw1y2z.ylassistant.data.network.QuarkApi
import io.github.aw1y2z.ylassistant.data.network.UCApi
import io.github.aw1y2z.ylassistant.data.network.XunleiApi
import io.github.aw1y2z.ylassistant.data.prefs.SettingsRepository
import io.github.aw1y2z.ylassistant.data.repository.QuarkAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.QuarkResolveRepository
import io.github.aw1y2z.ylassistant.data.repository.UCAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.UCResolveRepository
import io.github.aw1y2z.ylassistant.data.repository.XunleiAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.XunleiResolveRepository

/**
 * 下载闭环组合根（夸克 / UC / 迅雷）。
 *
 * 手工装配（原工程无 DI 框架）：把数据层各件组装成 DownloadManager 与仓库，
 * 由 [YLAssistantApp] 在 Application.onCreate 构造一次，全进程单例。
 */
class DownloadCore(context: Context) {

    private val app = context.applicationContext

    val settings = SettingsRepository(app)
    val taskDao = InMemoryTaskDao(app)

    // 夸克
    val quarkAccountStore = InMemoryQuarkAccountStore(app)
    val quarkApi = QuarkApi()
    val quarkAccountRepo = QuarkAccountRepository(quarkAccountStore, quarkApi)
    val quarkResolveRepo = QuarkResolveRepository(quarkApi)

    // UC
    val ucAccountStore = InMemoryUcAccountStore(app)
    val ucApi = UCApi()
    val ucAccountRepo = UCAccountRepository(ucAccountStore, ucApi)
    val ucResolveRepo = UCResolveRepository(ucApi)

    // 迅雷
    val xunleiAccountStore = InMemoryXunleiAccountStore(app)
    val xunleiApi = XunleiApi()
    val xunleiAccountRepo = XunleiAccountRepository(xunleiAccountStore, xunleiApi)
    val xunleiResolveRepo = XunleiResolveRepository(
        api = xunleiApi,
        accountProvider = { xunleiAccountRepo.getAccount()?.accessToken },
        deviceIdProvider = { xunleiAccountRepo.getAccount()?.deviceId },
        captchaProvider = { xunleiAccountRepo.getAccount()?.captchaToken },
        refreshProvider = {
            val acc = xunleiAccountRepo.getAccount()
            if (acc == null) null
            else xunleiApi.refreshToken(acc.refreshToken, acc.deviceId)?.let { (at, rt) ->
                xunleiAccountRepo.updateTokens(at, rt)
                at to rt
            }
        },
    )

    val downloadManager = DownloadManager(
        context = app,
        dao = taskDao,
        downloader = ChunkDownloader { HttpClients.downloadClient() },
        // headersStore 用默认 DownloadHeadersStore()：DownloadCore 全进程仅构造一次，实例稳定
        threadProvider = { settings.downloadThreads },
        saveDirProvider = { settings.downloadDirUri },
        concurrencyProvider = { settings.maxConcurrentDownloads },
        speedLimitProvider = { settings.downloadSpeedLimit },
        retryCountProvider = { settings.downloadRetryCount },
        keepWhenLockedProvider = { settings.keepDownloadWhenLocked },
        showSpeedProvider = { settings.notificationShowSpeed },
        // beforeDownloadProvider / storagePermissionProvider 由 UI 层（AssistantApp）注入动态权限申请
        // 仅夸克会把临时转存目录 fid 带回（UC/迅雷的 getShareDownloadLink 内部已自清理）
        persistentCleanupHandler = { cleanupId ->
            if (cleanupId.isNotBlank()) {
                quarkAccountRepo.getFreshCookie()?.let { cookie ->
                    quarkResolveRepo.cleanupTempDir(cleanupId, cookie)
                }
            }
        },
    )
}
