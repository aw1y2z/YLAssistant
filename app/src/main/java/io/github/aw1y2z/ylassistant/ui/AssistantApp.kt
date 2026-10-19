package io.github.aw1y2z.ylassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.aw1y2z.ylassistant.YLAssistantApp
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.di.DownloadCore
import io.github.aw1y2z.ylassistant.ui.screens.DownloadTab
import io.github.aw1y2z.ylassistant.ui.screens.FilesTab
import io.github.aw1y2z.ylassistant.ui.screens.SettingsTab
import io.github.aw1y2z.ylassistant.ui.sheets.LoginSheet
import io.github.aw1y2z.ylassistant.ui.sheets.ParseSheet
import io.github.aw1y2z.ylassistant.ui.sheets.WebLoginScreen
import io.github.aw1y2z.ylassistant.ui.viewmodel.AccountViewModel
import io.github.aw1y2z.ylassistant.ui.viewmodel.DriveBrowseViewModel
import io.github.aw1y2z.ylassistant.ui.viewmodel.ResolveViewModel
import io.github.aw1y2z.ylassistant.ui.viewmodel.accountVm
import io.github.aw1y2z.ylassistant.ui.viewmodel.driveBrowseVm
import io.github.aw1y2z.ylassistant.ui.viewmodel.resolveVm
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val TAB_DOWNLOAD = 0
private const val TAB_FILES = 1
private const val TAB_SETTINGS = 2

private const val SHEET_CLOSED = -1
private const val SHEET_LINK = 0
private const val SHEET_FILES = 1

@Composable
fun AssistantApp() {
    val core: DownloadCore = (LocalContext.current.applicationContext as YLAssistantApp).core

    var tab by remember { mutableIntStateOf(TAB_DOWNLOAD) }
    var sheet by remember { mutableIntStateOf(SHEET_CLOSED) }
    var loginPlatform by remember { mutableStateOf<SharePlatform?>(null) }
    var webLoginPlatform by remember { mutableStateOf<SharePlatform?>(null) }

    val resolveVm: ResolveViewModel = viewModel(factory = resolveVm(core))
    val accountVm: AccountViewModel = viewModel(factory = accountVm(core))
    val browseVm: DriveBrowseViewModel = viewModel(factory = driveBrowseVm(core))

    // 动态权限：下载管理器在需用时触发申请（通知 API33+ / 存储 API≤28），
    // launcher 必须由 Activity 提供，故在这里把 launcher + CompletableDeferred 注入单例 DownloadManager
    val ctx = LocalContext.current
    val dm = core.downloadManager
    var pendingNotif by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { g -> pendingNotif?.complete(g); pendingNotif = null }
    var pendingStorage by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { g -> pendingStorage?.complete(g); pendingStorage = null }

    DisposableEffect(dm) {
        dm.beforeDownloadProvider = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                val d = CompletableDeferred<Boolean>()
                withContext(Dispatchers.Main) {
                    pendingNotif = d
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                d.await()
            }
            true
        }
        dm.storagePermissionProvider = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                true
            } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                val d = CompletableDeferred<Boolean>()
                withContext(Dispatchers.Main) {
                    pendingStorage = d
                    storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                d.await()
            }
        }
        onDispose { }
    }

    // 直链就绪 → 入队下载（downloadLink 消费后会被置空）
    LaunchedEffect(resolveVm.downloadLink) {
        resolveVm.downloadLink?.let { resolveVm.startDownload(it) }
    }
    // 入队成功 → 关弹层、切到下载页
    LaunchedEffect(resolveVm.downloadStarted) {
        if (resolveVm.downloadStarted) {
            resolveVm.consumeDownloadStarted()
            sheet = SHEET_CLOSED
            tab = TAB_DOWNLOAD
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BottomTab("下载", MiuixIcons.Download, tab == TAB_DOWNLOAD) { tab = TAB_DOWNLOAD }
                BottomTab("文件", MiuixIcons.Folder, tab == TAB_FILES) { tab = TAB_FILES }
                BottomTab("设置", MiuixIcons.Settings, tab == TAB_SETTINGS) { tab = TAB_SETTINGS }
            }
        },
        containerColor = MiuixTheme.colorScheme.surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                .padding(padding)
                .statusBarsPadding(),
        ) {
            when (tab) {
                TAB_DOWNLOAD -> DownloadTab(core) { sheet = SHEET_LINK }
                TAB_FILES -> FilesTab(
                    core = core,
                    accountVm = accountVm,
                    browseVm = browseVm,
                    onLogin = { p -> loginPlatform = p },
                )
                else -> SettingsTab(core)
            }

            if (tab == TAB_DOWNLOAD) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    FloatingActionButton(
                        onClick = { sheet = SHEET_LINK },
                        modifier = Modifier.padding(20.dp),
                    ) {
                        Icon(imageVector = MiuixIcons.Add, contentDescription = "解析新链接")
                    }
                }
            }
        }

        // 解析弹层（挂在 Scaffold 内部）
        OverlayBottomSheet(
            show = sheet >= 0,
            onDismissRequest = { sheet = SHEET_CLOSED },
        ) {
            when (sheet) {
                SHEET_LINK -> ParseSheet.Step1(resolveVm, accountVm, onLogin = { p -> loginPlatform = p }) {
                    sheet = SHEET_FILES
                }
                SHEET_FILES -> ParseSheet.Step2(resolveVm) {
                    sheet = SHEET_CLOSED
                }
                else -> {}
            }
        }

        // 平台登录弹层（cookie / token）
        val lp = loginPlatform
        if (lp != null) {
            OverlayBottomSheet(
                show = true,
                onDismissRequest = { loginPlatform = null },
            ) {
                LoginSheet(
                    platform = lp,
                    vm = accountVm,
                    onDone = { loginPlatform = null },
                    onWebLogin = { p ->
                        loginPlatform = null
                        webLoginPlatform = p
                    },
                )
            }
        }

        // 夸克 / UC 网页登录全屏页（在登录弹层之上）
        val wp = webLoginPlatform
        if (wp != null) {
            WebLoginScreen(
                platform = wp,
                vm = accountVm,
                onBack = { webLoginPlatform = null },
            ) { webLoginPlatform = null }
        }
    }
}

@Composable
private fun BottomTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color)
        Text(text = label, style = MiuixTheme.textStyles.body2, color = color)
    }
}
