package com.komik.komik.ui.pages.read

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositoryImage
import com.komik.komik.service.paging.PagingImage
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.repository.Image
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ReadViewmodel : ViewModel() {

    private var _image = MutableLiveData<List<getImages>>()
    val image: MutableLiveData<List<getImages>> get() = _image

    private var _loading = MutableLiveData<Boolean>()
    val loading: MutableLiveData<Boolean> get() = _loading

    private var _error = MutableLiveData<String>()
    val error: MutableLiveData<String> get() = _error

    private var repository = Image(client)

    fun setImage(type: sourceType, url: String) {
        _loading.postValue(true)
        viewModelScope.launch {
           try {
               val data = repository.getImage(url, type)
               Log.d(TAG, "setImage: ${data.size}")
               _image.postValue(data)
               _loading.postValue(false)
           }catch (e:Exception){
               e.printStackTrace()
               Log.d(TAG, "setImage: ${e.localizedMessage}")
               _error.postValue(e.localizedMessage)
           }

        }
    }

    fun getImageFlows(type: sourceType, url: String): Flow<PagingData<getImages>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                PagingImage(repository, url, type)
            }
        ).flow
            .cachedIn(viewModelScope)
    }

    companion object {
        private const val TAG = "ReadViewmodel"
    }
}