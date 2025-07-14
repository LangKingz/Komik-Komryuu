package com.komik.komik.service.parser.comic

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
                    location = "comicscan"
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
                    location = "kiryuu"
                )
            }
    }

    suspend fun parseShinigami(client:client,page:Int): List<getListUpdate>? {
        val response = client.Shinigami.getMangaList(pageSize = 24, page = page, sortBy = "latest")
        val pageData = response.data

        return pageData?.mapIndexed{index,item->
            getListUpdate(
                index = index,
                title = item?.title.toString(),
                urlImage = item?.coverImageUrl.toString(),
                link = item?.mangaId.toString(),
                location = "shinigami"

            )
        }
    }


    fun parseIkiru(document: Document): List<getListUpdate> {
        return document.select(".grid div .hidden").filter { it ->
            it.select("a.relative").isNotEmpty()
        }.mapIndexed { index, it ->
            getListUpdate(
                index = index,
                title = it.select(".flex-col a.line-clamp-1").text(),
                urlImage = it.select("a.relative img").attr("src")
                    ?: "https://cosmic345.co/img/noimage.png",
                link = it.select("a.relative").attr("href"),
                location = "ikiru"
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
                    location = "komikcast"
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
                location = "komiku"
            )
        }
    }


}