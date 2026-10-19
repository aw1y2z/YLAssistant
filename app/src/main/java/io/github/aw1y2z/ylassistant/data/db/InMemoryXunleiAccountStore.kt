package io.github.aw1y2z.ylassistant.data.db

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 迅雷账号存储（SharedPreferences 单行），替代 Room 的 XunleiAccountDao。 */
class InMemoryXunleiAccountStore(context: Context) : XunleiAccountDao {

    private val prefs =
        context.applicationContext.getSharedPreferences("xunlei_account", Context.MODE_PRIVATE)
    private val _obs = MutableStateFlow<XunleiAccountEntity?>(read())

    override fun observeAccount(): Flow<XunleiAccountEntity?> = _obs.asStateFlow()

    override suspend fun getAccount(): XunleiAccountEntity? = read()

    override suspend fun upsert(account: XunleiAccountEntity) {
        prefs.edit()
            .putString("accessToken", account.accessToken)
            .putString("refreshToken", account.refreshToken)
            .putString("deviceId", account.deviceId)
            .putString("captchaToken", account.captchaToken)
            .putString("nickname", account.nickname)
            .putLong("updatedAt", account.updatedAt)
            .apply()
        _obs.value = account
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
        _obs.value = null
    }

    private fun read(): XunleiAccountEntity? {
        val token = prefs.getString("accessToken", "") ?: ""
        if (token.isEmpty()) return null
        return XunleiAccountEntity(
            id = "xunlei",
            accessToken = token,
            refreshToken = prefs.getString("refreshToken", "") ?: "",
            deviceId = prefs.getString("deviceId", "") ?: "",
            captchaToken = prefs.getString("captchaToken", "") ?: "",
            nickname = prefs.getString("nickname", "") ?: "",
            updatedAt = prefs.getLong("updatedAt", 0L)
        )
    }
}
