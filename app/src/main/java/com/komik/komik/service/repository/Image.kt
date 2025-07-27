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
        try {
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

                sourceType.doujindesu-> {
                    val html = client.Ikiru.getHtml(url)
                    val document = Jsoup.parse(html)
                    val pageData = ImageParser.doujindesuParseAsync(document,url)
                    Log.d(TAG, "getImage: $pageData")
                    pageData
                }

                sourceType.batoto_all -> {
                    val html = client.ScrapeDataImage.getHtml(url)
                    val document = Jsoup.parse(html)
                    val pageData = ImageParser.batotoParse(document)
                    Log.d(TAG, "getImage: $pageData")
                    pageData
                }

                sourceType.shinigami -> {
                    val pageData = ImageParser.parseShinigami(client,url)
                    Log.d(TAG, "getImage: $pageData")
                    pageData
                }

                else -> emptyList()
            }
        }catch (e:Exception){
            e.printStackTrace()
            Log.e(TAG, "getImage: ${e.message}")
            return emptyList()
        }
    }

    companion object{
        private const val TAG = "Image"
    }
}