package com.channels.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "playlist_items",
    indices = [Index(value = ["playlistId", "videoUrl"], unique = true), Index("playlistId")],
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val videoUrl: String,
    val title: String,
    val uploader: String,
    val uploaderUrl: String?,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val addedAt: Long,
)

/** Playlist plus how many items it holds, for the list view. */
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val itemCount: Int,
)
