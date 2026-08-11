package com.channels.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StarredChannelEntity::class,
        FeedItemEntity::class,
        DownloadEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class ChannelsDatabase : RoomDatabase() {
    abstract fun starredChannelDao(): StarredChannelDao
    abstract fun feedItemDao(): FeedItemDao
    abstract fun downloadDao(): DownloadDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        fun build(context: Context): ChannelsDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ChannelsDatabase::class.java,
                "channels.db",
            )
                // Real migrations so starred channels, playlists, and downloads survive
                // app updates. Destructive fallback stays only as a last-resort safety net.
                .addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigration()
                .build()
    }
}
