package com.komik.komik.ui.pages.source

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.service.repository.Comic
import kotlinx.coroutines.launch

class sourceViewmodel: ViewModel() {

    private var _source = MutableLiveData<List<getListUpdate>>()
    val source: MutableLiveData<List<getListUpdate>> get() = _source

    private var _loading = MutableLiveData<Boolean>()
    val loading: MutableLiveData<Boolean> get() = _loading

    private var _error = MutableLiveData<String>()
    val error: MutableLiveData<String> get() = _error

    private var _search = MutableLiveData<List<getListUpdate>>()
    val search: MutableLiveData<List<getListUpdate>> get() = _search


    val repository : Comic = Comic(client)


    fun getSource(type:sourceType,url:String,page:Int){
        _loading.postValue(true)
        viewModelScope.launch {
            try {
                val result = repository.getComic(type,url,page)
                Log.d("sourceViewmodel", "getSource: ${result.size}")
                _source.value = result
                _loading.postValue(false)
            }catch (e:Exception){
                Log.d("sourceViewmodel", "getSource: $e")
                e.printStackTrace()
                _error.postValue(e.message)
                _loading.postValue(false)

            }
        }
    }

    fun search (type: sourceType,query:String,url: String){
        _loading.postValue(true)
        viewModelScope.launch {
            try {
                val response = repository.getSearch(type,query,url)
                _search.postValue(response)
            }catch (e:Exception){
                e.printStackTrace()
                _error.postValue(e.message)
            }finally {
                _loading.postValue(false)
            }
        }

    }

}