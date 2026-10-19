package io.github.aw1y2z.ylassistant.ui.preview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.aw1y2z.ylassistant.ui.theme.Palette

/**
 * 引擎接入前的占位数据，只用于评估 UI 视觉。
 * 接入 DownloadManager 后整个文件删除，TaskCard 改读真实任务流。
 */
data class TaskPreview(
    val icon: ImageVector,
    val tint: Color,
    val name: String,
    val summary: String,
    val progress: Float?,
    val rightText: String,
)

/** 网盘账号：每个网盘各自登录，互不影响 */
data class SampleAccount(
    val name: String,
    val status: String,
    val tint: Color,
    val loggedIn: Boolean,
)

object SampleData {

    // 骨架阶段：夸克按已登录展示文件树预览，其余未登录
    val accounts = listOf(
        SampleAccount("夸克网盘", "已登录 · aw1y2z", Palette.Quark, loggedIn = true),
        SampleAccount("UC 网盘", "未登录", Palette.Uc, loggedIn = false),
        SampleAccount("迅雷网盘", "未登录", Palette.Xunlei, loggedIn = false),
    )

    val running = listOf(
        TaskPreview(
            icon = Icons.Filled.Movie,
            tint = Palette.Quark,
            name = "电影合集.zip",
            summary = "2.4 GB · 5.2 MB/s · 约 3 分钟",
            progress = 0.62f,
            rightText = "62%",
        ),
        TaskPreview(
            icon = Icons.Filled.Description,
            tint = Palette.Warning,
            name = "文档.pdf",
            summary = "12 MB · 等待中",
            progress = 0f,
            rightText = "0%",
        ),
    )

    val done = listOf(
        TaskPreview(
            icon = Icons.Filled.MusicNote,
            tint = Palette.Success,
            name = "歌曲.mp3",
            summary = "8.2 MB · 今天 14:32",
            progress = null,
            rightText = "已完成",
        ),
    )
}
