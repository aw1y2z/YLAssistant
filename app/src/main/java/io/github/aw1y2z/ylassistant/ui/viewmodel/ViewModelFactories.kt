package io.github.aw1y2z.ylassistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.aw1y2z.ylassistant.di.DownloadCore

/** 统一从 [DownloadCore]（YLAssistantApp 单例）构造各 ViewModel。 */

fun downloadVm(core: DownloadCore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DownloadViewModel::class.java))
        return DownloadViewModel(core.downloadManager) as T
    }
}

fun resolveVm(core: DownloadCore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ResolveViewModel::class.java))
        return ResolveViewModel(
            core.quarkAccountRepo,
            core.ucAccountRepo,
            core.xunleiAccountRepo,
            core.quarkResolveRepo,
            core.ucResolveRepo,
            core.xunleiResolveRepo,
            core.downloadManager,
            autoAllProvider = { core.settings.autoDownloadAll },
        ) as T
    }
}

fun accountVm(core: DownloadCore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AccountViewModel::class.java))
        return AccountViewModel(
            core.quarkAccountRepo,
            core.ucAccountRepo,
            core.xunleiAccountRepo,
        ) as T
    }
}

fun driveBrowseVm(core: DownloadCore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DriveBrowseViewModel::class.java))
        return DriveBrowseViewModel(
            core.quarkAccountRepo,
            core.ucAccountRepo,
            core.xunleiAccountRepo,
            core.quarkApi,
            core.ucApi,
            core.xunleiApi,
            core.quarkResolveRepo,
            core.ucResolveRepo,
            core.xunleiResolveRepo,
            core.downloadManager,
        ) as T
    }
}
