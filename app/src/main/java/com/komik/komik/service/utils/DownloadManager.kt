package com.komik.komik.service.utils

import android.app.DownloadManager as AndroidDownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.komik.komik.service.client
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.DownloadChapter
import com.komik.komik.service.data.model.DownloadLog
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.repository.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/** Legacy single-image download helper (for PhotoView long-press) */
fun downloadImageManager(context: Context, url: String) {
    try {
        val cleanUrl = url.replace("&amp;", "&").trim()
        val filename = "gambar_${System.currentTimeMillis()}_komryuu.jpg"
        val request = AndroidDownloadManager.Request(Uri.parse(cleanUrl))
            .setTitle(filename)
            .setDescription("Mengunduh Gambar")
            .setNotificationVisibility(AndroidDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as AndroidDownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Mulai mengunduh gambar...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * App-wide Chapter Download Manager
 * Supports concurrent limit (max 2-3 downloads), offline storage, Room sync, and progress observation.
 */
object ChapterDownloadManager {

    private const val TAG = "ChapterDownloadManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(2) // Max 2 concurrent image downloads
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val activeJobs = ConcurrentHashMap<Int, Job>()

    // Real-time progress map: chapterId -> percentage (0-100)
    private val _downloadProgressFlow = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val downloadProgressFlow: StateFlow<Map<Int, Int>> = _downloadProgressFlow.asStateFlow()

    // Real-time status map: chapterId -> status ("DOWNLOADING", "COMPLETED", "FAILED")
    private val _downloadStatusFlow = MutableStateFlow<Map<Int, String>>(emptyMap())
    val downloadStatusFlow: StateFlow<Map<Int, String>> = _downloadStatusFlow.asStateFlow()

    fun isChapterActive(chapterId: Int): Boolean {
        return activeJobs.containsKey(chapterId)
    }

    fun downloadChapter(
        context: Context,
        chapter: Chapter,
        comic: Comic,
        onComplete: (() -> Unit)? = null
    ) {
        val chapterId = chapter.id
        if (activeJobs.containsKey(chapterId)) {
            Toast.makeText(context, "Chapter ${chapter.title} sedang diunduh", Toast.LENGTH_SHORT).show()
            return
        }

        val appContext = context.applicationContext
        val job = scope.launch {
            val db = DatabaseRiwayats.getDatabase(appContext)
            val dao = db.downloadDao()

            val downloadItem = DownloadChapter(
                chapterId = chapter.id,
                comicId = comic.id,
                comicTitle = comic.title,
                chapterTitle = chapter.title,
                chapterNum = chapter.numChapter,
                comicImageUrl = comic.imageUrl,
                chapterUrl = chapter.link,
                sourceTypeStr = comic.location,
                status = DownloadChapter.STATUS_DOWNLOADING,
                progress = 0
            )

            dao.insertOrUpdateDownload(downloadItem)
            updateStatusState(chapterId, DownloadChapter.STATUS_DOWNLOADING)
            updateProgressState(chapterId, 0)

            dao.insertLog(
                DownloadLog(
                    chapterId = chapterId,
                    comicTitle = comic.title,
                    chapterTitle = chapter.title,
                    status = DownloadLog.STATUS_RUNNING,
                    message = "Mulai mengunduh ${chapter.title} - ${comic.title}"
                )
            )

            try {
                val source = typeSource.getSourceType(comic.location)
                val imageRepository = Image(client)
                val imagesList = imageRepository.getImage(chapter.link, source)

                if (imagesList.isEmpty()) {
                    throw IllegalStateException("Daftar gambar chapter kosong atau gagal dimuat")
                }

                val downloadDir = File(appContext.filesDir, "downloads/${comic.id}/${chapter.id}")
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val savedFilePaths = mutableListOf<String>()
                var totalBytes = 0L
                var failedPages = 0

                imagesList.forEachIndexed { index, imgObj ->
                    val file = File(downloadDir, "page_${index + 1}.jpg")
                    val downloadedBytes = semaphore.withPermit {
                        downloadSingleImage(imgObj.image, chapter.link, source, file)
                    }

                    if (downloadedBytes > 0) {
                        totalBytes += downloadedBytes
                        savedFilePaths.add(file.absolutePath)
                    } else {
                        failedPages++
                        Log.e(TAG, "Gagal mengunduh halaman ${index + 1} dari ${imagesList.size}: ${imgObj.image}")
                    }

                    val progress = ((index + 1) * 100) / imagesList.size
                    updateProgressState(chapterId, progress)

                    dao.updateDownloadState(
                        chapterId = chapterId,
                        status = DownloadChapter.STATUS_DOWNLOADING,
                        progress = progress,
                        downloadedPages = savedFilePaths.size,
                        totalPages = imagesList.size,
                        totalSizeBytes = totalBytes,
                        localDir = downloadDir.absolutePath,
                        pathsJson = Gson().toJson(savedFilePaths),
                        completedAt = null,
                        error = null
                    )
                }

                // Validasi bahwa seluruh halaman berhasil diunduh tanpa korup
                if (failedPages > 0 || savedFilePaths.size < imagesList.size) {
                    throw IllegalStateException("Gagal mengunduh $failedPages dari ${imagesList.size} halaman (file kosong atau ditolak server)")
                }

                // Selesai sukses
                val pathsJson = Gson().toJson(savedFilePaths)
                dao.updateDownloadState(
                    chapterId = chapterId,
                    status = DownloadChapter.STATUS_COMPLETED,
                    progress = 100,
                    downloadedPages = savedFilePaths.size,
                    totalPages = savedFilePaths.size,
                    totalSizeBytes = totalBytes,
                    localDir = downloadDir.absolutePath,
                    pathsJson = pathsJson,
                    completedAt = System.currentTimeMillis(),
                    error = null
                )

                updateStatusState(chapterId, DownloadChapter.STATUS_COMPLETED)
                updateProgressState(chapterId, 100)

                dao.insertLog(
                    DownloadLog(
                        chapterId = chapterId,
                        comicTitle = comic.title,
                        chapterTitle = chapter.title,
                        status = DownloadLog.STATUS_COMPLETED,
                        message = "Selesai mengunduh ${chapter.title} (${savedFilePaths.size} halaman, ${formatBytes(totalBytes)})"
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Selesai mengunduh ${chapter.title}", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download failed for chapter $chapterId", e)
                val downloadDir = File(appContext.filesDir, "downloads/${comic.id}/${chapter.id}")
                if (downloadDir.exists()) downloadDir.deleteRecursively()

                dao.updateDownloadState(
                    chapterId = chapterId,
                    status = DownloadChapter.STATUS_FAILED,
                    progress = 0,
                    downloadedPages = 0,
                    totalPages = 0,
                    totalSizeBytes = 0L,
                    localDir = "",
                    pathsJson = "[]",
                    completedAt = null,
                    error = e.localizedMessage
                )

                updateStatusState(chapterId, DownloadChapter.STATUS_FAILED)

                dao.insertLog(
                    DownloadLog(
                        chapterId = chapterId,
                        comicTitle = comic.title,
                        chapterTitle = chapter.title,
                        status = DownloadLog.STATUS_FAILED,
                        message = "Gagal mengunduh ${chapter.title} (${e.localizedMessage ?: "Koneksi terputus"})"
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Gagal mengunduh ${chapter.title}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                activeJobs.remove(chapterId)
            }
        }

        activeJobs[chapterId] = job
    }

    fun downloadBatchChapters(
        context: Context,
        chapters: List<Chapter>,
        comic: Comic
    ) {
        if (chapters.isEmpty()) {
            Toast.makeText(context, "Tidak ada chapter yang dapat diunduh", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Memulai unduhan ${chapters.size} chapter...", Toast.LENGTH_SHORT).show()
        chapters.forEach { chapter ->
            downloadChapter(context, chapter, comic)
        }
    }

    fun deleteDownloadedChapter(context: Context, chapterId: Int, comicId: Int) {
        scope.launch {
            val db = DatabaseRiwayats.getDatabase(context)
            val download = db.downloadDao().getDownloadByChapterIdSync(chapterId)
            if (download != null && download.localDirectory.isNotEmpty()) {
                val dir = File(download.localDirectory)
                if (dir.exists()) dir.deleteRecursively()
            } else {
                val defaultDir = File(context.filesDir, "downloads/$comicId/$chapterId")
                if (defaultDir.exists()) defaultDir.deleteRecursively()
            }
            db.downloadDao().deleteDownloadByChapterId(chapterId)
            updateStatusState(chapterId, DownloadChapter.STATUS_PENDING)
            updateProgressState(chapterId, 0)
        }
    }

    fun deleteAllDownloads(context: Context, onComplete: (() -> Unit)? = null) {
        scope.launch {
            val db = DatabaseRiwayats.getDatabase(context)
            val downloadsDir = File(context.filesDir, "downloads")
            if (downloadsDir.exists()) downloadsDir.deleteRecursively()

            db.downloadDao().deleteAllDownloads()
            _downloadProgressFlow.value = emptyMap()
            _downloadStatusFlow.value = emptyMap()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Semua unduhan telah dihapus", Toast.LENGTH_SHORT).show()
                onComplete?.invoke()
            }
        }
    }

    private fun downloadSingleImage(
        imageUrl: String,
        referer: String,
        type: sourceType,
        destinationFile: File
    ): Long {
        val cleanUrl = imageUrl.replace("&amp;", "&").trim()
        if (cleanUrl.isBlank()) return 0L

        val requestBuilder = Request.Builder().url(cleanUrl)

        // Browser modern default User-Agent & Accept
        val defaultUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        requestBuilder.addHeader("User-Agent", defaultUserAgent)
        requestBuilder.addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")

        val lowerUrl = cleanUrl.lowercase()
        val isDoujin = lowerUrl.contains("desu.pics") || lowerUrl.contains("doujindesu") || type == sourceType.doujindesu
        val isShinigami = lowerUrl.contains("shngm.id") || type == sourceType.shinigami
        val isIkiru = type == sourceType.ikiru || lowerUrl.contains("ikiru.id") || lowerUrl.contains("ikiru.wtf") || lowerUrl.contains("ikiru")

        if (isDoujin) {
            val ddReferer = if (referer.isNotBlank() && (referer.contains("doujin") || referer.contains("desu"))) {
                referer
            } else {
                "https://doujin.desu.xxx/"
            }
            requestBuilder.header("Referer", ddReferer)
            requestBuilder.header("Origin", "https://doujin.desu.xxx")
            requestBuilder.header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1; like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
            requestBuilder.header("X-Requested-With", "XMLHttpRequest")
        } else if (isShinigami) {
            requestBuilder.header("Referer", "https://app.shinigami.asia/")
        } else if (isIkiru) {
            val ikiruReferer = if (referer.isNotBlank() && referer.startsWith("http")) referer else "https://11.ikiru.wtf/"
            requestBuilder.header("Referer", ikiruReferer)
            requestBuilder.header("Origin", "https://11.ikiru.wtf")
            requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            requestBuilder.header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            Log.d("IKIRU_TRACE", "Loading CDN URL: $cleanUrl")
            Log.d("IKIRU_TRACE", "DownloadManager downloadSingleImage: url='$cleanUrl', referer='$ikiruReferer'")
        } else if (referer.isNotBlank() && referer.startsWith("http")) {
            requestBuilder.header("Referer", referer)
        }

        return try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    if (isIkiru) {
                        Log.e("IKIRU_TRACE", "DownloadManager downloadSingleImage gagal HTTP ${resp.code}: $cleanUrl")
                    }
                    Log.w(TAG, "downloadSingleImage gagal HTTP ${resp.code}: $cleanUrl")
                    if (destinationFile.exists()) destinationFile.delete()
                    return 0L
                }

                val contentType = resp.header("Content-Type")?.lowercase().orEmpty()
                if (contentType.contains("html") || contentType.contains("text")) {
                    if (isIkiru) {
                        Log.e("IKIRU_TRACE", "DownloadManager downloadSingleImage gagal, respons bukan gambar (contentType: $contentType): $cleanUrl")
                    }
                    Log.w(TAG, "downloadSingleImage respons bukan gambar (contentType: $contentType): $cleanUrl")
                    if (destinationFile.exists()) destinationFile.delete()
                    return 0L
                }

                val body = resp.body ?: run {
                    if (destinationFile.exists()) destinationFile.delete()
                    return 0L
                }

                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (!destinationFile.exists() || destinationFile.length() <= 0L) {
                    Log.w(TAG, "downloadSingleImage file kosong (0 bytes): $cleanUrl")
                    if (destinationFile.exists()) destinationFile.delete()
                    return 0L
                }

                destinationFile.length()
            }
        } catch (e: Exception) {
            if (isIkiru) {
                Log.e("IKIRU_TRACE", "DownloadManager downloadSingleImage error for $cleanUrl: ${e.message}", e)
            }
            Log.e(TAG, "downloadSingleImage error for $cleanUrl: ${e.message}")
            if (destinationFile.exists()) destinationFile.delete()
            0L
        }
    }

    private fun updateProgressState(chapterId: Int, progress: Int) {
        val map = _downloadProgressFlow.value.toMutableMap()
        map[chapterId] = progress
        _downloadProgressFlow.value = map
    }

    private fun updateStatusState(chapterId: Int, status: String) {
        val map = _downloadStatusFlow.value.toMutableMap()
        map[chapterId] = status
        _downloadStatusFlow.value = map
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }
}
