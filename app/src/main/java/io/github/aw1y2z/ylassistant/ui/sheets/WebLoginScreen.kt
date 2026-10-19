package io.github.aw1y2z.ylassistant.ui.sheets

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.aw1y2z.ylassistant.data.network.QuarkConstants
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.data.network.UCConstants
import io.github.aw1y2z.ylassistant.ui.viewmodel.AccountViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 夸克 / UC 网页登录页（移植自参考工程 QuarkLoginScreen/UCLoginScreen）：
 * - WebView 加载官方登录页，由用户手动登录；
 * - 点「保存」用 CookieManager 提取该域 Cookie，交 AccountViewModel 校验落库。
 * 仅 QUARK / UC 适用（迅雷走账密+短信，无网页登录）。
 */
@Composable
fun WebLoginScreen(
    platform: SharePlatform,
    vm: AccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val loginUrl: String
    val cookieDomain: String
    val ua: String
    val title: String
    val hint: String
    when (platform) {
        SharePlatform.QUARK -> {
            loginUrl = QuarkConstants.LOGIN_URL
            cookieDomain = QuarkConstants.COOKIE_DOMAIN
            ua = QuarkConstants.USER_AGENT
            title = "夸克网盘登录"
            hint = "扫码登录成功后，点右上角「保存」自动提取 Cookie"
        }
        SharePlatform.UC -> {
            loginUrl = UCConstants.LOGIN_URL
            cookieDomain = UCConstants.COOKIE_DOMAIN
            ua = UCConstants.USER_AGENT
            title = "UC 网盘登录"
            hint = "登录成功后，点右上角「保存」自动提取 Cookie"
        }
        else -> return
    }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var triggered by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val webView = remember {
        CookieManager.getInstance().setAcceptCookie(true)
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.userAgentString = ua
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                }
            }
            webChromeClient = WebChromeClient()
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            loadUrl(loginUrl)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            webView.destroy()
        }
    }

    BackHandler(enabled = !isSaving) { onBack() }

    // 保存完成后由 vm.saved 通知；这里监听 saving 由 true→false 的结束时刻
    LaunchedEffect(vm.saving) {
        if (triggered && !vm.saving) {
            triggered = false
            isSaving = false
            if (vm.saved) {
                vm.consumeSaved()
                onSaved()
            } else {
                errorMsg = vm.message ?: "未检测到登录态，请先在网页中完成登录"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 96.dp),
        )

        // 顶部栏（返回 + 标题 + 保存），覆盖在 WebView 之上
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(MiuixTheme.colorScheme.surface),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .clickable(onClick = { if (!isSaving) onBack() })
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    if (isSaving) {
                        Text(
                            text = "保存中…",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(8.dp),
                        )
                    } else {
                        TextButton(text = "保存", onClick = {
                            val cookie = CookieManager.getInstance().getCookie(cookieDomain).orEmpty()
                            if (cookie.isBlank() || !cookie.contains("__pus")) {
                                errorMsg = "未检测到登录态，请先在网页中完成登录"
                                return@TextButton
                            }
                            errorMsg = null
                            isSaving = true
                            triggered = true
                            if (platform == SharePlatform.QUARK) {
                                vm.saveQuarkCookie(cookie)
                            } else {
                                vm.saveUcCookie(cookie)
                            }
                        })
                    }
                }
                Text(
                    text = hint,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        if (isLoading) {
            Text(
                text = "加载中…",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 96.dp),
            )
        }
        errorMsg?.let {
            Text(
                text = it,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}
