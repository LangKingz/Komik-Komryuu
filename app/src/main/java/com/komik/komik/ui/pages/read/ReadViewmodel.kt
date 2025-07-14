package com.komik.komik.ui.pages.read

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositoryImage
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.repository.Image
import kotlinx.coroutines.launch

class ReadViewmodel: ViewModel() {

    private var _image = MutableLiveData<List<getImages>>()
    val image: MutableLiveData<List<getImages>> get() = _image

    private var repository = Image(client)

    fun setImage(type:sourceType, url: String){
        viewModelScope.launch {
            val data = repository.getImage(url, type)
            Log.d(TAG, "setImage: ${data.size}")
            _image.postValue(data)

        }
    }

    companion object{
        private const val TAG = "ReadViewmodel"
    }
}