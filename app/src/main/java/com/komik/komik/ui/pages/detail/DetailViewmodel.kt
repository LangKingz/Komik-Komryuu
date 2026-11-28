package com.komik.komik.ui.pages.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.genreDao
import com.komik.komik.service.data.model.ChapterwithComic
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.ParsedData
import com.komik.komik.service.repository.Detail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DetailViewmodel(
    private val repository: Detail,
    private val comicDao: ComicDao,
    private val chapterDao: chapterDao,
    private val genreDao: genreDao
) : ViewModel() {

    private val _dataDetail = MutableLiveData<ParsedData>()
    val dataDetail: LiveData<ParsedData> = _dataDetail

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _dataChapter = MutableLiveData<List<ChapterwithComic>>()
    val dataChapter: LiveData<List<ChapterwithComic>> = _dataChapter

    private val _dataChapterDetail = MutableLiveData<Comic>()
    val dataChapterDetail: LiveData<Comic> = _dataChapterDetail

    private val _dataGenre = MutableLiveData<List<Genre>>()
    val dataGenre: LiveData<List<Genre>> = _dataGenre

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _updateChapter = MutableLiveData<String>()
    val updateChapter: LiveData<String> = _updateChapter

    // Chunked Loading & Background Sorting/Filtering
    private val _visibleChapters = MutableLiveData<List<ChapterwithComic>>()
    val visibleChapters: LiveData<List<ChapterwithComic>> = _visibleChapters

    private val _totalChapterCount = MutableLiveData<Int>()
    val totalChapterCount: LiveData<Int> = _totalChapterCount

    private val _isChapterLoading = MutableLiveData<Boolean>(false)
    val isChapterLoading: LiveData<Boolean> = _isChapterLoading

    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _hasMoreChapters = MutableLiveData<Boolean>(false)
    val hasMoreChapters: LiveData<Boolean> = _hasMoreChapters

    private var allLoadedChapters: List<ChapterwithComic> = emptyList()
    private var processedChapters: List<ChapterwithComic> = emptyList()
    private var currentSortDescending: Boolean = true
    private var currentSearchQuery: String = ""
    private var currentDisplayedCount: Int = CHUNK_SIZE

    fun getAllLoadedChapters(): List<ChapterwithComic> = allLoadedChapters

    companion object {
        const val TAG = "DetailViewmodel"
        private const val CHUNK_SIZE = 40
    }

    fun refreshData(type: sourceType, url: String) {
        _loading.value = true
        _isChapterLoading.value = true
        viewModelScope.launch {
            try {
                val parsedData = repository.getDetail(type, url)
                val title = parsedData.title
                saveDataToDatabase(parsedData)
                _loading.value = false
                if (title.isNotEmpty()) {
                    val data = withContext(Dispatchers.IO) {
                        chapterDao.getChaptersForComic(title)
                    }
                    _dataChapter.postValue(data)
                    withContext(Dispatchers.Default) {
                        allLoadedChapters = data
                        applyFilterAndSortInternal()
                    }
                } else {
                    Log.d(TAG, "refreshData: ")
                    _error.postValue(parsedData.description)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.postValue(e.message)
                Log.d(TAG, "getDetail: ${e.localizedMessage}")
            } finally {
                _loading.postValue(false)
                _isChapterLoading.postValue(false)
            }
        }
    }

    fun getChapters(sourceType: sourceType, url: String) {
        if (sourceType == com.komik.komik.service.enumclass.sourceType.ikiru || url.contains("ikiru", ignoreCase = true)) {
            Log.d("IKIRU_TRACE", "DetailViewmodel.getChapters: sourceType=$sourceType, url='$url'")
        }
        _loading.value = true
        _isChapterLoading.value = true
        viewModelScope.launch {
            try {
                var comic: Comic? = null
                var chapters: List<ChapterwithComic> = emptyList()
                var genres: List<Genre> = emptyList()

                withContext(Dispatchers.IO) {
                    var data = comicDao.getComicByUrl(url)

                    if (data == null) {
                        val parsedData = repository.getDetail(sourceType, url)
                        if (parsedData.chapters.isNotEmpty()) {
                            saveDataToDatabase(parsedData)
                            data = comicDao.getComicByUrl(url)
                        } else {
                            withContext(Dispatchers.Main) {
                                
                                _error.value = parsedData.description
                                Log.d(TAG, "getChapters: ${_error}")
                            }
                        }
                    }

                    if (data != null) {
                        comic = data
                        genres = genreDao.getGenreByComicTitle(data.title)
                        chapters = chapterDao.getChapterForComic(data.id, data.location)
                    }
                }

                comic?.let { _dataChapterDetail.postValue(it) }
                _dataGenre.postValue(genres)
                _dataChapter.postValue(chapters)

                withContext(Dispatchers.Default) {
                    allLoadedChapters = chapters
                    applyFilterAndSortInternal()
                }

                _loading.postValue(false)
                _isChapterLoading.postValue(false)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, "getChapters: ${e.localizedMessage}")
                _error.postValue(e.message)
            } finally {
                _loading.postValue(false)
                _isChapterLoading.postValue(false)
            }
        }
    }

    fun filterChapters(query: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                currentSearchQuery = query.trim()
                applyFilterAndSortInternal()
            }
        }
    }

    fun toggleSortOrder(): Boolean {
        currentSortDescending = !currentSortDescending
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                applyFilterAndSortInternal()
            }
        }
        return currentSortDescending
    }

    fun isSortDescending(): Boolean = currentSortDescending

    fun loadNextChunk() {
        if (_isLoadingMore.value == true || _hasMoreChapters.value == false) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            withContext(Dispatchers.Default) {
                currentDisplayedCount += CHUNK_SIZE
                val sublist = processedChapters.take(currentDisplayedCount)
                _visibleChapters.postValue(sublist)
                _hasMoreChapters.postValue(currentDisplayedCount < processedChapters.size)
            }
            _isLoadingMore.postValue(false)
        }
    }

    private suspend fun applyFilterAndSortInternal() {
        val filtered = if (currentSearchQuery.isEmpty()) {
            allLoadedChapters
        } else {
            val q = currentSearchQuery.lowercase(Locale.getDefault())
            allLoadedChapters.filter { item ->
                item.chapter.title.lowercase(Locale.getDefault()).contains(q) ||
                item.chapter.numChapter.toString().contains(q)
            }
        }

        val sorted = if (currentSortDescending) {
            filtered.sortedByDescending { it.chapter.numChapter }
        } else {
            filtered.sortedBy { it.chapter.numChapter }
        }

        processedChapters = sorted
        currentDisplayedCount = CHUNK_SIZE
        val initialChunk = processedChapters.take(currentDisplayedCount)

        _totalChapterCount.postValue(allLoadedChapters.size)
        _visibleChapters.postValue(initialChunk)
        _hasMoreChapters.postValue(currentDisplayedCount < processedChapters.size)
    }

    private suspend fun saveDataToDatabase(parsedData: ParsedData) {
        withContext(Dispatchers.IO) {
            val existingComic = comicDao.getComicByUrl(parsedData.url)

            val requestComic = Comic(
                title = parsedData.title,
                alternativeTitle = parsedData.alternativeTitle,
                url = parsedData.url,
                imageUrl = parsedData.imageUrl,
                localImageUri = parsedData.localImageUri,
                description = parsedData.description,
                author = parsedData.author.toString(),
                location = parsedData.location,
                rating = parsedData.rating.toString(),
                type = parsedData.type,
                tags = parsedData.tags ?: "safe",
                status = parsedData.status ?: "Ongoing"
            )

            if (existingComic == null) {
                val newComicId = comicDao.insertComicId(requestComic).toInt()
                val chapterWithComicId = parsedData.chapters.map { chapter ->
                    chapter.copy(comicId = newComicId)
                }
                comicDao.insertComicwithChapterAndgenre(
                    parsedData.genres,
                    chapterWithComicId
                )
            } else {
                val comicId = existingComic.id
                comicDao.updateComic(
                    existingComic.copy(
                        status = parsedData.status,
                        description = parsedData.description,
                        rating = parsedData.rating.toString(),
                        location = parsedData.location
                    )
                )

                val chapter = parsedData.chapters
                val existingChapters = comicDao.getChapterByLink(parsedData.title, parsedData.location)
                val existingChapterLinks = existingChapters.map { it.link }.toSet()

                val chapterInsert = chapter.filter { news ->
                    !existingChapterLinks.contains(news.link)
                }.map { newChapter ->
                    newChapter.copy(
                        comicTitle = parsedData.title,
                        comicId = comicId
                    )
                }

                if (chapterInsert.isNotEmpty()) {
                    comicDao.insertLatestChapter(chapterInsert)
                    Log.d(TAG, "saveDataToDatabase: Chapter Di Simpan")
                    withContext(Dispatchers.Main) {
                        _updateChapter.value = "${chapterInsert.size} Chapter Di Simpan"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _updateChapter.value = "Tidak ada chapter baru"
                    }
                    Log.d(TAG, "saveDataToDatabase: Chapter Kosong")
                }
            }
        }
    }
}