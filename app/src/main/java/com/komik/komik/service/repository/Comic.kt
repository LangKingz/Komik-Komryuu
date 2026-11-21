package com.komik.komik.service.repository

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositroyComic
import com.komik.komik.service.parser.cinema.CinemaParser
import com.komik.komik.service.parser.comic.SearchParser
import com.komik.komik.service.parser.comic.SourceParser
import com.komik.komik.service.parser.comic.comicParser
import com.komik.komik.service.parser.model.getListUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class Comic(val client: client) : RepositroyComic {
    override suspend fun getComic(type: sourceType, url: String, pages: Int): List<getListUpdate> {

        return withContext(Dispatchers.IO) {
            when (type) {
                sourceType.komikcast -> {

                    val result = mutableListOf<getListUpdate>()

                    for (page in 2..pages) {
                        val html =
                            client.Komikcast.getHtml("${url}daftar-komik/page/$page/?sortby=update")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseKomikcast(document).toList()
                        result.addAll(pageData)

                    }

                    result

                }

                sourceType.ikiru -> {

                    val result = mutableListOf<getListUpdate>()
                    for (page in 1..pages) {
                        val html =
                            client.Ikiru.getHtml("${url}latest-update/?the_page=$page")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseIkiru(document).toList()

                        result.addAll(pageData)
                    }

                    result
                }

                sourceType.komiku,
                sourceType.shinigami -> {
                    val data = mutableListOf<getListUpdate>()

                    for (page in 1..10) {
                        val result = SourceParser.parseShinigami(client, page)
                        Log.d(TAG, "getComic: ${result?.size}")
                        data.addAll(result!!)
                    }

                    data
                }

                sourceType.comicscan -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..pages) {
                        val html =
                            client.Comicscan.getHtml("${url}manga/?page=$page&order=update")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseComicscan(document).toList()
                        result.addAll(pageData)
                    }

                    Log.d(TAG, "getComic: ${result.size}")

                    result
                }

                sourceType.kiryuu -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..pages) {
                        val html =
                            com.komik.komik.service.client.Ikiru.getHtml("${url}manga/?page=$page&order=update")
                        val document = Jsoup.parse(html)

                        val pageData = SourceParser.parseKiryuu(document)
                        result.addAll(pageData)
                    }
                    result
                }

                sourceType.manhwaindo -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..pages) {
                        val html =
                            client.Ikiru.getHtml("${url}series/?page=$page&order=update")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseManhwaindo(document)

                        result.addAll(pageData)
                    }

                    Log.d(TAG, "getComic: ${result}")

                    result
                }

                sourceType.batoto_all -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..pages) {
                        val html =
                            client.ScrapeDataImage.getHtml("https://bato.si/v4x-latest/$page")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseBatoto(document)

                        result.addAll(pageData)
                    }

                    Log.d(TAG, "getComic: ${result}")

                    result
                }

