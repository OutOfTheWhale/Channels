package com.channels.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query(
        "SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt, " +
            "COUNT(i.id) AS itemCount FROM playlists p " +
            "LEFT JOIN playlist_items i ON i.playlistId = p.id " +
            "GROUP BY p.id ORDER BY p.createdAt DESC",
    )
    fun observePlaylistsWithCounts(): Flow<List<PlaylistWithCount>>

    @Query("SELECT name FROM playlists WHERE id = :id")
    fun observeName(id: Long): Flow<String?>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :id")
    suspend fun clearItems(id: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoUrl = :videoUrl")
    suspend fun removeItem(playlistId: Long, videoUrl: String)
}
