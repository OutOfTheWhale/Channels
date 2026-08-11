package com.channels.di

import android.content.Context
import com.channels.data.FeedRepository
import com.channels.data.StarredRepository
import com.channels.data.db.ChannelsDatabase
import com.channels.data.download.DownloadRepository
import com.channels.data.youtube.YoutubeRepository
import com.channels.playback.PlayerController

/**
 * Manual dependency container. Kept intentionally small; repositories, the player
 * controller, and the database are added here in later phases so the whole app
 * wires up through one place instead of a DI framework.
 */
class AppContainer(private val appContext: Context) {

    val youtubeRepository: YoutubeRepository by lazy { YoutubeRepository() }

    private val database: ChannelsDatabase by lazy { ChannelsDatabase.build(appContext) }

    val starredRepository: StarredRepository by lazy {
        StarredRepository(database.starredChannelDao())
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(appContext, database.downloadDao())
    }

    val playerController: PlayerController by lazy {
        PlayerController(appContext, youtubeRepository, downloadRepository)
    }

    val feedRepository: FeedRepository by lazy {
        FeedRepository(youtubeRepository, starredRepository, database.feedItemDao())
    }
}
