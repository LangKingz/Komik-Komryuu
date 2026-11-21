package com.komik.komik.service.parser.comic

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.parser.model.getImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object ImageParser {
    fun ikiruParse(document: Document): List<getImages> {
        return document.select("main img")
            .map { element ->
                getImages(
                    index = 0,
                    image = element.attr("src"),
                )
            }
    }

    fun komikcastParse(document: Document): List<getImages> {
        return document.select("div.chapter_body div.main-reading-area img")
            .mapIndexed { index, element ->
                getImages(
                    index = index,
                    image = element.attr("src"),
                )
            }
    }

    fun comicscanParse(document: Document): List<getImages> {
        return document.select("div#readerarea img")
            .filterNot { image ->
                val src = image.attr("src")
                src == "https://cosmic345.co/wp-content/uploads/2024/09/800x250-1-RW4D.webp" ||
                        src == "https://cosmic345.co/wp-content/uploads/2024/11/NEW_BEAT_800X250.webp"
            }
            .mapIndexed { index, it ->
                getImages(
                    index = index,
                    it.attr("src")
                )
            }
    }

    fun kiryuuParse(document: Document): List<getImages> {
        return document.select("div#readerarea img")
            .mapIndexed { index, it ->
                getImages(
                    index = index,
                    it.attr("src")
                )
            }
    }

    fun manhwaIndoParse(document: Document): List<getImages> {
        return document.select(".maincontent div#readerarea img").mapIndexed { index, element ->
            getImages(
                index = index,
                image = element.attr("src"),
            )
        }
    }

    fun batotoParse(document: Document): List<getImages> {
        return document.select(".cursor-pointer img").mapIndexed { index, element ->
            getImages(
                index = index,
                image = element.attr("src"),
            )
        }
    }

    suspend fun doujindesuParseAsync( url: String,jsonString: String): List<getImages> {
        val imagesList = mutableListOf<getImages>()

        try {
            val json = JSONObject(jsonString)
            val contentUrls = json.optJSONArray("content_urls")

            if (contentUrls != null) {
                for (i in 0 until contentUrls.length()) {
                    val imageUrl = contentUrls.optString(i, "")
                    if (imageUrl.isNotEmpty()) {
                        imagesList.add(
                            getImages(
                                index = i, // atau i + 1 kalau AdapterImage kamu mulai halaman dari angka 1
                                image = imageUrl
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImageParser", "Error parsing Doujindesu images: ${e.localizedMessage}")
        }

        return imagesList

        // Pindahkan operasi jaringan ke IO Dispatcher (background thread khusus I/O)
//        return withContext(Dispatchers.IO) { // Ini yang penting!
//            val client = OkHttpClient()
//            val formBuilder = FormBody.Builder()
//                .add("id", url)
//                .build()
//
//            val request = Request.Builder()
//                .url("https://doujindesu.tv/themes/ajax/ch.php")
//                .post(formBuilder)
//                .header(
//                    "Referer", url
//                )
//                .header(
//                    "X-Requested-With", "XMLHttpRequest"
//                )
//                .header(
//                    "Accept", "image/avif,image/webp,*/*"
//                ).build()
//
//            val result = client.newCall(request).execute() // <-- Sekarang ini berjalan di background thread
//
//            val body = result.body?.string()
//
//            emptyList() // Kembalikan list kosong jika body null
//        }
    }

    suspend fun parseShinigami(client : client,id:String):List<getImages>{
        val response = client.Shinigami.getChapterDetail(id)
        val data = response.data

        val baseurl = data?.baseUrl
        val chapterPath = data?.chapter?.path
        val photo = data?.chapter?.data

        return photo?.mapIndexed { index, s ->
            getImages(
                index = index,
                image = "$baseurl$chapterPath$s"
            )
        }!!

    }
}