package com.channels.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StarredChannelEntity::class, FeedItemEntity::class, DownloadEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ChannelsDatabase : RoomDatabase() {
    abstract fun starredChannelDao(): StarredChannelDao
    abstract fun feedItemDao(): FeedItemDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        fun build(context: Context): ChannelsDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ChannelsDatabase::class.java,
                "channels.db",
            )
                // Pre-release: the cached feed is disposable, so just rebuild on schema change.
                .fallbackToDestructiveMigration()
                .build()
    }
}
