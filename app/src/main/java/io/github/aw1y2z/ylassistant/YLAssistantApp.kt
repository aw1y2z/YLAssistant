package io.github.aw1y2z.ylassistant

import android.app.Application
import io.github.aw1y2z.ylassistant.di.DownloadCore

class YLAssistantApp : Application() {

    lateinit var core: DownloadCore
        private set

    override fun onCreate() {
        super.onCreate()
        // 下载闭环组合根：进程级单例（DownloadManager 自带私有协程作用域，实例须唯一）
        core = DownloadCore(this)
    }
}
