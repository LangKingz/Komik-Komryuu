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
import kotlin.math.log

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
                        try {
                            val response = client.ikiruService.getProjectManga(
                                page = page,
                                sortBy = "popular",
                                sort = "desc"
                            )
                            val mangas = response.data?.mangas?.mapIndexed { index, item ->
                                getListUpdate(
                                    index = index,
                                    title = item?.title ?: "Manga",
                                    urlImage = formatIkiruCover(item?.featuredImage),
                                    link = item?.slug ?: item?.id.orEmpty(),
                                    location = "ikiru",
                                    tagModel = "komik"
                                )
                            }
                            if (!mangas.isNullOrEmpty()) {
                                result.addAll(mangas)
                            } else {
                                val projectResponse = client.ikiruService.getProjects(page = page)
                                val projects = projectResponse.data?.project?.mapIndexed { index, item ->
                                    getListUpdate(
                                        index = index,
                                        title = item?.title ?: "Project Manga",
                                        urlImage = formatIkiruCover(item?.featuredImage),
                                        link = item?.slug ?: item?.id.orEmpty(),
                                        location = "ikiru",
                                        tagModel = "komik"
                                    )
                                }
                                if (!projects.isNullOrEmpty()) {
                                    result.addAll(projects)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "getComic ikiru error page $page: ${e.message}")
                            try {
                                val fallback = SourceParser.parseIkiru(client, page)?.toList()
                                if (!fallback.isNullOrEmpty()) {
                                    result.addAll(fallback)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    result
                }

                sourceType.komiku,
                sourceType.shinigami -> {
                    val offset = if (pages > 1) (pages - 1) * 24 else 0
                    SourceParser.parseShinigami(client, limit = 24, offset = offset) ?: emptyList()
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

    override suspend fun getComicPaged(
        type: sourceType,
        url: String,
        limit: Int,
        offset: Int
    ): List<getListUpdate> {
        return withContext(Dispatchers.IO) {
            when (type) {
                sourceType.komiku,
                sourceType.shinigami -> {
                    SourceParser.parseShinigami(client, limit = limit, offset = offset) ?: emptyList()
                }

                sourceType.doujindesu -> {
                    try {
                        val response = client.DoujinDesuServiceDyricpted.getMangaList(limit,
                            offset = offset
                        )
                        Log.d(TAG, "getComicPaged: $response")

                        val json = client.Doujindesu.getHtml("https://doujin.desu.xxx/api/manga?limit=${limit}&type=manhwa&sort=latest_chapter&offset=${offset}")

                        SourceParser.parseDoujindesu(json)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d(TAG, "getComicPaged: ${e.localizedMessage}")
                        emptyList()
                    }
                }

                sourceType.komikcast -> {
                    val page = (offset / limit) + 1
                    val targetPage = if (page < 2) 2 else page
                    try {
                        val html = client.Komikcast.getHtml("${url}daftar-komik/page/$targetPage/?sortby=update")
                        val document = Jsoup.parse(html)
                        SourceParser.parseKomikcast(document).toList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }

                sourceType.ikiru -> {
                    val page = (offset / limit) + 1
                    try {
                        SourceParser.parseIkiru(client,page)?.toList() ?: emptyList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        SourceParser.parseIkiru(client, page)?.toList() ?: emptyList()
                    }
                }

                sourceType.comicscan -> {
                    val page = (offset / limit) + 1
                    try {
                        val html = client.Comicscan.getHtml("${url}manga/?page=$page&order=update")
                        val document = Jsoup.parse(html)
                        SourceParser.parseComicscan(document).toList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }

                sourceType.kiryuu -> {
                    val page = (offset / limit) + 1
                    try {
                        val html = com.komik.komik.service.client.Ikiru.getHtml("${url}manga/?page=$page&order=update")
                        val document = Jsoup.parse(html)
                        SourceParser.parseKiryuu(document)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }

                sourceType.manhwaindo -> {
                    val page = (offset / limit) + 1
                    try {
                        val html = client.Ikiru.getHtml("${url}series/?page=$page&order=update")
                        val document = Jsoup.parse(html)
                        SourceParser.parseManhwaindo(document)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }

                sourceType.batoto_all -> {
                    val page = (offset / limit) + 1
                    try {
                        val html = client.ScrapeDataImage.getHtml("https://bato.si/v4x-latest/$page")
                        val document = Jsoup.parse(html)
                        SourceParser.parseBatoto(document)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }

                sourceType.otakudesu -> {
                    val page = (offset / limit) + 1
                    try {
                        val html = client.Ikiru.getHtml("${url}ongoing-anime/page/$page/")
                        val document = Jsoup.parse(html)
                        CinemaParser.parswerOtakuDesu(document)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }

                else -> emptyList()
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
                try {
                    val response = client.ikiruService.searchManga(
                        query = query,
                        page = 1,
                        sortBy = "popular",
                        sort = "desc"
                    )
                    val searchData = response.data?.mangas?.mapIndexed { index, item ->
                        getListUpdate(
                            index = index,
                            title = item?.title ?: "Manga",
                            urlImage = formatIkiruCover(item?.featuredImage),
                            link = item?.slug ?: item?.id.orEmpty(),
                            location = "ikiru",
                            tagModel = "komik"
                        )
                    }
                    if (!searchData.isNullOrEmpty()) {
                        result.addAll(searchData)
                        result
                    } else {
                        val legacyResult = runLegacyIkiruSearch(query, url)
                        result.addAll(legacyResult)
                        result
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getSearch ikiru API error: ${e.message}, fallback to legacy")
                    val legacyResult = runLegacyIkiruSearch(query, url)
                    result.addAll(legacyResult)
                    result
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

    private fun formatIkiruCover(rawPath: String?): String {
        if (rawPath.isNullOrBlank()) return ""
        return if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
            rawPath
        } else {
            "${client.getIkiruUrl().trimEnd('/')}/${rawPath.trimStart('/')}"
        }
    }

    private suspend fun runLegacyIkiruSearch(query: String, url: String): List<getListUpdate> {
        return withContext(Dispatchers.IO) {
            try {
                val searchUrl = if (url.startsWith("http")) url else "${client.getIkiruUrl().trimEnd('/')}/daftar"
                val response = client.Ikiru.getHtml(searchUrl)
                val document = Jsoup.parse(response)
                val urlString = document.select("form.items-center").attr("hx-post")
                if (urlString.isBlank()) return@withContext emptyList<getListUpdate>()

                val okClient = OkHttpClient()
                val formBody = FormBody.Builder()
                    .add("query", query)
                    .build()

                val request = Request.Builder()
                    .url(urlString)
                    .post(formBody)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                    )
                    .build()

                val callResponse = okClient.newCall(request).execute()
                callResponse.use {
                    if (!it.isSuccessful) return@withContext emptyList<getListUpdate>()
                    val htmlString = it.body?.string() ?: return@withContext emptyList<getListUpdate>()
                    val doc = Jsoup.parse(htmlString)
                    SearchParser.parseIkiru(doc)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menjalankan legacy OkHttp search: ${e.message}")
                emptyList()
            }
        }
    }

    companion object {
        private const val TAG = "Comic"
    }
}