package com.komik.komik.service.repository

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositoryDetail
import com.komik.komik.service.parser.comic.comicParser
import com.komik.komik.service.parser.model.ParsedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class Detail(
    private val comicDao: ComicDao,
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
            sourceType.comicscan -> comicParser.parseComictoon(document, url)
            else -> null
        }

        Log.d(TAG, "getDetail: ${parsedData}")

        saveDataToDatabase(parsedData!!)

        return Comic(
            alternativeTitle = parsedData.alternativeTitle,
            title = parsedData.title,
            imageUrl = parsedData.imageUrl,
            url = parsedData.url,
            type = parsedData.type,
            rating = parsedData.rating.toString(),
            status = parsedData.status,
            description = parsedData.description,
            author = parsedData.author.toString(),
            location = parsedData.location
        )

    }

    private suspend fun saveDataToDatabase(parsedData: ParsedData) {
        withContext(Dispatchers.IO) {
            val existingComic = comicDao.getComicByUrl(parsedData.url)

            val requestComic = Comic(
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

            if (existingComic == null) {
                comicDao.insertComicwithChapterAndgenre(
                    requestComic,
                    parsedData.genres,
                    parsedData.chapters
                )
            } else {
                comicDao.updateChaptersForComic(
                    parsedData.title,
                    parsedData.chapters,
                    parsedData.location
                )
            }

        }
    }

    companion object {
        const val TAG = "RepositoryDetail"
    }
}