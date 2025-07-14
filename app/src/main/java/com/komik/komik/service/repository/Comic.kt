package com.komik.komik.service.repository

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositroyComic
import com.komik.komik.service.parser.comic.SourceParser
import com.komik.komik.service.parser.model.getListUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class Comic(val client: client) : RepositroyComic {
    override suspend fun getComic(type: sourceType,url:String): List<getListUpdate> {

        return withContext(Dispatchers.IO) {
            when (type) {
                sourceType.komikcast -> {

                    val result = mutableListOf<getListUpdate>()

                    for (page in 2..10) {
                        val html = client.Komikcast.getHtml("https://komikcast.li/daftar-komik/page/$page/?sortby=update")
                        val document = Jsoup.parse(html)
                        val pageData =SourceParser.parseKomikcast(document).toList()
                        result.addAll(pageData)

                    }

                    result

                }
                sourceType.ikiru -> {

                    val result = mutableListOf<getListUpdate>()
                    for (page in 1..10) {
                        val html = client.Ikiru.getHtml("https://id.ikiru.wtf/latest-update/?the_page=$page")
                        val document = Jsoup.parse(html)
                        val pageData =SourceParser.parseIkiru(document).toList()

                        result.addAll(pageData)
                    }

                    result
                }
                sourceType.komiku,
                sourceType.shinigami -> {
                    val data = mutableListOf<getListUpdate>()

                    for(page in 1..10){
                        val result = SourceParser.parseShinigami(client,page)
                        Log.d(TAG, "getComic: ${result?.size}")
                        data.addAll(result!!)
                    }

                    data
                }
                sourceType.comicscan -> {
                    val result = mutableListOf<getListUpdate>()

                    for(page in 1..10){
                        val html = client.Comicscan.getHtml("https://lc3.cosmicscans.asia/manga/?page=$page&order=update")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseComicscan(document).toList()
                        result.addAll(pageData)
                    }

                    Log.d(TAG, "getComic: ${result.size}")

                    result
                }
                sourceType.kiryuu -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..10){
                        val html = com.komik.komik.service.client.Ikiru.getHtml("https://kiryuu02.com/manga/?page=$page&order=update")
                        val document = Jsoup.parse(html)

                        val pageData = SourceParser.parseKiryuu(document)
                        result.addAll(pageData)
                    }
                    result
                }
                else -> emptyList() // Default case, kembalikan list kosong
            }
        }

    }

    override suspend fun getSearch(type: sourceType, query: String): List<getListUpdate> {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "Comic"
    }
}