package com.channels.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.channels.ChannelsApp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Streams a video's audio to a local file, updating progress in Room as it goes.
 * Runs as background work so downloads survive the app being minimized.
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val container = (applicationContext as ChannelsApp).container
        val downloads = container.downloadRepository

        return try {
            downloads.markRunning(url)

            // Resolve a fresh stream URL now (they expire), then stream it to disk.
            val track = container.youtubeRepository.resolveAudio(url)
            val ext = extensionFor(track.mimeType)
            val outFile = File(downloads.downloadsDir(), "${safeName(url)}.$ext")

            // YouTube throttles a single progressive request to ~playback speed, so we
            // pull the file in chunks using HTTP Range requests, which download at full
            // speed. We loop until a chunk comes back short (the end of the file).
            var start = 0L
            var total = -1L
            var lastReported = 0L
            outFile.outputStream().use { output ->
                while (true) {
                    val end = start + CHUNK_SIZE - 1
                    val request = Request.Builder()
                        .url(track.streamUrl)
                        .header("Range", "bytes=$start-$end")
                        .build()
                    val chunkBytes = client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            if (start == 0L) throw IllegalStateException("HTTP ${response.code}")
                            return@use -1L // treat as end-of-file
                        }
                        if (total < 0) {
                            total = response.header("Content-Range")
                                ?.substringAfter('/')?.toLongOrNull()
                                ?: response.body?.contentLength() ?: -1L
                        }
                        val body = response.body ?: return@use -1L
                        var written = 0L
                        body.byteStream().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                written += read
                                if (start + written - lastReported >= 512 * 1024) {
                                    downloads.updateProgress(url, start + written, maxOf(total, 0))
                                    lastReported = start + written
                                }
                            }
                        }
                        written
                    }
                    if (chunkBytes <= 0L) break
                    start += chunkBytes
                    downloads.updateProgress(url, start, maxOf(total, 0))
                    if (chunkBytes < CHUNK_SIZE) break        // short chunk => last one
                    if (total in 1..start) break              // reached known total
                }
            }

            downloads.markCompleted(url, outFile.absolutePath)
            Result.success()
        } catch (e: Exception) {
            downloads.markFailed(url)
            Result.failure()
        }
    }

    private fun extensionFor(mimeType: String?): String = when {
        mimeType == null -> "m4a"
        mimeType.contains("webm") || mimeType.contains("opus") -> "webm"
        mimeType.contains("mp4") || mimeType.contains("m4a") || mimeType.contains("aac") -> "m4a"
        else -> "m4a"
    }

    private fun safeName(url: String): String {
        val id = url.substringAfterLast("v=", url.substringAfterLast('/'))
        return id.replace(Regex("[^A-Za-z0-9_-]"), "_").ifEmpty { url.hashCode().toString() }
    }

    companion object {
        const val KEY_URL = "url"
        private const val CHUNK_SIZE = 8L * 1024 * 1024 // 8 MB range requests

        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
