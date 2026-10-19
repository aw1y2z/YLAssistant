package io.github.aw1y2z.ylassistant.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.ui.components.CardColumn
import io.github.aw1y2z.ylassistant.ui.viewmodel.AccountViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 平台登录（下载闭环前置）：
 * - 夸克 / UC：粘贴浏览器 Cookie（含 __pus / __puus）→ 校验 → 落库
 * - 迅雷：粘贴官方网页登录签发的 access/refresh token → 落库
 * 真实扫码/账密登录放到后续轮次；先用凭证打通链路。
 */
@Composable
fun LoginSheet(
    platform: SharePlatform,
    vm: AccountViewModel,
    onDone: () -> Unit,
    onWebLogin: (SharePlatform) -> Unit = {},
) {
    var cookie by remember { mutableStateOf("") }
    var xunleiAt by remember { mutableStateOf("") }
    var xunleiRt by remember { mutableStateOf("") }
    var xunleiNick by remember { mutableStateOf("") }

    val title = when (platform) {
        SharePlatform.QUARK -> "登录夸克网盘"
        SharePlatform.UC -> "登录 UC 网盘"
        SharePlatform.XUNLEI -> "登录迅雷网盘"
    }
    val hint = when (platform) {
        SharePlatform.QUARK -> "粘贴浏览器登录夸克后的 Cookie（含 __pus / __puus 字段）即可。仅保存在本机。"
        SharePlatform.UC -> "粘贴浏览器登录 UC 后的 Cookie（含 __pus / __puus 字段）即可。仅保存在本机。"
        SharePlatform.XUNLEI -> "粘贴官方网页登录页签发的 access_token（可附 refresh_token / 昵称）。仅保存在本机。"
    }
    val loggedIn = when (platform) {
        SharePlatform.QUARK -> vm.quarkAccount.value != null
        SharePlatform.UC -> vm.ucAccount.value != null
        SharePlatform.XUNLEI -> vm.xunleiAccount.value != null
    }

    LaunchedEffect(vm.saved) {
        if (vm.saved) {
            vm.consumeSaved()
            onDone()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onBackground,
        )

        CardColumn {
            Text(
                text = hint,
                modifier = Modifier.padding(14.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        if (platform == SharePlatform.XUNLEI) {
            var mode by remember { mutableStateOf("pwd") }
            LaunchedEffect(Unit) { vm.resetXunleiLogin() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    text = "账号密码",
                    onClick = { mode = "pwd"; vm.resetXunleiLogin() },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "粘贴 Token",
                    onClick = { mode = "token"; vm.resetXunleiLogin() },
                    modifier = Modifier.weight(1f),
                )
            }
            if (mode == "token") {
                TextField(
                    value = xunleiAt,
                    onValueChange = { xunleiAt = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = xunleiRt,
                    onValueChange = { xunleiRt = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = xunleiNick,
                    onValueChange = { xunleiNick = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { vm.saveXunleiToken(xunleiAt, xunleiRt, xunleiNick) },
                        modifier = Modifier.weight(1f),
                        enabled = xunleiAt.isNotBlank() && !vm.saving,
                    ) { Text("登录") }
                    if (loggedIn) TextButton(text = "登出", onClick = { vm.logout(platform); onDone() }, modifier = Modifier.weight(0.6f))
                }
            } else {
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var mobile by remember { mutableStateOf(vm.xunleiMobile) }
                var code by remember { mutableStateOf("") }
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = { vm.xunleiLogin(username, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !vm.saving,
                ) { Text("登录") }
                vm.xunleiStep?.let { step ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "需短信验证：输入手机号与验证码",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    TextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = code,
                        onValueChange = { code = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { vm.xunleiSendSms(mobile) },
                            modifier = Modifier.weight(1f),
                            enabled = !vm.saving,
                        ) { Text(if (vm.xunleiSmsSent) "重新发送" else "发送验证码") }
                        Button(
                            onClick = { vm.xunleiLoginWithSms(mobile, code) },
                            modifier = Modifier.weight(1f),
                            enabled = !vm.saving,
                        ) { Text("验证并登录") }
                    }
                }
                if (loggedIn) TextButton(text = "登出", onClick = { vm.logout(platform); onDone() }, modifier = Modifier.fillMaxWidth())
            }
            vm.xunleiError?.let {
                Text(
                    text = it,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                )
            }
        } else {
            TextField(
                value = cookie,
                onValueChange = { cookie = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        if (platform == SharePlatform.QUARK) vm.saveQuarkCookie(cookie) else vm.saveUcCookie(cookie)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = cookie.isNotBlank() && !vm.saving,
                ) { Text("登录") }
                if (loggedIn) TextButton(text = "登出", onClick = { vm.logout(platform); onDone() }, modifier = Modifier.weight(0.6f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    text = "网页登录",
                    onClick = { onWebLogin(platform) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (vm.saving) {
            Text(
                text = "校验中…",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        vm.message?.let {
            Text(
                text = it,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.error,
            )
        }
    }
}
