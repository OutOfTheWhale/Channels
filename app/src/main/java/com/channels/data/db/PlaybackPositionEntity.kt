package com.channels.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Last known playback position for a video, so listening resumes where it left off. */
@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val videoUrl: String,
    val positionMs: Long,
    val updatedAt: Long,
)
