package com.channels.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starred_channels")
data class StarredChannelEntity(
    @PrimaryKey val url: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscriberCount: Long,
    val starredAt: Long,
)
