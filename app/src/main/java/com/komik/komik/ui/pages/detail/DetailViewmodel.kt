package com.komik.komik.ui.pages.detail

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.repository.Detail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DetailViewmodel(private val repository: Detail) : ViewModel() {

    private var _dataDetail = MutableLiveData<Comic>()
    val dataDetail: MutableLiveData<Comic> = _dataDetail

    private var _error = MutableLiveData<String>()
    val error: MutableLiveData<String> = _error

    private var _loading = MutableLiveData<Boolean>()
    val loading: MutableLiveData<Boolean> = _loading

    fun getDetail(type: sourceType, url: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getDetail(type, url)
//                delay(3000)
                _dataDetail.value = response
                _loading.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message
                Log.d(TAG, "getDetail: ${e.localizedMessage}")

            }
        }
    }

    companion object{
        const val TAG = "DetailViewmodel"

    }

}