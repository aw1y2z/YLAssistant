package io.github.aw1y2z.ylassistant.data.db

import kotlinx.coroutines.flow.Flow

/**
 * UC 账号存储接口。原工程用 Room @Dao 实现，本工程工具链加不了 Room，
 * 由 InMemoryUcAccountStore（SharedPreferences）实现同一接口。
 */
interface UCAccountDao {

    fun observeAccount(): Flow<UCAccountEntity?>

    suspend fun upsert(account: UCAccountEntity)

    suspend fun getAccount(): UCAccountEntity?

    suspend fun clear()
}
