package com.channels

import android.app.Application
import com.channels.data.work.RefreshFeedWorker
import com.channels.di.AppContainer

/**
 * Application entry point. Holds the manual dependency container so we avoid a
 * heavyweight DI framework and keep the dependency footprint "light".
 */
class ChannelsApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RefreshFeedWorker.schedule(this)
    }
}
