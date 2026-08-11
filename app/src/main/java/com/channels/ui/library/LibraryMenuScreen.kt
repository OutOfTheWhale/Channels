package com.channels.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.channels.ui.components.ListRow
import com.channels.ui.components.RowDivider
import com.channels.ui.theme.Ink
import com.channels.ui.theme.Slate

@Composable
fun LibraryMenuScreen(
    onOpenStarred: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenPlaylists: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
        )
        MenuRow("Starred Channels", onOpenStarred)
        RowDivider()
        MenuRow("Downloads", onOpenDownloads)
        RowDivider()
        MenuRow("Playlists", onOpenPlaylists)
        RowDivider()
    }
}

@Composable
private fun MenuRow(title: String, onClick: () -> Unit) {
    ListRow(
        title = title,
        subtitle = null,
        onClick = onClick,
        trailing = { Text("›", style = MaterialTheme.typography.headlineMedium, color = Slate) },
    )
}
