package com.channels.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoUrl: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val filePath: String?,      // set once COMPLETED
    val state: String,          // QUEUED | RUNNING | COMPLETED | FAILED
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val createdAt: Long,
)
