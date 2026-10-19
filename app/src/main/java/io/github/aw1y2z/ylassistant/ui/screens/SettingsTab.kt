package io.github.aw1y2z.ylassistant.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aw1y2z.ylassistant.core.Settings
import io.github.aw1y2z.ylassistant.data.download.DownloadSaver
import io.github.aw1y2z.ylassistant.data.prefs.SettingsRepository
import io.github.aw1y2z.ylassistant.di.DownloadCore
import io.github.aw1y2z.ylassistant.ui.components.BooleanSwitch
import io.github.aw1y2z.ylassistant.ui.components.CardColumn
import io.github.aw1y2z.ylassistant.ui.components.InfoRow
import io.github.aw1y2z.ylassistant.ui.components.PageTitle
import io.github.aw1y2z.ylassistant.ui.components.SectionTitle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val C_CLOSED = 0
private const val C_THREADS = 1
private const val C_CONCURRENT = 2
private const val C_AFTER = 3
private const val C_STORAGE = 4

@Composable
fun SettingsTab(core: DownloadCore) {
    val context = LocalContext.current
    val activity = context as? Activity
    val repo: SettingsRepository = core.settings

    var followSystem by remember { mutableStateOf(Settings.followSystem) }
    var darkMode by remember { mutableStateOf(Settings.darkMode) }
    var threads by remember { mutableIntStateOf(repo.downloadThreads) }
    var concurrent by remember { mutableIntStateOf(repo.maxConcurrentDownloads) }
    var autoAll by remember { mutableStateOf(repo.autoDownloadAll) }
    var dirUri by remember { mutableStateOf(repo.downloadDirUri) }
    var choice by remember { mutableIntStateOf(C_CLOSED) }

    // 下载保存目录选择（SAF，适配各版本分区存储；持久授权保证重启后仍可写）
    val dirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            repo.downloadDirUri = uri.toString()
            dirUri = uri.toString()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        item { PageTitle("设置") }

        item { SectionTitle("下载") }
        item {
            CardColumn {
                ArrowPreference(
                    title = "下载线程数",
                    summary = "当前 $threads 线程（分片并发）",
                    onClick = { choice = C_THREADS },
                )
                ArrowPreference(
                    title = "同时下载数",
                    summary = "当前 $concurrent 个任务",
                    onClick = { choice = C_CONCURRENT },
                )
                ArrowPreference(
                    title = "默认存储路径",
                    summary = dirUri?.let { "已自定义：${DownloadSaver.safDirDisplay(it)}" }
                        ?: "系统下载目录",
                    onClick = {
                        if (dirUri != null) choice = C_STORAGE else dirLauncher.launch(null)
                    },
                )
                ArrowPreference(
                    title = "解析后行为",
                    summary = if (autoAll) "自动下载全部文件" else "先选择文件",
                    onClick = { choice = C_AFTER },
                )
            }
        }

        item { SectionTitle("外观") }
        item {
            CardColumn {
                ArrowPreference(
                    title = "主题模式",
                    summary = when {
                        followSystem -> "跟随系统"
                        darkMode -> "深色"
                        else -> "浅色"
                    },
                    onClick = { },
                )
                BooleanSwitch(
                    title = "深色模式",
                    summary = "关闭“跟随系统”后生效",
                    checked = darkMode,
                    onCheckedChange = {
                        darkMode = it
                        Settings.darkMode = it
                        activity?.recreate()
                    },
                )
            }
        }

        item { SectionTitle("关于") }
        item {
            CardColumn {
                InfoRow("版本", versionName(context))
            }
        }
    }

    // 选择弹层
    when (choice) {
        C_THREADS -> IntOptionSheet(
            title = "下载线程数",
            options = listOf(8, 16, 32, 64),
            current = threads,
            onPick = { v -> threads = v; repo.downloadThreads = v; choice = C_CLOSED },
            onDismiss = { choice = C_CLOSED },
        )
        C_CONCURRENT -> IntOptionSheet(
            title = "同时下载数",
            options = listOf(2, 4, 6, 8),
            current = concurrent,
            onPick = { v -> concurrent = v; repo.maxConcurrentDownloads = v; choice = C_CLOSED },
            onDismiss = { choice = C_CLOSED },
        )
        C_AFTER -> BooleanOptionSheet(
            title = "解析后行为",
            options = listOf("先选择文件", "自动下载全部文件"),
            current = autoAll,
            onPick = { v -> autoAll = v; repo.autoDownloadAll = v; choice = C_CLOSED },
            onDismiss = { choice = C_CLOSED },
        )
        C_STORAGE -> StorageSheet(
            current = dirUri,
            onPickDir = { dirLauncher.launch(null) },
            onRestoreDefault = {
                dirUri = null
                repo.downloadDirUri = null
                choice = C_CLOSED
            },
            onDismiss = { choice = C_CLOSED },
        )
    }
}

/** 存储路径选择弹层：重新选择 + 恢复默认（Miuix 一致的行样式） */
@Composable
private fun StorageSheet(
    current: String?,
    onPickDir: () -> Unit,
    onRestoreDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayBottomSheet(show = true, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "默认存储路径",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            OptionRow(label = "重新选择目录", selected = false) {
                onDismiss()
                onPickDir()
            }
            if (current != null) {
                OptionRow(label = "恢复系统下载目录", selected = false, onClick = onRestoreDefault)
            }
        }
    }
}

/** 数字选项选择弹层（Miuix 底弹层 + 单选行） */
@Composable
private fun IntOptionSheet(
    title: String,
    options: List<Int>,
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayBottomSheet(show = true, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            options.forEach { v ->
                OptionRow(label = "$v", selected = v == current) { onPick(v) }
            }
        }
    }
}

/** 是/否二选一弹层（[options] 固定两项：index0=关，index1=开） */
@Composable
private fun BooleanOptionSheet(
    title: String,
    options: List<String>,
    current: Boolean,
    onPick: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayBottomSheet(show = true, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            options.forEachIndexed { i, label ->
                OptionRow(label = label, selected = (i == 1) == current) { onPick(i == 1) }
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onBackground,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun versionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "-"
