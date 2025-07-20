package com.komik.komik.ui.pustaka

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.komik.komik.service.data.dao.LibraryDao

class PustakaProvider(private val libraryDao: LibraryDao): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PustakaViewmodel(libraryDao) as T
    }
}