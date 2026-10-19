package io.github.aw1y2z.ylassistant.data.db

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 夸克账号存储（SharedPreferences 单行），替代 Room 的 QuarkAccountDao。 */
class InMemoryQuarkAccountStore(context: Context) : QuarkAccountDao {

    private val prefs =
        context.applicationContext.getSharedPreferences("quark_account", Context.MODE_PRIVATE)
    private val _obs = MutableStateFlow<QuarkAccountEntity?>(read())

    override fun observeAccount(): Flow<QuarkAccountEntity?> = _obs.asStateFlow()

    override suspend fun getAccount(): QuarkAccountEntity? = read()

    override suspend fun upsert(account: QuarkAccountEntity) {
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

    private fun read(): QuarkAccountEntity? {
        val cookie = prefs.getString("cookie", "") ?: ""
        if (cookie.isEmpty()) return null
        return QuarkAccountEntity(
            id = "quark",
            cookie = cookie,
            nickname = prefs.getString("nickname", "") ?: "",
            updatedAt = prefs.getLong("updatedAt", 0L)
        )
    }
}
