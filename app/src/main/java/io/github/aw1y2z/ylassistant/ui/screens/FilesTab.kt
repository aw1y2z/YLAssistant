package io.github.aw1y2z.ylassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.data.network.model.ShareFile
import io.github.aw1y2z.ylassistant.di.DownloadCore
import io.github.aw1y2z.ylassistant.ui.components.AccountRow
import io.github.aw1y2z.ylassistant.ui.components.CardColumn
import io.github.aw1y2z.ylassistant.ui.components.EmptyHint
import io.github.aw1y2z.ylassistant.ui.components.PageTitle
import io.github.aw1y2z.ylassistant.ui.components.SectionTitle
import io.github.aw1y2z.ylassistant.ui.preview.SampleData
import io.github.aw1y2z.ylassistant.ui.viewmodel.AccountViewModel
import io.github.aw1y2z.ylassistant.ui.viewmodel.DriveBrowseViewModel
import io.github.aw1y2z.ylassistant.ui.viewmodel.PlatformSupport
import top.yukonga.miuix.kmp.basic.BreadcrumbBar
import top.yukonga.miuix.kmp.basic.BreadcrumbItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.FolderFill
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 文件页：账号入口 + 点账号进入该平台的目录浏览。
 *
 * 两级视图：
 *  - 未进入：账号列表（未登录给登录入口）；
 *  - 已进入某账号：面包屑 + 该账号目录浏览，账号区收起，左上「返回」退回到账号列表。
 */
@Composable
fun FilesTab(
    core: DownloadCore,
    accountVm: AccountViewModel,
    browseVm: DriveBrowseViewModel,
    onLogin: (SharePlatform) -> Unit,
) {
    val quarkAccount by accountVm.quarkAccount.collectAsStateWithLifecycle()
    val ucAccount by accountVm.ucAccount.collectAsStateWithLifecycle()
    val xunleiAccount by accountVm.xunleiAccount.collectAsStateWithLifecycle()

    fun loggedInAt(i: Int): Boolean = when (i) {
        0 -> quarkAccount != null
        1 -> ucAccount != null
        else -> xunleiAccount != null
    }

    fun nickAt(i: Int): String = when (i) {
        0 -> quarkAccount?.nickname.orEmpty()
        1 -> ucAccount?.nickname.orEmpty()
        else -> xunleiAccount?.nickname.orEmpty()
    }

    fun platformAt(i: Int): SharePlatform = when (i) {
        0 -> SharePlatform.QUARK
        1 -> SharePlatform.UC
        else -> SharePlatform.XUNLEI
    }

    val accounts = SampleData.accounts

    // 已进入的账号（null = 停留在账号列表）
    var enteredPlatform by remember { mutableStateOf<SharePlatform?>(null) }

    val entered = enteredPlatform
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        if (entered == null) {
            item { PageTitle("文件") }
            item { SectionTitle("账号") }
            item {
                CardColumn {
                    accounts.forEachIndexed { index, account ->
                        val loggedIn = loggedInAt(index)
                        AccountRow(
                            name = account.name,
                            status = if (loggedIn) "已登录 · ${nickAt(index)}" else "未登录",
                            tint = account.tint,
                            actionLabel = if (loggedIn) "登出" else "登录",
                            onAction = {
                                if (loggedIn) accountVm.logout(platformAt(index)) else onLogin(platformAt(index))
                            },
                            onRowClick = {
                                if (loggedIn) {
                                    browseVm.selectPlatform(platformAt(index))
                                    enteredPlatform = platformAt(index)
                                } else onLogin(platformAt(index))
                            },
                        )
                        if (index != accounts.lastIndex) {
                            HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
                        }
                    }
                }
            }
        } else {
            val activePlatform = entered
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { enteredPlatform = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回账号",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "${PlatformSupport.name(activePlatform)} · 我的文件",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            item {
                if (!loggedInAt(when (activePlatform) {
                    SharePlatform.QUARK -> 0
                    SharePlatform.UC -> 1
                    else -> 2
                })) {
                    CardColumn {
                        EmptyHint("登录已失效，请回到账号列表重新登录")
                        TextButton(
                            text = "返回账号列表",
                            onClick = { enteredPlatform = null },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                } else {
                    DriveBrowseCard(browseVm)
                }
            }
        }
    }
}

/** 活动平台的目录浏览卡：面包屑 + 文件夹/文件列表 */
@Composable
private fun DriveBrowseCard(vm: DriveBrowseViewModel) {
    val crumbNames = vm.crumbNames
    val files = vm.files

    CardColumn {
        BreadcrumbBar(
            items = crumbNames.mapIndexed { i, n -> BreadcrumbItem(path = "c$i", text = n) },
            onItemClick = { idx -> vm.jumpTo(idx) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        )

        if (vm.loading) {
            Box(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("加载中…", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        } else {
            vm.message?.let {
                Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    Text(it, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.error)
                }
            }
        }

        if (files.isEmpty() && !vm.loading && vm.message == null) {
            EmptyHint("该目录暂无文件")
        }

        files.forEachIndexed { index, f ->
            if (f.isdir) {
                DriveRow(
                    icon = MiuixIcons.FolderFill,
                    tint = MiuixTheme.colorScheme.primary,
                    name = f.fname,
                    meta = "文件夹",
                    trailing = "进入",
                    onClick = { vm.enterFolder(f) },
                )
            } else {
                DriveRow(
                    icon = Icons.Filled.Description,
                    tint = MiuixTheme.colorScheme.primary,
                    name = f.fname,
                    meta = "${formatSize(f.fsize)}",
                    trailing = "下载",
                    onClick = { vm.download(f) },
                )
            }
            if (index != files.lastIndex) HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
        }
    }
}

@Composable
private fun DriveRow(
    icon: ImageVector,
    tint: Color,
    name: String,
    meta: String,
    trailing: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        TextButton(text = trailing, onClick = onClick)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}
