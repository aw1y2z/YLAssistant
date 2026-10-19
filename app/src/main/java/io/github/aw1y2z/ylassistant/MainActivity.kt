package io.github.aw1y2z.ylassistant

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.github.aw1y2z.ylassistant.core.Settings
import io.github.aw1y2z.ylassistant.ui.AssistantApp
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(applicationContext)
        setupSystemBars()

        setContent {
            val mode = when {
                Settings.followSystem -> ColorSchemeMode.System
                Settings.darkMode -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.Light
            }
            // ThemeController 只能在 Compose 树最外层构造，切换深浅色后由 recreate() 重建
            val controller = remember(mode) { ThemeController(colorSchemeMode = mode) }

            // Miuix 0.9.4 的 NavigationBar / OverlayBottomSheet 依赖
            // LocalNavigationEventDispatcherOwner（来自 androidx.navigationevent）。
            // 当前 activity-compose 1.10.0 的 setContent 尚未自动注入该 local，
            // 且 ComponentActivity 也未实现 NavigationEventDispatcherOwner，故手动构造一个 owner 提供。
            val dispatcher = remember { NavigationEventDispatcher() }
            val navOwner = remember(dispatcher) {
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher: NavigationEventDispatcher = dispatcher
                }
            }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navOwner) {
                MiuixTheme(controller) {
                    AssistantApp()
                }
            }
        }
    }

    /**
     * edge-to-edge：内容延展到系统栏下方，由各页自己用 statusBarsPadding() 留出安全区，
     * 这样状态栏与页面背景连成一片，不会出现上下分离的色块。
     */
    private fun setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val dark = if (Settings.followSystem) {
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        } else {
            Settings.darkMode
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
