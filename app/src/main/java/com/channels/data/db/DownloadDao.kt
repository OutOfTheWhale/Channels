package com.channels.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoUrl = :url")
    fun observe(url: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE videoUrl = :url")
    suspend fun get(url: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("UPDATE downloads SET state = :state WHERE videoUrl = :url")
    suspend fun setState(url: String, state: String)

    @Query("UPDATE downloads SET state = :state, bytesDownloaded = :bytes, totalBytes = :total WHERE videoUrl = :url")
    suspend fun setProgress(url: String, state: String, bytes: Long, total: Long)

    @Query("UPDATE downloads SET state = :state, filePath = :path WHERE videoUrl = :url")
    suspend fun setCompleted(url: String, state: String, path: String)

    @Query("DELETE FROM downloads WHERE videoUrl = :url")
    suspend fun delete(url: String)
}
