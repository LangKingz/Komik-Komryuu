package com.komik.komik.ui.beranda

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.client
import com.komik.komik.service.repository.Repository
import kotlinx.coroutines.launch

class Homeviewmodel : ViewModel() {

    val repository = Repository(client)

    private var _source = MutableLiveData<List<ListItem>?>()
    val source: MutableLiveData<List<ListItem>?>
        get() = _source

    private var _loading = MutableLiveData<Boolean>()
    val loading: MutableLiveData<Boolean>
        get() = _loading

    private var _error = MutableLiveData<String>()
    val error: MutableLiveData<String>
        get() = _error

    init {
        fetchSource()
    }

     fun fetchSource(){
         _loading.postValue(true)
        viewModelScope.launch {
            try{
                val response = repository.getSourceExtension()

                _source.postValue(response)
                _loading.postValue(false)
            }catch (e:Exception){
                e.printStackTrace()
                _error.postValue("${e.message}")
            }
        }
    }

}