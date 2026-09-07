package com.channels.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedItemDao {

    // Newest by publish date first (unknown dates last). Live streams sort by their
    // date like anything else — a currently-live stream is recent, but an ended one from
    // weeks ago shouldn't be pinned to the top.
    @Query("SELECT * FROM feed_items ORDER BY publishedAt IS NULL, publishedAt DESC, fetchedAt DESC")
    fun observeFeed(): Flow<List<FeedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FeedItemEntity>)

    /** Remove any feed rows whose channel is no longer starred. */
    @Query("DELETE FROM feed_items WHERE channelUrl NOT IN (:starredUrls)")
    suspend fun pruneUnstarred(starredUrls: List<String>)

    @Query("DELETE FROM feed_items")
    suspend fun clear()
}
