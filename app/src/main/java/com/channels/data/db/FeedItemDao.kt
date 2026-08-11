package com.channels.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedItemDao {

    // Newest first; items with an unknown date sort last (by fetch order).
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
