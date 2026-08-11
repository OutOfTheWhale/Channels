package com.channels.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.channels.ChannelsApp
import com.channels.di.AppContainer

/**
 * Builds a ViewModel factory that hands the [AppContainer] to the constructor,
 * so screens can create their ViewModels without a DI framework.
 */
inline fun <reified VM : ViewModel> containerViewModelFactory(
    crossinline create: (AppContainer) -> VM,
) = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as ChannelsApp
        create(app.container)
    }
}

/** Access the app-wide [AppContainer] from a composable. */
@Composable
fun rememberAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as ChannelsApp).container

