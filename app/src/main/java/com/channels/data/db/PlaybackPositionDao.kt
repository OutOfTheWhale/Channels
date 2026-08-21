package com.channels.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaybackPositionDao {

    @Query("SELECT positionMs FROM playback_positions WHERE videoUrl = :url")
    suspend fun getPosition(url: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaybackPositionEntity)

    @Query("DELETE FROM playback_positions WHERE videoUrl = :url")
    suspend fun delete(url: String)
}
