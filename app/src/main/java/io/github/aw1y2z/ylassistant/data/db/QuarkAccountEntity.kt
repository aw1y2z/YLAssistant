package io.github.aw1y2z.ylassistant.data.db

/** 夸克网盘登录凭证（cookie 持久化，后续所有 API 请求携带）。 */
data class QuarkAccountEntity(
    val id: String = "quark",
    val cookie: String = "",
    val nickname: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
