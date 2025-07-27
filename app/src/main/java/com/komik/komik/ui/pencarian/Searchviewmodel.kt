package com.komik.komik.ui.pencarian

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.api.model.DataItem
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.client
import com.komik.komik.service.repository.Repository
import kotlinx.coroutines.launch

class Searchviewmodel:ViewModel() {

    val repository = Repository(client)

    private var _corousel = MutableLiveData<List<DataItem>?>()
    val corousel: MutableLiveData<List<DataItem>?>
        get() = _corousel
    private var _daily = MutableLiveData<List<ListItem>?>()
    val daily: MutableLiveData<List<ListItem>?>
        get() = _daily

    private var _loading = MutableLiveData<Boolean>()
    val loading: MutableLiveData<Boolean>
        get() = _loading

    init {

    }

    fun fetchCorouselData (){
        _loading.postValue(true)
        viewModelScope.launch {
            try {
                val respone = repository.getExplore(5,"popularity")
                Log.d(TAG, "fetchCorouselData: $respone")
                _corousel.postValue(respone)
                _loading.postValue(false)
            }catch (e:Exception){
                e.printStackTrace()
                Log.d(TAG, "fetchCorouselData: ${e.message}")
            }
        }
    }

    fun Extensi (){
        _loading.postValue(true)
        viewModelScope.launch {
            try {
                val respone = repository.getSourceExtension()
                Log.d(TAG, "fetchCorouselData: $respone")
                _daily.postValue(respone)
                _loading.postValue(false)
            }catch (e:Exception){
                e.printStackTrace()
                Log.d(TAG, "fetchCorouselData: ${e.message}")
            }
        }
    }

    companion object{
        private const val TAG = "Searchviewmodel"
    }

}