package com.komik.komik.ui.pustaka

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.utils.PreferenceManager

class PustakaProvider(
    private val libraryDao: LibraryDao,
    private val prefManager: PreferenceManager? = null
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PustakaViewmodel(libraryDao, prefManager) as T
    }
}
