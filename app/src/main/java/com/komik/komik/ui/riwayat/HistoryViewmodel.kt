package com.komik.komik.ui.riwayat

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.komik.komik.service.database.model.Riwayat

class HistoryViewmodel(application: Application): ViewModel() {

    private var _history = MutableLiveData<List<Riwayat>>()
    val history: MutableLiveData<List<Riwayat>> get() = _history



}