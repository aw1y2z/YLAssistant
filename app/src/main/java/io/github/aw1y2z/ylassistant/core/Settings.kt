package io.github.aw1y2z.ylassistant.core

import android.content.Context
import android.content.SharedPreferences

/**
 * 本机设置。当前只承载界面骨架需要的少量项，
 * 后续接入下载引擎时再补保存目录、限速等完整字段。
 */
object Settings {

    private const val FILE = "ylassistant_settings"
    private const val KEY_FOLLOW_SYSTEM = "follow_system"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_THREADS = "download_threads"
    private const val KEY_CONCURRENT = "max_concurrent"
    private const val KEY_SPEED_LIMIT = "speed_limit_mb"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    /** 深浅色是否跟随系统；关闭时用 [darkMode] 决定 */
    var followSystem: Boolean
        get() = prefs.getBoolean(KEY_FOLLOW_SYSTEM, true)
        set(value) = prefs.edit().putBoolean(KEY_FOLLOW_SYSTEM, value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var downloadThreads: Int
        get() = prefs.getInt(KEY_THREADS, 16)
        set(value) = prefs.edit().putInt(KEY_THREADS, value).apply()

    var maxConcurrent: Int
        get() = prefs.getInt(KEY_CONCURRENT, 3)
        set(value) = prefs.edit().putInt(KEY_CONCURRENT, value).apply()

    /** 0 表示不限速 */
    var speedLimitMb: Int
        get() = prefs.getInt(KEY_SPEED_LIMIT, 0)
        set(value) = prefs.edit().putInt(KEY_SPEED_LIMIT, value).apply()
}
