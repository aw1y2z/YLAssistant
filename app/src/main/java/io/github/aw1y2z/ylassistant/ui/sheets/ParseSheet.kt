package io.github.aw1y2z.ylassistant.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aw1y2z.ylassistant.data.network.ShareLinkParser
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.data.network.model.ShareFile
import io.github.aw1y2z.ylassistant.ui.components.CardColumn
import io.github.aw1y2z.ylassistant.ui.viewmodel.AccountViewModel
import io.github.aw1y2z.ylassistant.ui.viewmodel.ResolveViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 解析弹层：两步，不跳新页面。
 * Step1 输入链接（识别平台 + 登录态校验 + 未登录引导）；Step2 选择文件。
 */
object ParseSheet {

    @Composable
    fun Step1(
        vm: ResolveViewModel,
        accountVm: AccountViewModel,
        onLogin: (SharePlatform) -> Unit,
        onToFiles: () -> Unit,
    ) {
        var link by remember { mutableStateOf("") }
        var pwd by remember { mutableStateOf("") }
        val quarkAcc by accountVm.quarkAccount.collectAsStateWithLifecycle()
        val ucAcc by accountVm.ucAccount.collectAsStateWithLifecycle()
        val xunleiAcc by accountVm.xunleiAccount.collectAsStateWithLifecycle()

        // 从链接识别平台；未识别则为 null（暂不启用解析）
        val platform = link.trim().let { if (it.isEmpty()) null else ShareLinkParser.parse(it)?.platform }
        val loggedIn = when (platform) {
            SharePlatform.QUARK -> quarkAcc != null
            SharePlatform.UC -> ucAcc != null
            SharePlatform.XUNLEI -> xunleiAcc != null
            null -> false
        }
        val loggedInAny = quarkAcc != null || ucAcc != null || xunleiAcc != null

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "解析分享链接",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onBackground,
            )

            TextField(
                value = link,
                onValueChange = { link = it },
                modifier = Modifier.fillMaxWidth(),
            )

            if (platform != null) {
                CardColumn {
                    Text(
                        text = "已识别：${platformName(platform)}",
                        modifier = Modifier.padding(14.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "提取码（分享链接有密码时填写，可选）",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                TextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!loggedIn) {
                    TextButton(text = "登录${platformName(platform)}", onClick = { onLogin(platform) })
                }
            } else if (!loggedInAny) {
                CardColumn {
                    Text(
                        text = "尚未登录任一网盘，解析下载需先登录",
                        modifier = Modifier.padding(14.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            Button(
                onClick = {
                    vm.startResolve(link, pwd.takeIf { it.isNotBlank() })
                    onToFiles()
                },
                enabled = platform != null && loggedIn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("解析并下载")
            }

            if (vm.uiState is ResolveViewModel.UiState.Error) {
                Text(
                    text = (vm.uiState as ResolveViewModel.UiState.Error).message,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                )
            }
        }
    }

    @Composable
    fun Step2(vm: ResolveViewModel, onDone: () -> Unit) {
        val uiState = vm.uiState
        val files = (uiState as? ResolveViewModel.UiState.Detail)?.files.orEmpty()
        val dirStack = vm.dirStack

        // 勾选状态；列表变化（进/出目录）后自动选中第一个文件（文件夹不可选）
        var picked by remember { mutableStateOf(setOf<Int>()) }
        LaunchedEffect(files) {
            picked = files.indexOfFirst { !it.isdir }.takeIf { it >= 0 }?.let { setOf(it) } ?: emptySet()
        }
        fun toggle(index: Int) {
            picked = if (index in picked) picked - index else picked + index
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { if (dirStack.isNotEmpty()) vm.goUp() else onDone() }) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = dirStack.lastOrNull()?.fname ?: "选择要下载的文件",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                when {
                    uiState is ResolveViewModel.UiState.Loading -> {
                        CardColumn {
                            Text(
                                text = "加载中…",
                                modifier = Modifier.padding(16.dp),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    uiState is ResolveViewModel.UiState.Error -> {
                        CardColumn {
                            Text(
                                text = (uiState as ResolveViewModel.UiState.Error).message,
                                modifier = Modifier.padding(16.dp),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.error,
                            )
                        }
                    }
                    files.isEmpty() -> {
                        CardColumn {
                            Text(
                                text = vm.downloadError?.takeIf { it.isNotBlank() }
                                    ?: "暂无文件，请返回重贴链接",
                                modifier = Modifier.padding(16.dp),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    else -> {
                        CardColumn {
                            files.forEachIndexed { index, file ->
                                if (file.isdir) {
                                    DirRow(file) { vm.openDir(file) }
                                } else {
                                    FileCheckRow(file, checked = index in picked) { toggle(index) }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (vm.fetchLoading) {
                    Text(
                        text = "正在转存并获取直链，大文件请稍候…",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (vm.downloadError?.isNotBlank() == true) {
                    Text(
                        text = vm.downloadError.orEmpty(),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = "已选 ${picked.size} 项",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            val first = picked.minOrNull() ?: 0
                            files.getOrNull(first)?.let { vm.fetchDownloadLink(it) }
                        },
                        enabled = picked.isNotEmpty() && !vm.fetchLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("开始下载")
                    }
                }
            }
        }
    }

    private fun platformName(p: SharePlatform): String = when (p) {
        SharePlatform.QUARK -> "夸克网盘"
        SharePlatform.UC -> "UC 网盘"
        SharePlatform.XUNLEI -> "迅雷网盘"
    }
}

@Composable
private fun DirRow(dir: ShareFile, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "📁 ${dir.fname}",
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Text(
                text = "文件夹 · 点击进入",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FileCheckRow(file: ShareFile, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) MiuixTheme.colorScheme.primary else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (checked) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    shape = RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (file.isdir) "📁 ${file.fname}" else file.fname,
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Text(
                text = if (file.isdir) "文件夹" else "${formatSize(file.fsize)}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
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
