package com.komik.komik.ui.beranda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.api.model.DataItem
import com.komik.komik.service.api.model.DataItems
import com.komik.komik.service.client
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.repository.Repository
import com.komik.komik.service.utils.AppConfigHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Homeviewmodel : ViewModel() {

    val repository = Repository(client)

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

    private var lastIncludeInactive: Boolean = false

    fun loadHomeData(includeInactive: Boolean = false) {
        lastIncludeInactive = includeInactive
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            // Fetch extensions & Server Config
            try {
                val exts = withContext(Dispatchers.IO) {
                    AppConfigHelper.fetchConfig()
                    repository.getSourceExtension(includeInactive = includeInactive)
                }
                _source.postValue(exts)
                _isServerOnline.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _isServerOnline.postValue(false)
                _error.postValue(e.localizedMessage ?: "Gagal memuat sumber ekstensi")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun fetchSource(includeInactive: Boolean = lastIncludeInactive) {
        loadHomeData(includeInactive)
    }
}
