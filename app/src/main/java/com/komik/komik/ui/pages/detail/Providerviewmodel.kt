package com.komik.komik.ui.pages.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.genreDao
import com.komik.komik.service.repository.Detail

class Providerviewmodel(
    private val repo: Detail,
    private val comicDao: ComicDao,
    private val chapterDao: chapterDao,
    private val genreDao: genreDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetailViewmodel(repo, comicDao, chapterDao,genreDao) as T
    }

}