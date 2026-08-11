package com.channels.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.channels.ui.theme.Mist

/** Circular channel avatar. Falls back to a neutral block when there's no image. */
@Composable
fun ChannelAvatar(url: String?, size: Int = 44) {
    val shape = CircleShape
    if (url.isNullOrBlank()) {
        Box(Modifier.size(size.dp).clip(shape).background(Mist))
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(shape).background(Mist),
        )
    }
}

/** 16:9-ish video thumbnail. Falls back to a neutral block when there's no image. */
@Composable
fun VideoThumb(url: String?, width: Int = 72, height: Int = 44) {
    val shape = RoundedCornerShape(6.dp)
    if (url.isNullOrBlank()) {
        Box(Modifier.size(width.dp, height.dp).clip(shape).background(Mist))
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width.dp, height.dp).clip(shape).background(Mist),
        )
    }
}
