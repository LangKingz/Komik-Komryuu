package com.komik.komik.ui.pages.source

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.client
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.repository.Comic
import kotlinx.coroutines.launch

class sourceViewmodel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseRiwayats.getDatabase(application)
    val allHistory: LiveData<List<RiwayatDetail>> = db.riwayatdao().getRiwayatDetail()

    private val _source = MutableLiveData<List<getListUpdate>>()
    val source: LiveData<List<getListUpdate>> get() = _source

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _isPaginating = MutableLiveData<Boolean>()
    val isPaginating: LiveData<Boolean> get() = _isPaginating

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _search = MutableLiveData<List<getListUpdate>>()
    val search: LiveData<List<getListUpdate>> get() = _search

    private val _pingMs = MutableLiveData<Long?>()
    val pingMs: LiveData<Long?> get() = _pingMs

    val repository: Comic = Comic(client)

    // Pagination State
    val pageLimit = 24
    var currentOffset = 0
        private set
    var isLoadingPage = false
        private set
    var hasNextPage = true
        private set
    val cachedComics = mutableListOf<getListUpdate>()

    fun loadInitialData(type: sourceType, url: String) {
        if (isLoadingPage) return
        isLoadingPage = true
        _loading.postValue(true)
        _error.postValue(null)
        currentOffset = 0
        hasNextPage = true
        cachedComics.clear()

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val result = repository.getComicPaged(type, url, limit = pageLimit, offset = currentOffset)
                val duration = System.currentTimeMillis() - startTime
                _pingMs.postValue(duration)
                Log.d("sourceViewmodel", "loadInitialData: ${result.size} items in ${duration}ms")

                cachedComics.addAll(result)
                _source.postValue(cachedComics.toList())
                currentOffset += result.size
                if (result.size < pageLimit) {
                    hasNextPage = false
                }
            } catch (e: Exception) {
                Log.e("sourceViewmodel", "loadInitialData error: ${e.message}", e)
                _error.postValue(e.message)
            } finally {
                isLoadingPage = false
                _loading.postValue(false)
            }
        }
    }

    fun loadNextOffset(type: sourceType, url: String) {
        if (isLoadingPage || !hasNextPage) return
        isLoadingPage = true
        _isPaginating.postValue(true)

        viewModelScope.launch {
            try {
                val result = repository.getComicPaged(type, url, limit = pageLimit, offset = currentOffset)
                Log.d("sourceViewmodel", "loadNextOffset: fetched ${result.size} items at offset $currentOffset")

                if (result.isNotEmpty()) {
                    cachedComics.addAll(result)
                    _source.postValue(cachedComics.toList())
                    currentOffset += result.size
                }
                if (result.size < pageLimit) {
                    hasNextPage = false
                }
            } catch (e: Exception) {
                Log.e("sourceViewmodel", "loadNextOffset error: ${e.message}", e)
                // Tetap simpan data lama tanpa merusak list UI
            } finally {
                isLoadingPage = false
                _isPaginating.postValue(false)
            }
        }
    }

    // Delegasi backward compatibility
    fun getSource(type: sourceType, url: String, page: Int = 30) {
        loadInitialData(type, url)
    }

    fun search(type: sourceType, query: String, url: String) {
        _loading.postValue(true)
        viewModelScope.launch {
            try {
                val response = repository.getSearch(type, query, url)
                _search.postValue(response)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.postValue(e.message)
            } finally {
                _loading.postValue(false)
            }
        }
    }
}