//                sourceType.doujindesu -> {
//                    val result = mutableListOf<getListUpdate>()
//
//                    for (page in 1..pages) {
//                        val html = client.Ikiru.getHtml("https://doujin.desu.xxx/manhwa?page=${page}")
//                        val document = Jsoup.parse(html)
//                        val pageData = SourceParser.parseDoujindesu(document)
//
//                        result.addAll(pageData)
//                    }
//
//                    result
//                }
                sourceType.doujindesu -> {
                    val result = mutableListOf<getListUpdate>()
                    val limit = 24

                    for (page in 1..pages) {
                        val offset = (page - 1) * limit
                        val json = client.Doujindesu.getHtml(
                            "https://doujin.desu.xxx/api/manga?limit=$limit&type=manhwa&sort=latest_chapter&offset=$offset"
                        )

                        Log.d(TAG, "getComic: ${json}")
                        val pageData = SourceParser.parseDoujindesu(json)

                        result.addAll(pageData)
                    }

                    result
                }

                sourceType.otakudesu -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..10) {
                        val html = client.Ikiru.getHtml("${url}ongoing-anime/page/$page/")
                        val document = Jsoup.parse(html)
                        val pageData = CinemaParser.parswerOtakuDesu(document)

                        result.addAll(pageData)
                    }

                    result
                }

                else -> emptyList() // Default case, kembalikan list kosong
            }
        }

    }

    override suspend fun getSearch(type: sourceType, query: String,url: String): List<getListUpdate> {
        val result = mutableListOf<getListUpdate>()
        return when (type) {
            sourceType.komikcast -> {
                val url = "${url}?s=$query"
                val html = client.Komikcast.getHtml(url)
                val document = Jsoup.parse(html)
                val page = SourceParser.parseKomikcast(document)
                result.addAll(page)
                Log.d(TAG, "getSearch: ${result.size}")
                if (result.isNotEmpty()) {
                    result
                } else {
                    emptyList()
                }
            }

            sourceType.ikiru -> {
                val response = client.Ikiru.getHtml(url)
                val document = Jsoup.parse(response)
                val urlString = document.select("form.items-center").attr("hx-post")
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient()

                    val formBody = FormBody.Builder()
                        .add("query", query)
                        .build()

                    val request = Request.Builder()
                        .url(urlString)
                        .post(formBody) // Tentukan metode POST dengan body yang sudah dibuat
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                        )
                        .build()

                    try {
                        // 4. Eksekusi request dan dapatkan response
                        val response = client.newCall(request).execute()
                        response.use {
                            if (!it.isSuccessful) {
                                Log.e(TAG, "Request Gagal! Kode: ${it.code}")
                                return@withContext emptyList<getListUpdate>()
                            }
                            val htmlString = it.body?.string()

                            if (htmlString != null) {
                                val doc = Jsoup.parse(htmlString)
                                val pageData = SearchParser.parseIkiru(doc)
                                Log.d(TAG, "OkHttp & Jsoup: Ditemukan ${pageData.size} item.")
                                return@withContext pageData
                            } else {
                                Log.w(TAG, "Response body dari OkHttp kosong.")
                                return@withContext emptyList<getListUpdate>()
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Gagal menjalankan OkHttp: ${e.message}")
                        return@withContext emptyList<getListUpdate>()
                    }
                }
            }

            sourceType.shinigami -> {

                val data = SearchParser.parseShinigami(client, 24, query)
                Log.d(TAG, "getComic: ${data?.size}")
                result.addAll(data ?: emptyList())

                result

            }

            sourceType.comicscan -> {
                val url = "${url}?s=$query"
                val html = client.Comicscan.getHtml(url)

                val document = Jsoup.parse(html)
                val pagedata = SourceParser.parseComicscan(document)
                result.addAll(pagedata)

                Log.d(TAG, "getSearch: ${result.size}")

                result
            }

            sourceType.kiryuu -> {
                val url = "${url}?s=$query"
                val html = client.Comicscan.getHtml(url)
                val document = Jsoup.parse(html)
                val page = SourceParser.parseKiryuu(document)
                result.addAll(page)
                Log.d(TAG, "getSearch: ${result.size}")

                result
            }

            sourceType.komiku -> {
                result
            }

            sourceType.manhwaindo -> {
                val url = "${url}?s=$query"
                val html = client.Ikiru.getHtml(url)
                val document = Jsoup.parse(html)

                val dataPaged = SourceParser.parseManhwaindo(document)
                result.addAll(dataPaged)

                result
            }

//            sourceType.doujindesu -> {
//                val url = "${url}?s=$query"
//                val html = client.Ikiru.getHtml(url)
//                val document = Jsoup.parse(html)
//
////                val dataPaged = SourceParser.parseDoujindesu(document)
//                result.addAll(dataPaged)
//
//                result
//            }

            else -> emptyList()
        }
    }

    companion object {
        private const val TAG = "Comic"
    }
}