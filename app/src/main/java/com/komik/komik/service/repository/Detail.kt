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
    override suspend fun getDetail(type: sourceType, url: String): ParsedData {
        try {
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
                sourceType.shinigami -> comicParser.parseShinigami(url, client,applicationContext)
                sourceType.manhwaindo -> comicParser.parsemanhwaIndo(document, url, applicationContext)
                sourceType.doujindesu -> comicParser.parseDoujinDesu(document, url, applicationContext)
                sourceType.batoto_all -> comicParser.parseBatoto(document, url, applicationContext)
                else -> null
            }

            Log.d(TAG, "getDetail: ${parsedData}")

            return parsedData!!
        }catch (e:Exception){
            Log.d(TAG, "getDetail: ${e.message}")
            return ParsedData(
                url = url,
                title = "",
                imageUrl = "",
                description = e.localizedMessage.toString(),
                author = "",
                location = type.name,
                chapters = emptyList(),
                genres = emptyList(),
                alternativeTitle = "",
                type = "",
            )
        }

    }



    companion object {
        const val TAG = "RepositoryDetail"
    }
}