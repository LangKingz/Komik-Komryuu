package com.komik.komik.ui.pustaka

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.data.model.LibraryDetail
import com.komik.komik.service.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FilterStatus {
    ALL, READING, PLAN, COMPLETED, ON_HOLD
}

enum class SortOrder {
    NEWEST, TITLE_AZ, TITLE_ZA, LAST_READ
}

class PustakaViewmodel(
    private val library: LibraryDao,
    var prefManager: PreferenceManager? = null
) : ViewModel() {

    private val _rawLibrary = library.getLibrary()
    val filteredData = MediatorLiveData<List<LibraryDetail>>()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _syncStatus = MutableLiveData<SyncResult>()
    val syncStatus: LiveData<SyncResult> get() = _syncStatus

    var currentStatusFilter = FilterStatus.ALL
    var currentSortOrder = SortOrder.NEWEST
    var currentSearchQuery = ""
    var isSafeMode = false

    init {
        _loading.value = true

        filteredData.addSource(_rawLibrary) { list ->
            applyFiltersAndSort(list)
            _loading.value = false
        }
    }

    fun setFilterStatus(status: FilterStatus) {
        currentStatusFilter = status
        applyFiltersAndSort(_rawLibrary.value)
    }

    fun setSortOrder(sort: SortOrder) {
        currentSortOrder = sort
        applyFiltersAndSort(_rawLibrary.value)
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query.trim()
        applyFiltersAndSort(_rawLibrary.value)
    }

    fun setSafeModeEnabled(enabled: Boolean) {
        isSafeMode = enabled
        applyFiltersAndSort(_rawLibrary.value)
    }

    fun refreshFilters() {
        applyFiltersAndSort(_rawLibrary.value)
    }

    private fun applyFiltersAndSort(rawList: List<LibraryDetail>?) {
        if (rawList == null) {
            filteredData.value = emptyList()
            return
        }

        var result = rawList

        // 1. Safe Mode Filter
        if (isSafeMode) {
            result = result.filter { it.comic.tags != "adult" }
        }

        // 2. Real-time Search Filter
        if (currentSearchQuery.isNotEmpty()) {
            result = result.filter {
                it.comic.title.contains(currentSearchQuery, ignoreCase = true) ||
                (it.comic.alternativeTitle?.contains(currentSearchQuery, ignoreCase = true) == true) ||
                it.comic.location.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // 3. Status Baca / Shelf Filter
        result = when (currentStatusFilter) {
            FilterStatus.ALL -> result
            FilterStatus.READING -> result.filter { item ->
                val shelf = prefManager?.getComicShelfStatus(item.comic.id)
                if (!shelf.isNullOrBlank() && shelf != "all") {
                    shelf.equals("reading", ignoreCase = true)
                } else {
                    !item.library.isRead && (item.comic.status?.contains("Ongoing", ignoreCase = true) == true || !item.library.isRead)
                }
            }
            FilterStatus.PLAN -> result.filter { item ->
                val shelf = prefManager?.getComicShelfStatus(item.comic.id)
                if (!shelf.isNullOrBlank() && shelf != "all") {
                    shelf.equals("plan", ignoreCase = true)
                } else {
                    !item.library.isRead && item.comic.status?.contains("Ongoing", ignoreCase = true) != true
                }
            }
            FilterStatus.COMPLETED -> result.filter { item ->
                val shelf = prefManager?.getComicShelfStatus(item.comic.id)
                if (!shelf.isNullOrBlank() && shelf != "all") {
                    shelf.equals("completed", ignoreCase = true)
                } else {
                    item.library.isRead || item.comic.status?.contains("Completed", ignoreCase = true) == true
                }
            }
            FilterStatus.ON_HOLD -> result.filter { item ->
                val shelf = prefManager?.getComicShelfStatus(item.comic.id)
                shelf?.equals("on_hold", ignoreCase = true) == true
            }
        }

        // 4. Sorting
        result = when (currentSortOrder) {
            SortOrder.NEWEST -> result.sortedByDescending { it.library.createdAt }
            SortOrder.TITLE_AZ -> result.sortedBy { it.comic.title.lowercase() }
            SortOrder.TITLE_ZA -> result.sortedByDescending { it.comic.title.lowercase() }
            SortOrder.LAST_READ -> result.sortedByDescending { it.library.createdAt }
        }

        filteredData.value = result
    }

    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            library.DeleteALlLibrary()
        }
    }

    fun syncLibraryToFirestore() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _syncStatus.postValue(SyncResult.Error("Silakan masuk dengan akun Google terlebih dahulu."))
            return
        }

        _loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentItems = library.getAllLibraryList()
                if (currentItems.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _loading.value = false
                        _syncStatus.value = SyncResult.Success("Pustaka lokal kosong, tidak ada data yang perlu dicadangkan.")
                    }
                    return@launch
                }

                val db = FirebaseFirestore.getInstance()
                val batch = db.batch()
                val userLibCollection = db.collection("USERS").document(user.uid).collection("LIBRARY")

                for (item in currentItems) {
                    val docRef = userLibCollection.document(item.comic.id.toString())
                    val data = hashMapOf(
                        "comicId" to item.comic.id,
                        "title" to item.comic.title,
                        "url" to item.comic.url,
                        "location" to item.comic.location,
                        "imageUrl" to item.comic.imageUrl,
                        "localImageUri" to (item.comic.localImageUri ?: ""),
                        "status" to (item.comic.status ?: ""),
                        "tags" to (item.comic.tags ?: ""),
                        "createdAt" to item.library.createdAt,
                        "lastSyncAt" to System.currentTimeMillis()
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }

                batch.commit().addOnCompleteListener { task ->
                    _loading.value = false
                    if (task.isSuccessful) {
                        _syncStatus.value = SyncResult.Success("Berhasil mencadangkan ${currentItems.size} komik ke Cloud Firestore!")
                    } else {
                        _syncStatus.value = SyncResult.Error("Gagal mencadangkan: ${task.exception?.localizedMessage}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    _syncStatus.value = SyncResult.Error("Error sinkronisasi: ${e.localizedMessage}")
                }
            }
        }
    }

    sealed class SyncResult {
        data class Success(val message: String) : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
}
