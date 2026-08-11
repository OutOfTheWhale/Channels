package com.channels.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_items")
data class FeedItemEntity(
    @PrimaryKey val videoUrl: String,
    val channelUrl: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val isLive: Boolean,
    val publishedAt: Long?,
    val fetchedAt: Long,
)
