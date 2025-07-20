package com.komik.komik.service.repository

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositroyComic
import com.komik.komik.service.parser.comic.SearchParser
import com.komik.komik.service.parser.comic.SourceParser
import com.komik.komik.service.parser.model.getListUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class Comic(val client: client) : RepositroyComic {
    override suspend fun getComic(type: sourceType, url: String): List<getListUpdate> {

        return withContext(Dispatchers.IO) {
            when (type) {
                sourceType.komikcast -> {

                    val result = mutableListOf<getListUpdate>()

                    for (page in 2..10) {
                        val html =
                            client.Komikcast.getHtml("https://komikcast.li/daftar-komik/page/$page/?sortby=update")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseKomikcast(document).toList()
                        result.addAll(pageData)

                    }

                    result

                }

                sourceType.ikiru -> {

                    val result = mutableListOf<getListUpdate>()
                    for (page in 1..10) {
                        val html =
                            client.Ikiru.getHtml("https://id.ikiru.wtf/latest-update/?the_page=$page")
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

                    for (page in 1..10) {
                        val html =
                            client.Comicscan.getHtml("https://lc3.cosmicscans.asia/manga/?page=$page&order=update")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseComicscan(document).toList()
                        result.addAll(pageData)
                    }

                    Log.d(TAG, "getComic: ${result.size}")

                    result
                }

                sourceType.kiryuu -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..10) {
                        val html =
                            com.komik.komik.service.client.Ikiru.getHtml("https://kiryuu02.com/manga/?page=$page&order=update")
                        val document = Jsoup.parse(html)

                        val pageData = SourceParser.parseKiryuu(document)
                        result.addAll(pageData)
                    }
                    result
                }

                sourceType.manhwaindo -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..10) {
                        val html =
                            client.Ikiru.getHtml("https://manhwaindo.com/series/?page=$page&order=update")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseManhwaindo(document)

                        result.addAll(pageData)
                    }

                    Log.d(TAG, "getComic: ${result}")

                    result
                }

                sourceType.batoto_all -> {
                    val result = mutableListOf<getListUpdate>()

                    for (page in 1..10) {
                        val html =
                            client.ScrapeDataImage.getHtml("https://bato.si/v4x-latest/$page")
                        val document = Jsoup.parse(html)
                        val pageData = SourceParser.parseBatoto(document)

                        result.addAll(pageData)
                    }

                    result
                }

                else -> emptyList() // Default case, kembalikan list kosong
            }
        }

    }

    override suspend fun getSearch(type: sourceType, query: String): List<getListUpdate> {
        val result = mutableListOf<getListUpdate>()
        return when (type) {
            sourceType.komikcast,
            sourceType.ikiru -> {
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient()

                    val formBody = FormBody.Builder()
                        .add("query", query)
                        .build()

                    val request = Request.Builder()
                        .url("https://id.ikiru.wtf/ajax-call?nonce=91595ea9cd&action=search")
                        .post(formBody) // Tentukan metode POST dengan body yang sudah dibuat
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                        )
                        .build()

                    try {
                        // 4. Eksekusi request dan dapatkan response
                        val response = client.newCall(request).execute()
                        // 'use' akan otomatis menutup response setelah selesai, ini penting!
                        response.use {
                            if (!it.isSuccessful) {
                                Log.e(TAG, "Request Gagal! Kode: ${it.code}")
                                return@withContext emptyList<getListUpdate>()
                            }
                            // 5. Ambil body response sebagai String (HTML mentah)
                            val htmlString = it.body?.string()

                            if (htmlString != null) {
                                // 6. Parsing HTML menggunakan Jsoup
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

            sourceType.shinigami,
            sourceType.comicscan,
            sourceType.kiryuu,
            sourceType.komiku -> {
                result
            }

            else -> emptyList()
        }
    }

    companion object {
        private const val TAG = "Comic"
    }
}