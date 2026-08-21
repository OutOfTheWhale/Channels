package com.channels.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.channels.data.PlaybackPositionRepository
import com.channels.data.download.DownloadRepository
import com.channels.data.youtube.YoutubeRepository
import com.channels.domain.model.AudioTrack
import com.channels.domain.model.VideoItem
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val track: AudioTrack? = null,
    val loadingTitle: String? = null,   // set while a tapped video is being resolved
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val error: String? = null,
) {
    val hasContent: Boolean get() = track != null || loadingTitle != null
}

/**
 * App-scoped bridge between the UI and the [PlaybackService]. Connects a
 * [MediaController], resolves audio on demand via [YoutubeRepository], and exposes
 * a single [state] flow the player UI observes.
 */
class PlayerController(
    context: Context,
    private val repo: YoutubeRepository,
    private val downloads: DownloadRepository,
    private val positions: PlaybackPositionRepository,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var lastPersistAt = 0L

    // The current play queue, so playback auto-advances to the next item when one ends.
    private var queue: List<VideoItem> = emptyList()
    private var queueIndex: Int = 0

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(playerListener) }
            syncFromController()
        }, ContextCompat.getMainExecutor(appContext))
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            managePositionUpdates()
            if (!isPlaying) persistPosition(force = true) // save the moment we pause
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = knownDuration() ?: it.durationMs,
                )
            }
            if (playbackState == Player.STATE_ENDED) {
                clearPosition(_state.value.track?.videoUrl) // finished — don't resume it
                advanceToNext()
            }
        }

        override fun onPlaybackParametersChanged(params: PlaybackParameters) {
            _state.update { it.copy(speed = params.speed) }
        }
    }

    /** Play a single video (a one-item queue). */
    fun play(video: VideoItem) = playQueue(listOf(video), 0)

    /**
     * Play a list of videos starting at [startIndex]. When one finishes, the next in
     * the list starts automatically.
     */
    fun playQueue(videos: List<VideoItem>, startIndex: Int) {
        if (videos.isEmpty()) return
        queue = videos
        queueIndex = startIndex.coerceIn(0, videos.lastIndex)
        loadCurrent()
    }

    private fun advanceToNext() {
        if (queueIndex < queue.lastIndex) {
            queueIndex++
            loadCurrent()
        }
    }

    /** Manually jump to the next track in the queue. */
    fun skipToNext() {
        if (queueIndex < queue.lastIndex) {
            persistPosition(force = true) // save where we left the current one
            queueIndex++
            loadCurrent()
        }
    }

    /** Restart the current track if we're past the start, otherwise go to the previous one. */
    fun skipToPrevious() {
        val pos = controller?.currentPosition ?: 0
        if (pos > 3000 || queueIndex == 0) {
            controller?.seekTo(0)
        } else {
            persistPosition(force = true)
            queueIndex--
            loadCurrent()
        }
    }

    private fun loadCurrent() {
        val video = queue.getOrNull(queueIndex) ?: return
        _state.update {
            it.copy(
                loadingTitle = video.title,
                error = null,
                hasNext = queueIndex < queue.lastIndex,
                hasPrevious = queueIndex > 0,
            )
        }
        scope.launch {
            try {
                // Prefer an offline download when we have one; otherwise resolve the stream.
                val local = downloads.completedOrNull(video.url)
                val track = if (local?.filePath != null) {
                    AudioTrack(
                        videoUrl = video.url,
                        title = video.title,
                        uploader = video.uploader,
                        uploaderUrl = video.uploaderUrl,
                        durationSeconds = video.durationSeconds,
                        thumbnailUrl = video.thumbnailUrl,
                        streamUrl = File(local.filePath).toURI().toString(),
                        mimeType = null,
                        averageBitrate = 0,
                    )
                } else {
                    repo.resolveAudio(video.url) // suspends; does IO internally
                }
                val c = awaitController() ?: return@launch
                // Resume where we left off, unless we're within 10s of the end (then start over).
                val durMs = if (track.durationSeconds > 0) track.durationSeconds * 1000 else 0L
                val saved = positions.getPosition(video.url)
                val startAt = if (saved > 3000 && (durMs == 0L || saved < durMs - 10_000)) saved else 0L
                c.setMediaItem(buildMediaItem(track), startAt)
                c.prepare()
                c.play()
                lastPersistAt = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        track = track,
                        loadingTitle = null,
                        positionMs = startAt,
                        durationMs = durMs,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loadingTitle = null, error = e.message ?: "Couldn't play audio") }
            }
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs.coerceAtLeast(0)) ?: Unit

    fun skip(deltaMs: Long) {
        val c = controller ?: return
        val max = if (c.duration > 0) c.duration else Long.MAX_VALUE
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, max))
    }

    fun cycleSpeed() {
        val c = controller ?: return
        c.setPlaybackSpeed(nextSpeed(c.playbackParameters.speed))
    }

    // --- internals ---

    private fun buildMediaItem(track: AudioTrack): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.uploader)
            .apply { track.thumbnailUrl?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        return MediaItem.Builder()
            .setUri(track.streamUrl)
            // For live streams the mimeType is an HLS/DASH manifest type, which tells
            // ExoPlayer to use the right (live) source factory instead of progressive.
            .apply {
                if (track.mimeType == YoutubeRepository.MIME_HLS ||
                    track.mimeType == YoutubeRepository.MIME_DASH
                ) {
                    setMimeType(track.mimeType)
                }
            }
            .setMediaMetadata(metadata)
            .build()
    }

    private suspend fun awaitController(): MediaController? {
        var tries = 0
        while (controller == null && tries < 60) {
            delay(50)
            tries++
        }
        return controller
    }

    private fun knownDuration(): Long? {
        val d = controller?.duration ?: return null
        return if (d > 0) d else null
    }

    private fun syncFromController() {
        val c = controller ?: return
        _state.update { it.copy(isPlaying = c.isPlaying, speed = c.playbackParameters.speed) }
        managePositionUpdates()
    }

    private fun managePositionUpdates() {
        positionJob?.cancel()
        if (controller?.isPlaying == true) {
            positionJob = scope.launch {
                while (isActive) {
                    syncPosition()
                    delay(500)
                }
            }
        } else {
            syncPosition()
        }
    }

    private fun syncPosition() {
        val c = controller ?: return
        _state.update {
            it.copy(
                positionMs = c.currentPosition.coerceAtLeast(0),
                durationMs = knownDuration() ?: it.durationMs,
            )
        }
        persistPosition() // throttled save so we can resume after the app closes
    }

    /** Save the current position for the current track (throttled to ~5s unless [force]). */
    private fun persistPosition(force: Boolean = false) {
        val c = controller ?: return
        val url = _state.value.track?.videoUrl ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastPersistAt < 5_000) return
        lastPersistAt = now
        val pos = c.currentPosition
        val dur = c.duration
        scope.launch {
            // Don't remember the very start, or a spot within 10s of the end.
            if (pos > 3_000 && (dur <= 0 || pos < dur - 10_000)) {
                positions.save(url, pos)
            } else {
                positions.clear(url)
            }
        }
    }

    private fun clearPosition(url: String?) {
        url ?: return
        scope.launch { positions.clear(url) }
    }

    companion object {
        val SPEEDS = floatArrayOf(1f, 1.25f, 1.5f, 1.75f, 2f)

        fun nextSpeed(current: Float): Float {
            val idx = SPEEDS.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
            return SPEEDS[(idx + 1).mod(SPEEDS.size)]
        }
    }
}
