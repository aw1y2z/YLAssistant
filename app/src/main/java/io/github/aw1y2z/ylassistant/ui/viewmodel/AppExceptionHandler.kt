package io.github.aw1y2z.ylassistant.ui.viewmodel

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * 全局协程异常兜底：挂到各 ViewModel 的 viewModelScope 上，
 * 确保任何未显式捕获的异常只记录日志、不再直接终止进程（避免“点一下就闪退”）。
 * 业务层仍应尽量 try/catch 把错误显示到 UI（见 DriveBrowseViewModel 等）。
 */
val AppCoroutineExceptionHandler = CoroutineExceptionHandler { context, e ->
    Log.e("YL_CRASH", "uncaught exception in $context", e)
}
