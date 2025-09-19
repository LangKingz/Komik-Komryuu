package com.komik.komik.service.parser.comic

import com.komik.komik.service.client
import com.komik.komik.service.parser.model.getListUpdate
import org.jsoup.nodes.Document


object SearchParser {

    fun parseIkiru(document: Document): List<getListUpdate> {

        return document.select("div#searchResults a")
            .filter {it ->
                it.select("img").isNotEmpty()
            }
            .mapIndexed{index,item ->
            getListUpdate(
                index= index,
                title = item.select("h3").text(),
                location = "ikiru",
                urlImage = item.select("img").attr("src"),
                link = item.attr("href"),
                tagModel = "komik"
            )
        }

    }

    suspend fun parseShinigami(client: client, page: Int, query: String): List<getListUpdate>? {
        val response = client.Shinigami.getSearchMangaList(query = query)
        val pageData = response.data

        return pageData?.mapIndexed { index, item ->
            getListUpdate(
                index = index,
                title = item?.title.toString(),
                urlImage = item?.coverImageUrl.toString(),
                link = item?.mangaId.toString(),
                location = "shinigami",
                tagModel = "komik"

            )
        }
    }

}