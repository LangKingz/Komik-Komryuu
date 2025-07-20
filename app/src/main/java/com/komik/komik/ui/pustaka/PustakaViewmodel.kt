package com.komik.komik.ui.pustaka

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.data.model.LibraryDetail

class PustakaViewmodel(private val library: LibraryDao) : ViewModel() {

    // 1. Ubah 'data' menjadi MediatorLiveData agar bisa kita kontrol
    val data = MediatorLiveData<List<LibraryDetail>>()

    // _loading tetap sama, tapi kita ubah val-nya agar lebih aman
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    init {
        // 2. Set loading jadi true saat ViewModel dibuat
        _loading.value = true

        // 3. Ambil sumber data asli dari DAO
        val source: LiveData<List<LibraryDetail>> = library.getLibrary()

        // 4. Gunakan 'addSource' untuk memediasi/menengahi
        data.addSource(source) { result ->
            // Ketika data dari 'source' datang:
            // a. Kirim datanya ke 'data' utama
            data.value = result

            // b. Matikan status loading
            _loading.value = false

            // c. Hentikan mediasi agar loading tidak dimatikan terus-menerus
            data.removeSource(source)
        }
    }


}