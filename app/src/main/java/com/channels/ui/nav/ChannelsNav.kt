package com.channels.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.channels.domain.model.VideoItem
import com.channels.ui.channel.ChannelScreen
import com.channels.ui.home.HomeScreen
import com.channels.ui.library.DownloadsScreen
import com.channels.ui.library.LibraryMenuScreen
import com.channels.ui.library.PlaylistDetailScreen
import com.channels.ui.library.PlaylistsScreen
import com.channels.ui.library.StarredChannelsScreen
import com.channels.ui.player.MiniPlayer
import com.channels.ui.player.PlayerScreen
import com.channels.ui.rememberAppContainer
import com.channels.ui.search.SearchScreen
import com.channels.ui.theme.Hairline
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val LIBRARY_STARRED = "library_starred"
    const val LIBRARY_DOWNLOADS = "library_downloads"
    const val LIBRARY_PLAYLISTS = "library_playlists"
    const val PLAYER = "player"
}

/** Top-level tabs. Text-only labels keep the chrome minimal and on-brand. */
enum class Dest(val route: String, val label: String) {
    Home(Routes.HOME, "Home"),
    Search(Routes.SEARCH, "Search"),
    Library(Routes.LIBRARY, "Library"),
}

private fun Dest.owns(route: String?): Boolean = when (this) {
    Dest.Home -> route == Routes.HOME
    Dest.Search -> route == Routes.SEARCH
    Dest.Library -> route != null &&
        (route == Routes.LIBRARY || route.startsWith("library_") || route.startsWith("playlist"))
}

@Composable
fun ChannelsRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val player = rememberAppContainer().playerController
    val playerState by player.state.collectAsStateWithLifecycle()

    val openChannel: (String) -> Unit = { url ->
        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
        navController.navigate("channel/$encoded")
    }
    val onPlay: (List<VideoItem>, Int) -> Unit = { list, index ->
        player.playQueue(list, index)
        navController.navigate(Routes.PLAYER)
    }
    val onPlayerScreen = currentRoute == Routes.PLAYER

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                if (playerState.hasContent && !onPlayerScreen) {
                    MiniPlayer(
                        state = playerState,
                        onOpen = { navController.navigate(Routes.PLAYER) },
                        onPlayPause = player::togglePlayPause,
                    )
                }
                LightBottomBar(
                    selected = { dest -> dest.owns(currentRoute) },
                    onSelect = { dest ->
                        // Tapping a tab pops any detail screens and lands on the tab's root.
                        navController.navigate(dest.route) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) { HomeScreen(onPlay = onPlay) }

            composable(Routes.SEARCH) {
                SearchScreen(onOpenChannel = openChannel, onPlay = onPlay)
            }

            composable(Routes.LIBRARY) {
                LibraryMenuScreen(
                    onOpenStarred = { navController.navigate(Routes.LIBRARY_STARRED) },
                    onOpenDownloads = { navController.navigate(Routes.LIBRARY_DOWNLOADS) },
                    onOpenPlaylists = { navController.navigate(Routes.LIBRARY_PLAYLISTS) },
                )
            }
            composable(Routes.LIBRARY_STARRED) {
                StarredChannelsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChannel = openChannel,
                )
            }
            composable(Routes.LIBRARY_DOWNLOADS) {
                DownloadsScreen(onBack = { navController.popBackStack() }, onPlay = onPlay)
            }
            composable(Routes.LIBRARY_PLAYLISTS) {
                PlaylistsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPlaylist = { id -> navController.navigate("playlist/$id") },
                )
            }
            composable("playlist/{playlistId}") { entry ->
                val id = entry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L
                PlaylistDetailScreen(
                    playlistId = id,
                    onBack = { navController.popBackStack() },
                    onPlay = onPlay,
                )
            }

            composable("channel/{channelUrl}") { entry ->
                val encoded = entry.arguments?.getString("channelUrl").orEmpty()
                val channelUrl = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
                ChannelScreen(
                    channelUrl = channelUrl,
                    onBack = { navController.popBackStack() },
                    onPlay = onPlay,
                )
            }
            composable(Routes.PLAYER) {
                PlayerScreen(
                    controller = player,
                    onBack = { navController.popBackStack() },
                    onOpenChannel = openChannel,
                )
            }
        }
    }
}

@Composable
private fun LightBottomBar(
    selected: (Dest) -> Boolean,
    onSelect: (Dest) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawLine(
                    color = Hairline,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f,
                )
            }
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dest.entries.forEach { dest ->
            val isSelected = selected(dest)
            Text(
                text = dest.label,
                textAlign = TextAlign.Center,
                color = if (isSelected) Ink else Slate,
                style = if (isSelected) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                modifier = Modifier
                    .clickable(onClick = { onSelect(dest) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}
