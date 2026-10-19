package io.github.aw1y2z.ylassistant.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aw1y2z.ylassistant.data.db.QuarkAccountEntity
import io.github.aw1y2z.ylassistant.data.db.UCAccountEntity
import io.github.aw1y2z.ylassistant.data.db.XunleiAccountEntity
import io.github.aw1y2z.ylassistant.data.network.SharePlatform
import io.github.aw1y2z.ylassistant.data.network.XunleiApi
import io.github.aw1y2z.ylassistant.data.network.XunleiLoginStep
import io.github.aw1y2z.ylassistant.data.repository.QuarkAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.UCAccountRepository
import io.github.aw1y2z.ylassistant.data.repository.XunleiAccountRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 三平台账号：登录态（cookie / token）+ 登录/登出。
 * 夸克/UC 走 cookie 校验，迅雷走网页会话 token（access_token）。
 * 下载闭环依赖登录态，故作为闭环前置能力提供。
 */
class AccountViewModel(
    private val quarkRepo: QuarkAccountRepository,
    private val ucRepo: UCAccountRepository,
    private val xunleiRepo: XunleiAccountRepository,
) : ViewModel() {

    val quarkAccount: StateFlow<QuarkAccountEntity?> =
        quarkRepo.observeAccount().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val ucAccount: StateFlow<UCAccountEntity?> =
        ucRepo.observeAccount().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val xunleiAccount: StateFlow<XunleiAccountEntity?> =
        xunleiRepo.observeAccount().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var saving by mutableStateOf(false)
        private set

    var saved by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    fun saveQuarkCookie(cookie: String) = saveCookie(cookie) { quarkRepo.saveQuarkAccount(it) }

    fun saveUcCookie(cookie: String) = saveCookie(cookie) { ucRepo.saveUCAccount(it) }

    private fun saveCookie(cookie: String, save: suspend (String) -> Boolean) {
        val trimmed = cookie.trim()
        if (trimmed.isBlank()) {
            message = "cookie 为空"
            return
        }
        saving = true
        message = null
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            val ok = runCatching { save(trimmed) }.getOrDefault(false)
            saving = false
            message = if (ok) null else "cookie 无效或未通过校验"
            saved = ok
        }
    }

    /** 迅雷：粘贴官方网页登录签发的 access/refresh token */
    fun saveXunleiToken(accessToken: String, refreshToken: String, nickname: String) {
        val at = accessToken.trim()
        if (at.isBlank()) {
            message = "access_token 为空"
            return
        }
        saving = true
        message = null
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            val ok = runCatching {
                xunleiRepo.saveWebSession(at, refreshToken.trim(), nickname.trim())
            }.getOrDefault(false)
            saving = false
            message = if (ok) null else "token 无效或已过期"
            saved = ok
        }
    }

    // ===== 迅雷：账号密码 + 短信验证登录 =====
    var xunleiStep by mutableStateOf<XunleiLoginStep?>(null)
        private set
    var xunleiError by mutableStateOf<String?>(null)
        private set
    var xunleiSmsSent by mutableStateOf(false)
        private set
    var xunleiMobile by mutableStateOf("")
        private set

    /** 进入迅雷登录页时重置中间状态（VM 跨弹层存活，避免残留上次的短信步骤） */
    fun resetXunleiLogin() {
        xunleiStep = null
        xunleiSmsSent = false
        xunleiError = null
    }

    /** 账号密码登录；needSms 时切短信步骤，session 齐备时直接换 token */
    fun xunleiLogin(username: String, password: String) {
        val u = username.trim()
        val p = password
        if (u.isBlank() || p.isBlank()) { xunleiError = "请输入账号和密码"; return }
        saving = true
        xunleiError = null
        xunleiStep = null
        xunleiSmsSent = false
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            try {
                val step = xunleiRepo.loginWithPassword(u, p)
                if (step.needSms) {
                    val reviewMap = XunleiApi.parseReviewUrl(step.reviewUrl)
                    val creditKey = reviewMap["creditkey"].orEmpty()
                    if (creditKey.isNotBlank()) {
                        xunleiStep = step.copy(
                            smsCreditKey = creditKey,
                            smsToken = reviewMap["token"].orEmpty()
                        )
                    } else {
                        val smsStep = xunleiRepo.sendSms(u)
                        if (smsStep.smsCreditKey.isNotBlank()) {
                            xunleiSmsSent = true
                            xunleiMobile = u
                            xunleiStep = smsStep
                        } else {
                            xunleiError = smsStep.message.ifBlank { "短信发送失败，请重试" }
                            xunleiStep = step.copy(message = "短信发送失败")
                        }
                    }
                } else if (step.sessionKey.isNotBlank() && step.sessionId.isNotBlank()) {
                    val ok = xunleiRepo.finishLogin(step, u)
                    if (ok) saved = true else xunleiError = "登录失败，无法换取凭证"
                } else {
                    xunleiError = step.message.ifBlank { "登录失败，请检查账号密码" }
                }
            } catch (e: Exception) {
                xunleiError = "登录失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                saving = false
            }
        }
    }

    /** 发送/重发短信验证码（密码登录触发验证后，或短信步骤手动发送） */
    fun xunleiSendSms(mobile: String) {
        val m = mobile.trim()
        if (m.isBlank()) { xunleiError = "请输入手机号"; return }
        xunleiMobile = m
        saving = true
        xunleiError = null
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            try {
                val step = xunleiRepo.sendSms(xunleiMobile)
                if (step.smsCreditKey.isNotBlank()) {
                    xunleiSmsSent = true
                    xunleiStep = step
                } else {
                    xunleiError = step.message.ifBlank { "短信发送失败" }
                }
            } catch (e: Exception) {
                xunleiError = "发送失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                saving = false
            }
        }
    }

    /** 短信验证码登录并完成换 token，成功落库并关闭弹层 */
    fun xunleiLoginWithSms(mobile: String, code: String) {
        val step = xunleiStep
        if (step == null) { xunleiError = "登录状态已失效，请重试"; return }
        val m = mobile.trim()
        if (m.isBlank()) { xunleiError = "请输入手机号"; return }
        if (code.trim().isBlank()) { xunleiError = "请输入验证码"; return }
        saving = true
        xunleiError = null
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            try {
                val ok = xunleiRepo.loginWithSms(m, code.trim(), step.smsCreditKey, step.smsToken)
                if (ok) saved = true else xunleiError = "验证码校验失败"
            } catch (e: Exception) {
                xunleiError = "验证失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                saving = false
            }
        }
    }

    fun logout(platform: SharePlatform) {
        viewModelScope.launch(AppCoroutineExceptionHandler) {
            when (platform) {
                SharePlatform.QUARK -> quarkRepo.logoutQuark()
                SharePlatform.UC -> ucRepo.logoutUC()
                SharePlatform.XUNLEI -> xunleiRepo.logout()
            }
        }
    }

    fun consumeSaved() {
        saved = false
    }

    fun consumeMessage() {
        message = null
    }
}
