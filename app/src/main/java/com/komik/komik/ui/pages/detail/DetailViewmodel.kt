package com.komik.komik.ui.pages.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.genreDao
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.ChapterwithComic
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.ParsedData
import com.komik.komik.service.repository.Detail
import com.komik.komik.service.repository.Detail.Companion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log

class DetailViewmodel(
    private val repository: Detail,
    private val comicDao: ComicDao,
    private val chapterDao: chapterDao,
    private val genreDao: genreDao
) : ViewModel() {

    private var _dataDetail = MutableLiveData<ParsedData>()
    val dataDetail: MutableLiveData<ParsedData> = _dataDetail

    private var _error = MutableLiveData<String>()
    val error: MutableLiveData<String> = _error

    private var _dataChapter = MutableLiveData<List<ChapterwithComic>>()
    val dataChapter: LiveData<List<ChapterwithComic>> = _dataChapter

    private var _dataChapterDetail = MutableLiveData<Comic>()
    val dataChapterDetail: LiveData<Comic> = _dataChapterDetail

    private var _dataGenre = MutableLiveData<List<Genre>>()
    val dataGenre: LiveData<List<Genre>> = _dataGenre

    private var _loading = MutableLiveData<Boolean>()
    val loading: MutableLiveData<Boolean> = _loading

    private var _updateChapter = MutableLiveData<String>()
    val updateChapter: LiveData<String> = _updateChapter

    fun refreshData(type: sourceType, url: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val parsedData = repository.getDetail(type, url)
                val title = parsedData.title
                saveDataToDatabase(parsedData)
                _loading.value = false
                if (title.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "refreshData: ${parsedData.title}")
                        val data = chapterDao.getChaptersForComic(parsedData.title)
                        _dataChapter.postValue(data)
                    }
                }else{
                    _error.value = parsedData.description
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message
                Log.d(TAG, "getDetail: ${e.localizedMessage}")
            } finally {
                _loading.value = false
            }
        }
    }

    fun getChapters(sourceType: sourceType, url: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    var data = comicDao.getComicByUrl(url)

                    if (data == null) {
                        val parsedData = repository.getDetail(sourceType, url)
                        if(parsedData.chapters.isNotEmpty()){
                            saveDataToDatabase(parsedData)
                            data = comicDao.getComicByUrl(url)
                        }
                        else{
                           withContext(Dispatchers.Main){
                               _error.value = parsedData.description
                           }
                        }
                    }

                    if (data != null) {
                        _dataChapterDetail.postValue(data)
                        val genre = genreDao.getGenreByComicTitle(data.title)
                        _dataGenre.postValue(genre)
                        val chapter = chapterDao.getChapterForComic(data.id, data.location)
                        _dataChapter.postValue(chapter)
                    }
                }
                _loading.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, "getChapters: ${e.localizedMessage}")
                error.value = e.message
            }
        }

    }

    private suspend fun saveDataToDatabase(parsedData: ParsedData) {
        withContext(Dispatchers.IO) {
            val existingComic = comicDao.getComicByUrl(parsedData.url)

            val requestComic = Comic(
                title = parsedData.title,
                alternativeTitle = parsedData.alternativeTitle,
                url = parsedData.url,
                imageUrl = parsedData.imageUrl,
                localImageUri = parsedData.localImageUri,
                description = parsedData.description,
                author = parsedData.author.toString(),
                location = parsedData.location,
                rating = parsedData.rating.toString(),
                type = parsedData.type,
                tags = parsedData.tags ?: "safe",
                status = parsedData.status ?: "Ongoing"
            )

            if (existingComic == null) {

                val newComidId = comicDao.insertComicId(requestComic).toInt()
                val chapterWithcomidId = parsedData.chapters.map { chapter ->
                    chapter.copy(
                        comicId = newComidId
                    )
                }
                comicDao.insertComicwithChapterAndgenre(
                    parsedData.genres,
                    chapterWithcomidId
                )


            } else {

                val comicid = existingComic.id

                comicDao.updateComic(
                    existingComic.copy(
                        status = parsedData.status,
                        description = parsedData.description,
                        rating = parsedData.rating.toString(),
                        location = parsedData.location,
//                        localImageUri = parsedData.localImageUri
                    )
                )

                val chapter = parsedData.chapters
                val existingChapters =
                    comicDao.getChapterByLink(parsedData.title, parsedData.location)

                val existingChapterLinks = existingChapters.map { it.link }.toSet()

                val chapterInsert = chapter.filter { news ->
                    !existingChapterLinks.contains(news.link)
                }.map { newChapter ->
                    newChapter.copy(
                        comicTitle = parsedData.title,
                        comicId = comicid
                    )
                }

                if (chapterInsert.isNotEmpty()) {
                    comicDao.insertLatestChapter(chapterInsert)

                    Log.d(TAG, "saveDataToDatabase: Chapter Di Simpan")

                    withContext(Dispatchers.Main) {
                        _updateChapter.value = "${chapterInsert.size} Chapter Di Simpan"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _updateChapter.value = "Tidak ada chapter"
                    }
                    Log.d(TAG, "saveDataToDatabase: Chapter Kosong")
                }


            }

        }
    }

    companion object {
        const val TAG = "DetailViewmodel"

    }

}