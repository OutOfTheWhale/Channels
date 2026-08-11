package com.channels.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StarredChannelDao {

    @Query("SELECT * FROM starred_channels ORDER BY starredAt DESC")
    fun observeAll(): Flow<List<StarredChannelEntity>>

    @Query("SELECT url FROM starred_channels")
    suspend fun getAllUrls(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM starred_channels WHERE url = :url)")
    fun observeIsStarred(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: StarredChannelEntity)

    @Query("DELETE FROM starred_channels WHERE url = :url")
    suspend fun delete(url: String)
}
