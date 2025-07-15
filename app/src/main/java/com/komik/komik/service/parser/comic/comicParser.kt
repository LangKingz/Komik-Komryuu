package com.komik.komik.service.parser.comic

import com.komik.komik.service.client
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.parser.model.ParsedData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object comicParser {
    fun parseKiryuu(document: Document, url: String): ParsedData {
        val title = document.select("h1.entry-title").text()
        val imageUrl = document.select(".thumb img").attr("src")
        val description = document.select(".seriestuhead p").text()
        val author = document.select("table tr")[4].select("td")[1].text()

        val alternativeTitle = document.select("div.seriestualt").text()
        val rating = document.select("div.num").text()
        val type = document.select("table tr")[1].select("td")[1].text()
        val status = document.select("table tr")[0].select("td")[1].text()

        val chapters = document.select("ul.clstyle li").map {
            Chapter(
                title = it.select("a .chapternum").text(),
                link = it.select("a").attr("href"),
                comicTitle = title,
                date = it.select("a .chapterdate").text(),
                location = "kiryuu"
            )
        }

        val genres = document.select(".seriestugenre a").map {
            Genre(
                comicTitle = title,
                genre = it.text()
            )
        }

        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = description,
            author = author,
            location = "kiryuu",
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle,
            type = type,
            rating = rating,
            status = status
        )
    }

    fun parseKomikcast(document: Document,url:String):ParsedData{
        val title = document.select(".komik_info-content-body-title")[0].text()
        val imageUrl = document.select(".komik_info-content-thumbnail img").attr("src")
        val deskripsitable = document.select(".komik_info-description-sinopsis p").text()
        val author = document.select("span.komik_info-content-info").text()

        val alternativeTitle= document.select("span.komik_info-content-native")
        val rating = document.select("div.data-rating").attr("data-ratingkomik")

        val chapters = document.select("ul li.komik_info-chapters-item ").map {
            Chapter(
                title = it.select("a.chapter-link-item").text(),
                link = it.select("a.chapter-link-item").attr("href"),
                comicTitle = title,
                date = it.select(".chapter-link-time").text(),
                location = "komikcast"
            )
        }


        val genres = document.select("span.komik_info-content-genre a").map {
            Genre(
                comicTitle = title,
                genre = it.text()
            )
        }

        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = deskripsitable,
            author = author,
            location = "komikcast",
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle.toString(),
            type = "Manhwa",
            rating = rating,
            status = "onGoing"
        )
    }


    suspend fun parseIkiru(document: Document, url: String): ParsedData {
        val bodyClasses = document.body().className().split(" ")
        val mangaId =
            bodyClasses.firstOrNull { it.startsWith("postid-") }?.removePrefix("postid-")

        val idparser = client.Ikiru.getHtml("https://id.ikiru.wtf/ajax-call?manga_id=${mangaId}&page=1&action=chapter_list")
        val document2 = Jsoup.parse(idparser)

        val title = document.select(".flex h1.text-2xl")[0].text()
        val imageUrl = document.select(".contents img").attr("src")
        val deskripsitable = document.select("div").attr("itemprop", "description").text()
        val author = document.select("span.komik_info-content-info").text()

        val rating = document.select("span.to-accent-2").text()
        val type = document.select("p.ml-1").text()

        val alternative = document.select("div.text-text").text()


        val chapters =
            document2.select("div#chapter-list div.cursor-pointer a.cursor-pointer").map {
                Chapter(
                    title = it.select("span").text(),
                    link = it.attr("href"),
                    comicTitle = title,
                    date = it.select("time").text(),
                    location = "ikiru"
                )
            }


        val genres = document.select("span.text-inherit")
            .map {
                Genre(
                    comicTitle = title,
                    genre = it.text()
                )
            }

        return ParsedData(
            url = url,
            title = title,
            alternativeTitle = alternative,
            imageUrl = imageUrl,
            description = deskripsitable,
            rating = rating,
            type = type,
            author = author,
            location = "ikiru",
            chapters = chapters,
            genres = genres
        )
    }

    fun parseKomiku(document: Document, url: String): ParsedData {
        val baseurl = "https://komiku.id"
        val title = document.select(" h1 span").text()
        val imageUrl = document.select(".ims img").attr("src")
        val description = document.select("p.desc").text()
        val tableprofil = document.select("table tr")
        val author = tableprofil[4].select("td")[1].text()

        val chapters = document.select("table#Daftar_Chapter tr").map {
            val linkChapter = baseurl + it.select("td.judulseries a").attr("href")
            Chapter(
                title = it.select("td.judulseries span").text(),
                link = linkChapter,
                comicTitle = title,
                date = it.select("td.tanggalseries").text(),
                location = "komiku"
            )
        }

        val genres = document.select("li.genre span").map {
            Genre(
                comicTitle = title,
                genre = it.text()
            )
        }

        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = description,
            author = author,
            location = "komiku",
            chapters = chapters,
            genres = genres
        )
    }

    fun parseComictoon(document: Document, url: String): ParsedData {
        val title = document.select(".bigcontent .infox h1").text() ?: ""
        val imageUrl = document.select(".bigcontent .thumbook .thumb  img").attr("src") ?: ""
        val description = document.select(".bigcontent .infox .entry-content p").text() ?: ""
        val author = document.select(".bigcontent .infox .flex-wrap span")[1].text() ?: ""

        val alternativeTitle = "#"
        val rating = document.select(".num").text()
        val type = document.select(".tsinfo .imptdt")[1].select(" a").text() ?: ""
        val status = document.select(".tsinfo .imptdt")[0].select("a").text() ?:""



        val chapters = document.select("ul li .chbox").map {
            Chapter(
                title = it.select(".eph-num a span")[0].text(),
                link = it.select(".eph-num a").attr("href"),
                comicTitle = title,
                date = it.select(".eph-num a span")[1].text(),
                location = "comicscan"
            )
        }

        val genres = document.select(".wd-full .mgen a").map {
            Genre(
                comicTitle = title,
                genre = it.text()
            )
        }

        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = description,
            author =author,
            location = "comicscan",
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle,
            rating = rating,
            type = type,
            status = status
        )
    }
}