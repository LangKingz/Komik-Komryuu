package com.komik.komik.service.repository

import android.content.Context
import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositoryDetail
import com.komik.komik.service.parser.comic.comicParser
import com.komik.komik.service.parser.model.ParsedData
import com.komik.komik.service.utils.downloadAndSaveImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class Detail(
    private val comicDao: ComicDao,
    private val client: client,
    private val applicationContext: Context
) : RepositoryDetail {
    override suspend fun getDetail(type: sourceType, url: String): Comic {
        val html = when (type) {
            sourceType.komikcast -> client.Komikcast.getHtml(url)
            sourceType.ikiru -> client.Ikiru.getHtml(url)
            sourceType.kiryuu -> client.Ikiru.getHtml(url)
            sourceType.komiku,
            sourceType.comicscan -> client.Comicscan.getHtml(url)
            sourceType.shinigami -> ""
            sourceType.manhwaindo -> client.Ikiru.getHtml(url)
            sourceType.doujindesu -> client.Ikiru.getHtml(url)
            sourceType.batoto_all -> client.ScrapeDataImage.getHtml(url)
            else -> ""
        }

        val document = Jsoup.parse(html)

        val parsedData = when (type) {
            sourceType.ikiru -> comicParser.parseIkiru(document, url,applicationContext)
            sourceType.komikcast -> comicParser.parseKomikcast(document, url ,applicationContext)
            sourceType.kiryuu -> comicParser.parseKiryuu(document, url,applicationContext)
            sourceType.comicscan -> comicParser.parseComictoon(document, url,applicationContext)
            sourceType.shinigami -> comicParser.parseShinigami(url, client, 150)
            sourceType.manhwaindo -> comicParser.parsemanhwaIndo(document, url, applicationContext)
            sourceType.doujindesu -> comicParser.parseDoujinDesu(document, url, applicationContext)
            sourceType.batoto_all -> comicParser.parseBatoto(document, url, applicationContext)
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
            localImageUri = parsedData.localImageUri,
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
                localImageUri = parsedData.localImageUri,
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
//                comicDao.updateChaptersForComic(
//                    parsedData.title,
//                    parsedData.chapters,
//                    parsedData.location
//                )

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
                val existingChapters = comicDao.getChapterByLink(parsedData.title,parsedData.location)

                val existingChapterLinks = existingChapters.map { it.link }.toSet()

                val chapterInsert = chapter.filter { news ->
                    !existingChapterLinks.contains(news.link)
                }.map { newChapter ->
                    newChapter.copy(
                        comicTitle = parsedData.title,
                    )
                }

                if(chapterInsert.isNotEmpty()){
                    comicDao.insertLatestChapter(chapterInsert)
                    Log.d(TAG, "saveDataToDatabase: Chapter Di Simpan")
                }else{
                    Log.d(TAG, "saveDataToDatabase: Chapter Kosong")
                }



            }

        }
    }

    companion object {
        const val TAG = "RepositoryDetail"
    }
}