package com.komik.komik.service.repository

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.database.dao.ComicDao
import com.komik.komik.service.database.dao.chapterDao
import com.komik.komik.service.database.dao.genreDao
import com.komik.komik.service.database.model.Comic
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositoryDetail
import com.komik.komik.service.parser.comic.comicParser
import com.komik.komik.service.parser.model.ParsedData
import com.komik.komik.service.parser.model.getListUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class Detail(
    private val comicDao: ComicDao,
    private val genreDao: genreDao,
    private val chapterDao: chapterDao,
    private val client: client
) : RepositoryDetail {
    override suspend fun getDetail(type: sourceType, url: String): Comic {
        val html = when (type) {
            sourceType.komikcast -> client.Komikcast.getHtml(url)
            sourceType.ikiru -> client.Ikiru.getHtml(url)
            sourceType.kiryuu -> client.Ikiru.getHtml(url)
            sourceType.komiku,
            sourceType.comicscan -> client.Comicscan.getHtml(url)
            sourceType.shinigami -> ""
            else -> ""
        }

        val document = Jsoup.parse(html)

        val parsedData = when (type) {
            sourceType.ikiru -> comicParser.parseIkiru(document, url)
            sourceType.komikcast -> comicParser.parseKomikcast(document, url)
            sourceType.kiryuu -> comicParser.parseKiryuu(document, url)
            else -> null
        }

        saveDataToDatabase(parsedData!!)

        return Comic(
            alternativeTitle = parsedData.alternativeTitle,
            title = parsedData.title,
            imageUrl =parsedData.imageUrl,
            url = parsedData.url,
            type = parsedData.type,
            rating = "",
            status = parsedData.status,
            description = parsedData.description,
            author = parsedData.author.toString(),
            location = parsedData.location
        )

    }

    private suspend fun saveDataToDatabase(parsedData: ParsedData) {
        withContext(Dispatchers.IO) {
            val existingComic = comicDao.getComicByUrl(parsedData.url)

            if (existingComic == null) {
                comicDao.insertComic(
                    Comic(
                        title = parsedData.title,
                        alternativeTitle = parsedData.alternativeTitle,
                        url = parsedData.url,
                        imageUrl = parsedData.imageUrl,
                        description = parsedData.description,
                        author = parsedData.author.toString(),
                        location = parsedData.location,
                        rating = parsedData.rating.toString(),
                        type = parsedData.type,
                        status = parsedData.status
                    )
                )
            }

            val existingGenres = genreDao.getGenreByComicTitle(parsedData.title)
            if (existingGenres.isEmpty()) {
                genreDao.insert(parsedData.genres)
            }

            val existingChapters = chapterDao.getChaptersForComic(parsedData.title)
            if (existingChapters.isEmpty()) {
                chapterDao.insertChapters(parsedData.chapters)
            } else {
                Log.d(TAG, "saveDataToDatabase:  ${parsedData.title}")
                chapterDao.deleteChapters(parsedData.title)
                chapterDao.insertChapters(parsedData.chapters)
            }
        }
    }

    companion object {
        const val TAG = "RepositoryDetail"
    }
}