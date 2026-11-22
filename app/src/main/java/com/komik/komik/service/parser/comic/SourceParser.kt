package com.komik.komik.service.parser.comic

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.parser.model.getListUpdate
import org.jsoup.nodes.Document

object SourceParser {

     fun parseComicscan(document: Document): List<getListUpdate> {
        return document.select(".bs .bsx")
            .mapIndexed { index, it ->
                getListUpdate(
                    index = index,
                    title = it.select(".bigor .tt").text(),
                    urlImage = it.select(".limit img").attr("src")
                        ?: "https://cosmic345.co/img/noimage.png",
                    link = it.select("a").attr("href"),
                    location = "comicscan",
                    tagModel = "komik"
                )
            }
    }

    fun parseKiryuu(document: Document): List<getListUpdate> {
        return document.select(".bsx")
            .mapIndexed { index, it ->
                getListUpdate(
                    index = index,
                    title = it.select("a").attr("title"),
                    urlImage = it.select(".limit img").attr("src"),
                    link = it.select("a").attr("href"),
                    location = "kiryuu",
                    tagModel = "komik"
                )
            }
    }

//    https://api.shinigami.io/v1/manga/list?type=project&is_update=true&sort=latest&sort_order=desc&offset=20&limit=20

    suspend fun parseShinigami(client: client, limit: Int = 24, offset: Int = 0): List<getListUpdate>? {
        val response = client.Shinigami.getMangaList(
            pageSize = limit,
            limit = limit,
            offset = offset,
            sortBy = "latest"
        )
        val pageData = response.data

        return pageData?.mapIndexed { index, item ->
            getListUpdate(
                index = offset + index,
                title = item?.title.toString(),
                urlImage = item?.coverImageUrl.toString(),
                link = item?.mangaId.toString(),
                location = "shinigami",
                tagModel = "komik"
            )
        }
    }


    suspend fun parseIkiru(client: client, page: Int): List<getListUpdate>? {
        val response = client.ikiruService.getProjectManga(
            page = page,
            sortBy = "popular",
            sort = "desc"
        )

        Log.d("SourcePArser", "parseIkiru: ${response}")

        val pageData = response.data?.mangas
        return pageData?.mapIndexed { index, item ->
            getListUpdate(
                index = index,
                title = item?.title.toString(),
                urlImage = item?.featuredImage.toString(),
                link = item?.slug.toString(),
                location = "ikiru",
                tagModel = "komik"
            )
        }
    }

    fun parseKomikcast(document: Document): List<getListUpdate> {
        return document.select(".list-update_item")
            .mapIndexed { index, it ->
                val link = it.select("a").attr("href")
                getListUpdate(
                    index = index,
                    title = it.select(".list-update_item-info h3").text(),
                    urlImage = it.select(".list-update_item-image img").attr("src"),
                    link = link,
                    location = "komikcast",
                    tagModel = "komik"
                )
            }
    }

    fun parseKomikindo(document: Document): List<getListUpdate> {
        return document.select(".ls2").mapIndexed { index, it ->
            val baseurl = "https://komikindo.rip/"
            val link = baseurl + it.select(".ls2v a").attr("href")
            getListUpdate(
                index = index,
                title = it.select(".ls2j h3").text(),
                urlImage = it.select(".ls2v a img").attr("data-src") ?: "",
                link = link,
                location = "komiku",
                tagModel = "komik"
            )
        }
    }

    fun parseManhwaindo(document: Document): List<getListUpdate> {
        return document.select(".bs .bsx")
            .mapIndexed { index, it ->
                getListUpdate(
                    index = index,
                    title = it.select(".bigor .tt").text(),
                    urlImage = it.select(".limit img").attr("src")
                        ?: "https://cosmic345.co/img/noimage.png",
                    link = it.select("a").attr("href"),
                    location = "manhwaindo",
                    tagModel = "komik"
                )
            }
    }

    fun parseBatoto(document: Document): List<getListUpdate> {
        val baseUrl = "https://bato.si"
        return document.select(".grid .flex").attr("q:key", "wP_7")
            .filter { it ->
                it.select("h3 a.link-hover span").isNotEmpty() && it
                    .select(".shrink-0 .group img.rounded").isNotEmpty()
            }
            .mapIndexed { index, element ->
                getListUpdate(
                    index = index,
                    title = element.select("h3 a.link-hover span").text(),
                    urlImage = baseUrl + element.select(".shrink-0 .group img.rounded").attr("src"),
                    link = baseUrl + element.select(".group a").attr("href"),
                    location = "batoto-all",
                    tagModel = "komik"
                )
            }
    }

//    fun parseDoujindesu(document: Document) : List<getListUpdate>{
//        val baseurl = "https://doujin.desu.xxx/"
//
//        return document.select(".entries article").mapIndexed { index, element ->
//            getListUpdate(
//                index = index,
//                title = element.select(".entry a").attr("title"),
//                urlImage = element.select(".thumbnail img").attr("src"),
//                link =baseurl+ element.select(".entry a").attr("href"),
//                location = "dd",
//                tagModel = "komik"
//                )
//        }
//    }

    fun parseDoujindesu(jsonString: String): List<getListUpdate> {
        val baseurl = "https://doujin.desu.xxx/"
        val list = mutableListOf<getListUpdate>()

        try {
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val slug = item.optString("slug", "")
                val title = item.optString("title", "")
                val coverUrl = item.optString("cover_url", "")

                list.add(
                    getListUpdate(
                        index = i,
                        title = title,
                        urlImage = coverUrl,
                        link = "${baseurl}api/manga/$slug",
                        location = "doujindesu",
                        tagModel = "komik"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }






}