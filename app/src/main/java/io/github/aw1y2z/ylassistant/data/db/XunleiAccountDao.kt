package io.github.aw1y2z.ylassistant.data.db

import kotlinx.coroutines.flow.Flow

/** 迅雷账号存储接口（由 InMemoryXunleiAccountStore 实现）。 */
interface XunleiAccountDao {

    fun observeAccount(): Flow<XunleiAccountEntity?>

    suspend fun upsert(account: XunleiAccountEntity)

    suspend fun getAccount(): XunleiAccountEntity?

    suspend fun clear()
}
