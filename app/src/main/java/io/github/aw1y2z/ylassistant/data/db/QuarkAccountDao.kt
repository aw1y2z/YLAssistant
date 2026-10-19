package io.github.aw1y2z.ylassistant.data.db

import kotlinx.coroutines.flow.Flow

/**
 * 夸克账号存储接口。原工程用 Room @Dao 实现，本工程工具链加不了 Room，
 * 由 InMemoryQuarkAccountStore（SharedPreferences）实现同一接口。
 */
interface QuarkAccountDao {

    fun observeAccount(): Flow<QuarkAccountEntity?>

    suspend fun upsert(account: QuarkAccountEntity)

    suspend fun getAccount(): QuarkAccountEntity?

    suspend fun clear()
}
