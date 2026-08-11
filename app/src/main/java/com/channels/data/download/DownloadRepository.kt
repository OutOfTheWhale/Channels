package com.channels.data.download

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.channels.data.db.DownloadDao
import com.channels.data.db.DownloadEntity
import com.channels.domain.model.DownloadItem
import com.channels.domain.model.DownloadState
import com.channels.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Tracks offline audio downloads. Enqueues a [DownloadWorker] per video and exposes
 * the download rows so the UI can show progress and play files offline.
 */
class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
) {

    fun observeDownloads(): Flow<List<DownloadItem>> =
        dao.observeAll().map { rows -> rows.map { it.toItem() } }

    fun observeDownload(videoUrl: String): Flow<DownloadItem?> =
        dao.observe(videoUrl).map { it?.toItem() }

    /** Returns a completed download whose file still exists, else null. */
    suspend fun completedOrNull(videoUrl: String): DownloadItem? {
        val row = dao.get(videoUrl) ?: return null
        val path = row.filePath ?: return null
        if (row.state != DownloadState.COMPLETED.name || !File(path).exists()) return null
        return row.toItem()
    }

    /** Queue a download for a video and kick off the worker. */
    suspend fun enqueue(video: VideoItem) {
        dao.upsert(
            DownloadEntity(
                videoUrl = video.url,
                title = video.title,
                uploader = video.uploader,
                durationSeconds = video.durationSeconds,
                thumbnailUrl = video.thumbnailUrl,
                filePath = null,
                state = DownloadState.QUEUED.name,
                bytesDownloaded = 0,
                totalBytes = 0,
                createdAt = System.currentTimeMillis(),
            ),
        )
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_URL to video.url))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "download:${video.url}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun delete(videoUrl: String) {
        val row = dao.get(videoUrl)
        row?.filePath?.let { runCatching { File(it).delete() } }
        WorkManager.getInstance(context).cancelUniqueWork("download:$videoUrl")
        dao.delete(videoUrl)
    }

    // --- worker callbacks ---
    suspend fun markRunning(url: String) = dao.setState(url, DownloadState.RUNNING.name)
    suspend fun updateProgress(url: String, bytes: Long, total: Long) =
        dao.setProgress(url, DownloadState.RUNNING.name, bytes, total)
    suspend fun markCompleted(url: String, path: String) =
        dao.setCompleted(url, DownloadState.COMPLETED.name, path)
    suspend fun markFailed(url: String) = dao.setState(url, DownloadState.FAILED.name)
    suspend fun get(url: String) = dao.get(url)?.toItem()

    fun downloadsDir(): File = File(context.filesDir, "downloads").apply { mkdirs() }

    private fun DownloadEntity.toItem() = DownloadItem(
        videoUrl = videoUrl,
        title = title,
        uploader = uploader,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl,
        filePath = filePath,
        state = runCatching { DownloadState.valueOf(state) }.getOrDefault(DownloadState.FAILED),
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
    )
}
