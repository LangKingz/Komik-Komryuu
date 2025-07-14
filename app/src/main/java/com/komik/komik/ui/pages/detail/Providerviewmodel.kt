package com.komik.komik.ui.pages.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.komik.komik.service.repository.Detail

class Providerviewmodel(private val repo: Detail):ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetailViewmodel(repo) as T
    }

}