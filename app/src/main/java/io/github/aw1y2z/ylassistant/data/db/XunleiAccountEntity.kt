package io.github.aw1y2z.ylassistant.data.db

/**
 * 迅雷网盘登录凭证（access_token 落库，pan API 请求携带 Bearer）。
 * 持久化由 InMemoryXunleiAccountStore（SharedPreferences）承载。
 */
data class XunleiAccountEntity(
    val id: String = "xunlei",
    val accessToken: String = "",
    val refreshToken: String = "",
    val deviceId: String = "",
    val captchaToken: String = "",
    val nickname: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
