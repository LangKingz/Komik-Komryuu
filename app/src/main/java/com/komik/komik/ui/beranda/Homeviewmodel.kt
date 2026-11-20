package com.komik.komik.ui.beranda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.api.model.DataItem
import com.komik.komik.service.api.model.DataItems
import com.komik.komik.service.client
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.repository.Comic
import com.komik.komik.service.repository.Repository
import com.komik.komik.service.utils.AppConfigHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Homeviewmodel : ViewModel() {

    val repository = Repository(client)
    private val comicRepo = Comic(client)

    private val _source = MutableLiveData<List<DataItems>?>()
    val source: LiveData<List<DataItems>?> get() = _source

    private val _carousel = MutableLiveData<List<DataItem>>()
    val carousel: LiveData<List<DataItem>> get() = _carousel

    private val _latestComics = MutableLiveData<List<getListUpdate>>()
    val latestComics: LiveData<List<getListUpdate>> get() = _latestComics

    private val _popularComics = MutableLiveData<List<getListUpdate>>()
    val popularComics: LiveData<List<getListUpdate>> get() = _popularComics

    private val _genres = MutableLiveData<List<Genre>>()
    val genres: LiveData<List<Genre>> get() = _genres

    private val _isServerOnline = MutableLiveData<Boolean>(true)
    val isServerOnline: LiveData<Boolean> get() = _isServerOnline

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            _genres.postValue(
                listOf(
                    Genre(comicTitle = "", genre = "Semua"),
                    Genre(comicTitle = "", genre = "Action"),
                    Genre(comicTitle = "", genre = "Romance"),
                    Genre(comicTitle = "", genre = "Fantasy"),
                    Genre(comicTitle = "", genre = "Manhwa"),
                    Genre(comicTitle = "", genre = "Comedy"),
                    Genre(comicTitle = "", genre = "Isekai"),
                    Genre(comicTitle = "", genre = "Drama"),
                    Genre(comicTitle = "", genre = "Adventure"),
                    Genre(comicTitle = "", genre = "School Life")
                )
            )

            // 1. Fetch extensions & Server Config
            try {
                val exts = withContext(Dispatchers.IO) {
                    AppConfigHelper.fetchConfig()
                    repository.getSourceExtension()
                }
                _source.postValue(exts)
                _isServerOnline.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _isServerOnline.postValue(false)
            }

            // 2. Fetch Explore / Carousel
            try {
                val explore = withContext(Dispatchers.IO) {
                    repository.getExplore(pageSize = 6, sortBy = "popular")
                }
                _carousel.postValue(explore)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Fetch latest comics
            try {
                val latest = withContext(Dispatchers.IO) {
                    comicRepo.getComic(sourceType.komikcast, "https://komikcast.li/", 1)
                }
                _latestComics.postValue(latest.take(12))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 4. Fetch popular comics
            try {
                val popular = withContext(Dispatchers.IO) {
                    comicRepo.getComic(sourceType.ikiru, "https://id.ikiru.wtf/", 1)
                }
                _popularComics.postValue(popular.take(12))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _loading.postValue(false)
        }
    }

    fun fetchSource() {
        loadHomeData()
    }
}