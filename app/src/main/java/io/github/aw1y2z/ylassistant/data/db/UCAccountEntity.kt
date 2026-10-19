package io.github.aw1y2z.ylassistant.data.db

/** UC 网盘登录凭证（cookie 落库，后续所有 API 请求携带）。 */
data class UCAccountEntity(
    val id: String = "uc",
    val cookie: String = "",
    val nickname: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
