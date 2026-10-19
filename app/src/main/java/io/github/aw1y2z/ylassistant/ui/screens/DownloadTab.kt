package io.github.aw1y2z.ylassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aw1y2z.ylassistant.data.db.DownloadTaskEntity
import io.github.aw1y2z.ylassistant.data.download.DownloadStats
import io.github.aw1y2z.ylassistant.di.DownloadCore
import io.github.aw1y2z.ylassistant.ui.components.EmptyHint
import io.github.aw1y2z.ylassistant.ui.components.PageTitle
import io.github.aw1y2z.ylassistant.ui.components.SectionTitle
import io.github.aw1y2z.ylassistant.ui.theme.Palette
import io.github.aw1y2z.ylassistant.ui.viewmodel.DownloadViewModel
import io.github.aw1y2z.ylassistant.ui.viewmodel.downloadVm
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DownloadTab(core: DownloadCore, onNewTask: () -> Unit) {
    val vm: DownloadViewModel = viewModel(factory = downloadVm(core))
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    val active = tasks.filter {
        it.status == DownloadTaskEntity.STATUS_DOWNLOADING ||
            it.status == DownloadTaskEntity.STATUS_PENDING ||
            it.status == DownloadTaskEntity.STATUS_FINALIZING
    }
    val done = tasks.filter { it.status == DownloadTaskEntity.STATUS_COMPLETED }
    val stopped = tasks.filter {
        it.status == DownloadTaskEntity.STATUS_PAUSED ||
            it.status == DownloadTaskEntity.STATUS_FAILED
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
    ) {
        item { PageTitle("下载") }

        if (tasks.isEmpty()) {
            item { EmptyHint("还没有下载任务\n粘贴云盘分享链接即可开始") }
            item {
                // 空态即入口
                Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp)) {
                    top.yukonga.miuix.kmp.basic.Button(
                        onClick = onNewTask,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("粘贴链接解析") }
                }
            }
        } else {
            if (active.isNotEmpty()) {
                item { SectionTitle("进行中") }
                items(active) { t ->
                    TaskCard(
                        task = t, stat = stats[t.id],
                        onPause = { vm.pause(t.id) }, onResume = { vm.resume(t.id) }, onDelete = { vm.remove(t.id) },
                    )
                }
            }
            if (done.isNotEmpty()) {
                item { SectionTitle("已完成") }
                items(done) { t ->
                    TaskCard(
                        task = t, stat = stats[t.id],
                        onPause = { }, onResume = { vm.resume(t.id) }, onDelete = { vm.remove(t.id) },
                    )
                }
            }
            if (stopped.isNotEmpty()) {
                item { SectionTitle("已暂停 / 失败") }
                items(stopped) { t ->
                    TaskCard(
                        task = t, stat = stats[t.id],
                        onPause = { vm.pause(t.id) }, onResume = { vm.resume(t.id) }, onDelete = { vm.remove(t.id) },
                    )
                }
            }
        }
    }
}

private fun iconFor(name: String): Pair<ImageVector, Color> = when {
    name.contains(".mp4", true) || name.contains(".mkv", true) ||
        name.contains(".mov", true) || name.contains(".webm", true) ->
        Icons.Filled.Movie to Palette.Quark
    name.contains(".mp3", true) || name.contains(".m4a", true) ||
        name.contains(".flac", true) -> Icons.Filled.MusicNote to Palette.Success
    name.contains(".zip", true) || name.contains(".rar", true) ||
        name.contains(".7z", true) -> Icons.Filled.Description to Palette.Warning
    else -> Icons.Filled.Description to Palette.Uc
}

@Composable
private fun TaskCard(
    task: DownloadTaskEntity,
    stat: DownloadStats?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    val (icon, tint) = iconFor(task.fileName)
    val progress = if (task.totalSize > 0) task.downloadedSize.toFloat() / task.totalSize.toFloat() else null
    val isActive = task.status == DownloadTaskEntity.STATUS_DOWNLOADING ||
        task.status == DownloadTaskEntity.STATUS_FINALIZING
    val isFailed = task.status == DownloadTaskEntity.STATUS_FAILED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isFailed) Palette.Danger else tint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        style = MiuixTheme.textStyles.headline1,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isFailed && task.errorMsg.isNotBlank()) {
                            task.errorMsg
                        } else if (task.status == DownloadTaskEntity.STATUS_COMPLETED) {
                            "已完成"
                        } else {
                            statusSummary(task, stat)
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = if (isFailed) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                if (isActive) IconButton(onClick = onPause) {
                    Icon(Icons.Filled.Pause, contentDescription = "暂停", tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                if (!isActive && task.status != DownloadTaskEntity.STATUS_COMPLETED) IconButton(onClick = onResume) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "继续", tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }

            if (progress != null && task.status != DownloadTaskEntity.STATUS_COMPLETED) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stat?.speed?.let { speedText(it) }.orEmpty(),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            } else {
                // 已完成/无进度：轻分隔线提示状态
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
            }
        }
    }
}

private fun statusSummary(task: DownloadTaskEntity, stat: DownloadStats?): String {
    val size = if (task.totalSize > 0) "${formatSize(task.totalSize)}" else ""
    val speed = stat?.speed?.takeIf { it > 0 }?.let { " · ${speedText(it)}" }.orEmpty()
    return listOf(size, "状态 ${DownloadTaskEntity.statusText(task.status)}").filter { it.isNotBlank() }.joinToString(" · ") + speed
}

private fun speedText(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return ""
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    var value = bytesPerSec.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}

private fun formatSize(bytes: Long): String = speedText(bytes).replace("B/s", "B")
