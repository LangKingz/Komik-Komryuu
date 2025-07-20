package com.komik.komik.service.repository

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositoryImage
import com.komik.komik.service.parser.comic.ImageParser

import com.komik.komik.service.parser.model.getImages
import org.jsoup.Jsoup

class Image(private val client: client) : RepositoryImage {
    override suspend fun getImage(url: String, type: sourceType): List<getImages> {
        return when (type) {
            sourceType.ikiru -> {

                val html = client.Ikiru.getHtml(url)
                val document = Jsoup.parse(html)
                val pagedData = ImageParser.ikiruParse(document)

                Log.d(TAG, "getImage: $pagedData")

                return pagedData
            }

            sourceType.komikcast-> {
                val html = client.Komikcast.getHtml(url)
                val document = Jsoup.parse(html)
                val pageData = ImageParser.komikcastParse(document)

                Log.d(TAG, "getImage: $pageData")

                pageData
            }

            sourceType.comicscan->{
                val html = client.Comicscan.getHtml(url)
                val document = Jsoup.parse(html)
                val pageData = ImageParser.comicscanParse(document)

                Log.d(TAG, "getImage: $pageData")

                pageData
            }

            sourceType.kiryuu->{
                val html = client.Ikiru.getHtml(url)
                val document = Jsoup.parse(html)
                val pageData = ImageParser.kiryuuParse(document)

                Log.d(TAG, "getImage: $pageData")

                pageData
            }

            sourceType.manhwaindo -> {
                val html = client.Ikiru.getHtml(url)
                val document = Jsoup.parse(html)
                val pageData = ImageParser.manhwaIndoParse(document)

                Log.d(TAG, "getImage: $pageData")
                pageData
            }

            else -> emptyList()
        }
    }

    companion object{
        private const val TAG = "Image"
    }
}