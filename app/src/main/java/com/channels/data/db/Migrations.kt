package com.channels.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations. Each one only *adds* tables/indexes, so existing rows (starred
 * channels, playlists, downloads) survive app updates. The SQL matches exactly what
 * Room generates for each entity — keep it in sync when the schema changes, and add a
 * new migration for every version bump instead of relying on destructive fallback.
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `feed_items` (`videoUrl` TEXT NOT NULL, " +
                "`channelUrl` TEXT NOT NULL, `title` TEXT NOT NULL, `uploader` TEXT NOT NULL, " +
                "`durationSeconds` INTEGER NOT NULL, `thumbnailUrl` TEXT, `isLive` INTEGER NOT NULL, " +
                "`publishedAt` INTEGER, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`videoUrl`))",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `downloads` (`videoUrl` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `uploader` TEXT NOT NULL, `durationSeconds` INTEGER NOT NULL, " +
                "`thumbnailUrl` TEXT, `filePath` TEXT, `state` TEXT NOT NULL, " +
                "`bytesDownloaded` INTEGER NOT NULL, `totalBytes` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`videoUrl`))",
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`playlistId` INTEGER NOT NULL, `videoUrl` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`uploader` TEXT NOT NULL, `uploaderUrl` TEXT, `durationSeconds` INTEGER NOT NULL, " +
                "`thumbnailUrl` TEXT, `addedAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_playlist_items_playlistId_videoUrl` " +
                "ON `playlist_items` (`playlistId`, `videoUrl`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_items_playlistId` " +
                "ON `playlist_items` (`playlistId`)",
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
