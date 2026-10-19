package io.github.aw1y2z.ylassistant.data.db

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UC 账号存储（SharedPreferences 单行），替代 Room 的 UCAccountDao。 */
class InMemoryUcAccountStore(context: Context) : UCAccountDao {

    private val prefs =
        context.applicationContext.getSharedPreferences("uc_account", Context.MODE_PRIVATE)
    private val _obs = MutableStateFlow<UCAccountEntity?>(read())

    override fun observeAccount(): Flow<UCAccountEntity?> = _obs.asStateFlow()

    override suspend fun getAccount(): UCAccountEntity? = read()

    override suspend fun upsert(account: UCAccountEntity) {
        prefs.edit()
            .putString("cookie", account.cookie)
            .putString("nickname", account.nickname)
            .putLong("updatedAt", account.updatedAt)
            .apply()
        _obs.value = account
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
        _obs.value = null
    }

    private fun read(): UCAccountEntity? {
        val cookie = prefs.getString("cookie", "") ?: ""
        if (cookie.isEmpty()) return null
        return UCAccountEntity(
            id = "uc",
            cookie = cookie,
            nickname = prefs.getString("nickname", "") ?: "",
            updatedAt = prefs.getLong("updatedAt", 0L)
        )
    }
}
