package com.komik.komik.ui.pages.read

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.komik.komik.service.client
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.DownloadChapter
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.repository.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReadViewmodel : ViewModel() {

    private val _image = MutableLiveData<List<getImages>>()
    val image: LiveData<List<getImages>> get() = _image

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    /** Triggered when images are freshly loaded (chapter switch complete) */
    private val _chapterLoaded = MutableLiveData<Boolean>()
    val chapterLoaded: LiveData<Boolean> get() = _chapterLoaded

    private val repository = Image(client)

    // Preload cache state
    var preloadedNextChapterId: Int? = null
        private set
    var preloadedNextImages: List<getImages>? = null
        private set
    var isPreloadingNext: Boolean = false
        private set

    fun setImage(type: sourceType, url: String, chapterId: Int? = null, context: Context? = null) {
        // If chapter matches our preloaded cache, display immediately with zero network delay!
        if (chapterId != null && preloadedNextChapterId == chapterId && !preloadedNextImages.isNullOrEmpty()) {
            Log.d(TAG, "setImage: Instant load from preloaded cache for chapterId=$chapterId")
            val cached = preloadedNextImages!!
            preloadedNextChapterId = null
            preloadedNextImages = null
            isPreloadingNext = false
            _image.postValue(cached)
            _loading.postValue(false)
            _chapterLoaded.postValue(true)
            return
        }

        // If not cached or different chapter, reset preload cache and fetch from network
        clearPreloadCache()
        _loading.postValue(true)
        _chapterLoaded.postValue(false)

        viewModelScope.launch(Dispatchers.IO) {
            // Check offline downloaded files first
            if (context != null) {
                try {
                    val db = DatabaseRiwayats.getDatabase(context.applicationContext)
                    val download = if (chapterId != null && chapterId != 0) {
                        db.downloadDao().getDownloadByChapterIdSync(chapterId)
                    } else {
                        db.downloadDao().getDownloadByUrlSync(url)
                    }
                    if (download != null && download.status == DownloadChapter.STATUS_COMPLETED) {
                        val localPaths = download.getLocalImagePaths()
                        if (localPaths.isNotEmpty() && localPaths.all { val f = java.io.File(it); f.exists() && f.length() > 0L }) {
                            Log.d(TAG, "setImage: Loaded completely offline for chapterId=${download.chapterId} (${localPaths.size} images)")
                            val localImages = localPaths.mapIndexed { idx, path ->
                                getImages(index = idx + 1, image = path)
                            }
                            _image.postValue(localImages)
                            _loading.postValue(false)
                            _chapterLoaded.postValue(true)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed checking offline download: ${e.message}")
                }
            }

            try {
                if (type == sourceType.ikiru || url.contains("ikiru", ignoreCase = true)) {
                    Log.d("IKIRU_TRACE", "ReadViewmodel.setImage: fetching images for type=$type, url='$url'")
                }
                val data = repository.getImage(url, type)
                if (type == sourceType.ikiru || url.contains("ikiru", ignoreCase = true)) {
                    Log.d("IKIRU_TRACE", "ReadViewmodel.setImage: successfully received ${data.size} images for Ikiru")
                }
                Log.d(TAG, "setImage: ${data.size} images loaded")
                _image.postValue(data)
                _loading.postValue(false)
                _chapterLoaded.postValue(true)
            } catch (e: Exception) {
                if (type == sourceType.ikiru || url.contains("ikiru", ignoreCase = true)) {
                    Log.e("IKIRU_TRACE", "ReadViewmodel.setImage error for Ikiru url='$url': ${e.message}", e)
                }
                e.printStackTrace()
                Log.d(TAG, "setImage error: ${e.localizedMessage}")
                _error.postValue(e.localizedMessage ?: "Gagal memuat halaman")
                _loading.postValue(false)
                _chapterLoaded.postValue(false)
            }
        }
    }

    /**
     * Preloads images for the upcoming chapter in the background:
     * 1. Fetches the image URLs list without disturbing the UI thread.
     * 2. Preloads the first 3-5 images with Glide DiskCacheStrategy.ALL into memory/disk.
     */
    fun preloadNextChapter(
        context: Context,
        type: sourceType,
        nextChapterId: Int,
        nextChapterUrl: String
    ) {
        if (isPreloadingNext || preloadedNextChapterId == nextChapterId) return
        isPreloadingNext = true
        Log.d(TAG, "preloadNextChapter: prefetching images for chapterId=$nextChapterId")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appContext = context.applicationContext
                val db = DatabaseRiwayats.getDatabase(appContext)
                val offlineDownload = db.downloadDao().getDownloadByChapterIdSync(nextChapterId)
                    ?: db.downloadDao().getDownloadByUrlSync(nextChapterUrl)

                if (offlineDownload != null && offlineDownload.status == DownloadChapter.STATUS_COMPLETED) {
                    val localPaths = offlineDownload.getLocalImagePaths()
                    if (localPaths.isNotEmpty() && localPaths.all { val f = java.io.File(it); f.exists() && f.length() > 0L }) {
                        preloadedNextChapterId = nextChapterId
                        preloadedNextImages = localPaths.mapIndexed { idx, path ->
                            getImages(index = idx + 1, image = path)
                        }
                        return@launch
                    }
                }

                val data = repository.getImage(nextChapterUrl, type)
                if (data.isNotEmpty()) {
                    preloadedNextChapterId = nextChapterId
                    preloadedNextImages = data
                    Log.d(TAG, "preloadNextChapter: success! Preloading first 3-5 images with Glide...")

                    val preloadCount = minOf(5, data.size)
                    val appContext = context.applicationContext
                    for (i in 0 until preloadCount) {
                        val imgUrl = data[i].image
                        try {
                            if (type == sourceType.doujindesu) {
                                val headers = LazyHeaders.Builder()
                                    .addHeader("Referer", nextChapterUrl)
                                    .addHeader(
                                        "User-Agent",
                                        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1; like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                                    )
                                    .addHeader("X-Requested-With", "XMLHttpRequest")
                                    .build()
                                val glideUrl = GlideUrl(imgUrl, headers)
                                Glide.with(appContext)
                                    .load(glideUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .preload()
                            } else {
                                Glide.with(appContext)
                                    .load(imgUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .preload()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to preload image $i: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "preloadNextChapter failed: ${e.localizedMessage}")
            } finally {
                isPreloadingNext = false
            }
        }
    }

    fun clearPreloadCache() {
        preloadedNextChapterId = null
        preloadedNextImages = null
        isPreloadingNext = false
    }

    companion object {
        private const val TAG = "ReadViewmodel"
    }
}
