package com.komik.komik.service.parser.comic

import com.komik.komik.service.api.model.DataIteme
import com.komik.komik.service.client
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.parser.model.ParsedData
import com.komik.komik.service.utils.parsechapterToInt
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object comicParser {

    suspend fun parseShinigami(id: String, client: client, page: Int): ParsedData {

        val response = client.Shinigami.getMangaDetail(id)
        val data = response.data

        val dataChapter = mutableListOf<DataIteme>()

        for (page in 1..10) {
            val resChapter = client.Shinigami.getChapter(id, pageSize = 40, page = page)
            val chapter = resChapter.data
            dataChapter.add(chapter as DataIteme)
        }

        val title = data?.title
        val imageUrl = data?.coverPortraitUrl
        val description = data?.description
        val author = data?.taxonomy?.author

        val alternativeTitle = data?.alternativeTitle
        val rating = data?.userRate
        val type = "Manhwa"
        val status = data?.status

        val genre = data?.taxonomy?.genre
        val genres = genre?.mapIndexed { index, genre ->
            Genre(
                comicTitle = title.toString(),
                genre = genre?.name.toString(),
                location = "shinigami"
            )
        }

        val chapters = dataChapter.map {
            Chapter(
                comicTitle = title.toString(),
                title = "chapter ${it.chapterNumber}",
                location = "shinigami",
                link = it.chapterId.toString(),
                date = it.releaseDate.toString()
            )
        }

        return ParsedData(
            url = id,
            title = title.toString(),
            imageUrl = imageUrl.toString(),
            description = description.toString(),
            author = author.toString(),
            location = "shinigami",
            chapters = chapters ?: emptyList(),
            genres = genres ?: emptyList(),
            alternativeTitle = alternativeTitle,
            type = type,
            rating = rating.toString(),
            status = status.toString()
        )
    }


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
                numChapter = it.attr("data-num").toDouble(),
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

    fun parseKomikcast(document: Document, url: String): ParsedData {
        val title = document.select(".komik_info-content-body-title")[0].text()
        val imageUrl = document.select(".komik_info-content-thumbnail img").attr("src")
        val deskripsitable = document.select(".komik_info-description-sinopsis p").text()
        val author = document.select("span.komik_info-content-info").text()

        val alternativeTitle = document.select("span.komik_info-content-native")
        val rating = document.select("div.data-rating").attr("data-ratingkomik")

        val chapters = document.select("ul li.komik_info-chapters-item ").map {
            val chapter = it.select("a.chapter-link-item").text()
            Chapter(
                title = chapter,
                link = it.select("a.chapter-link-item").attr("href"),
                comicTitle = title,
                numChapter = parsechapterToInt(chapter),
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

        val idparser =
            client.Ikiru.getHtml("https://id.ikiru.wtf/ajax-call?manga_id=${mangaId}&page=1&action=chapter_list")
        val document2 = Jsoup.parse(idparser)

        val title = document.select(".flex h1.text-2xl")[0].text()
        val imageUrl = document.select(".contents img").attr("src")
        val deskripsitable = document.select("div.p-4").attr("itemprop", "description").text()
        val author = document.select("span.komik_info-content-info").text()

        val rating = document.select("span.to-accent-2").text()
        val type = document.select("p.ml-1").text()

        val alternative = document.select("div.text-text").text()


        val chapters =
            document2.select("div#chapter-list div.cursor-pointer").map {
                Chapter(
                    title = it.select("span").text(),
                    link = it.select("a").attr("href"),
                    comicTitle = title,
                    numChapter = it.attr("data-chapter-number").toDouble(),
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
        val status = document.select(".tsinfo .imptdt")[0].select("a").text() ?: ""


        val chapters = document.select("ul.clstyle li ").map {
            Chapter(
                title = it.select(".eph-num a span")[0].text(),
                link = it.select(".eph-num a").attr("href"),
                comicTitle = title,
                numChapter = it.attr("data-num").toDouble(),
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
            author = author,
            location = "comicscan",
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle,
            rating = rating,
            type = type,
            status = status
        )
    }

    fun parseDoujinDesu(document: Document, url: String): ParsedData {
        val title = document.select("h1.title").text() ?: ""
        val imageUrl = document.select("a.permalink img").attr("src") ?: ""
        val description = document.select(".metadata .pb-2 p").text() ?: ""
        val author = document.select("tr.pages td").select("i").text() ?: ""

        val alternativeTitle = document.select(".alter i").text()
        val rating = document.select(".rating-prc").text()
        val type = document.select(".magazines td")[1].select(" a").text() ?: "Manhwa"
        val status = document.select(".metadata tr")[0].select("td")[1].text() ?: "onGoing"

        val BASE_URL = "https://doujindesu.tv"

        val chapters = document.select(".bxcl ul li").map {
            val chapter = it.select(".epsright a").attr("title")
            Chapter(
                title = it.select("span.lchx a").text(),
                link = BASE_URL + it.select("a").attr("href"),
                comicTitle = title,
                numChapter = parsechapterToInt(chapter),
                date = it.select("span.date").text(),
                location = "dd"
            )
        }

        val genres = document.select(".tags a").map {
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
            location = "dd",
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle,
            rating = rating,
            type = type,
            status = status
        )
    }

    fun parsemanhwaIndo(document: Document, url: String): ParsedData {
        val title = document.select("h1.entry-title").text() ?: ""
        val imageUrl = document.select(".thumb  img").attr("src") ?: ""
        val description = document.select(".wd-full .entry-content p").text() ?: ""
        val author = document.select(".tsinfo .imptdt")[3].select("i").text() ?: ""

        val alternativeTitle = "#"
        val rating = document.select(".num").text()
        val type = document.select(".tsinfo .imptdt")[1].select(" a").text() ?: "Manhwa"
        val status = document.select(".tsinfo .imptdt")[0].select("a").text() ?: "onGoing"


        val chapters = document.select("ul li").map {
            Chapter(
                title = it.select(".eph-num a span")[0].text(),
                link = it.select(".eph-num a").attr("href"),
                comicTitle = title,
                numChapter = it.attr("data-num").toDouble(),
                date = it.select(".eph-num a span")[1].text(),
                location = "manhwaindo"
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
            author = author,
            location = "manhwaindo",
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle,
            rating = rating,
            type = type,
            status = status
        )
    }

}