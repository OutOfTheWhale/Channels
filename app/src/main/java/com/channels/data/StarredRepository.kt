package com.channels.data

import com.channels.data.db.StarredChannelDao
import com.channels.data.db.StarredChannelEntity
import com.channels.domain.model.ChannelItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Read/write access to the user's starred channels. */
class StarredRepository(private val dao: StarredChannelDao) {

    fun observeStarred(): Flow<List<ChannelItem>> =
        dao.observeAll().map { list -> list.map { it.toChannelItem() } }

    fun observeIsStarred(url: String): Flow<Boolean> = dao.observeIsStarred(url)

    suspend fun starredUrls(): List<String> = dao.getAllUrls()

    suspend fun star(channel: ChannelItem) = dao.insert(
        StarredChannelEntity(
            url = channel.url,
            name = channel.name,
            thumbnailUrl = channel.thumbnailUrl,
            subscriberCount = channel.subscriberCount,
            starredAt = System.currentTimeMillis(),
        ),
    )

    suspend fun unstar(url: String) = dao.delete(url)

    private fun StarredChannelEntity.toChannelItem() = ChannelItem(
        url = url,
        name = name,
        thumbnailUrl = thumbnailUrl,
        subscriberCount = subscriberCount,
        description = null,
    )
}
