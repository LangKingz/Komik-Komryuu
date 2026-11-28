package com.komik.komik.service.parser.comic

import android.R
import android.content.Context
import android.util.Log
import com.komik.komik.service.api.model.DataIteme
import com.komik.komik.service.client
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.parser.model.ParsedData
import com.komik.komik.service.utils.downloadAndSaveImage
import com.komik.komik.service.utils.extractNumberFromDataNum
import com.komik.komik.service.utils.formatChapterNumber
import com.komik.komik.service.utils.formatWaktuLalu
import com.komik.komik.service.utils.genereteId
import com.komik.komik.service.utils.parseDate
import com.komik.komik.service.utils.parsechapterToInt
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object comicParser {

    suspend fun parseShinigami(id: String, client: client, context: Context): ParsedData {

        val response = client.Shinigami.getMangaDetail(id)
        val data = response.data

        val resChapter = client.Shinigami.getChapter(id, pageSize = 3000)
        val chapter = resChapter.data

        val title = data?.title
        val imageUrl = data?.coverImageUrl
        val description = data?.description
        val author = data?.taxonomy?.author

        val alternativeTitle = data?.alternativeTitle
        val rating = data?.userRate
        val type = "Manhwa"
        val status = data?.status

        val statusManhwa = when (status) {
            0 -> "Complete"
            1 -> "onGoing"
            else -> "onGoing"
        }

        val genre = data?.taxonomy?.genre
        val genres = genre?.mapIndexed { index, genre ->
            Genre(
                comicTitle = title.toString(),
                genre = genre?.name.toString(),
                location = "shinigami"
            )
        }

        val chapters = chapter?.map {
            val num = it?.chapterNumber ?: 0.0
            val chapterString = "Chapter ${num.formatChapterNumber()}"
            val date = it?.releaseDate
            Chapter(
                comicTitle = title.toString(),
                title = chapterString,
                location = "shinigami",
                numChapter = num,
                link = it?.chapterId.toString(),
                date = parseDate(date.toString()),
                comicId = 0
            )
        }

        val imageLokal = downloadAndSaveImage(context, imageUrl.toString(), genereteId().toLong())

        return ParsedData(
            url = id,
            title = title.toString(),
            localImageUri = imageLokal,
            imageUrl = imageUrl.toString(),
            description = description.toString(),
            author = author.toString(),
            location = "shinigami",
            chapters = chapters ?: emptyList(),
            genres = genres ?: emptyList(),
            alternativeTitle = alternativeTitle,
            type = type,
            rating = rating.toString(),
            status = statusManhwa
        )
    }


    suspend fun parseKiryuu(document: Document, url: String, context: Context): ParsedData {
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
                location = "kiryuu",
                comicId = 0

            )
        }

        val genres = document.select(".seriestugenre a").map {
            Genre(
                comicTitle = title,
                genre = it.text()
            )
        }

        val localImage = downloadAndSaveImage(context, imageUrl, genereteId().toLong())

        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = description,
            author = author,
            location = "kiryuu",
            localImageUri = localImage,
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle,
            type = type,
            rating = rating,
            status = status
        )
    }

    suspend fun parseKomikcast(document: Document, url: String, context: Context): ParsedData {
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
                location = "komikcast",
                comicId = 0

            )
        }


        val genres = document.select("span.komik_info-content-genre a").map {
            Genre(
                comicTitle = title,
                genre = it.text()
            )
        }

        val localImage = downloadAndSaveImage(context, imageUrl, genereteId().toLong())


        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = deskripsitable,
            author = author,
            location = "komikcast",
            localImageUri = localImage,
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle.toString(),
            type = "Manhwa",
            rating = rating,
            status = "onGoing"
        )
    }


    fun extractIkiruMangaSlug(rawUrl: String): String {
        var s = rawUrl.trim().trimEnd('/')
        if (s.contains("://")) {
            s = s.substringAfter("://").substringAfter('/')
        }
        if (s.contains("manga/")) {
            s = s.substringAfterLast("manga/").trim('/')
        }
        if (s.contains('?')) s = s.substringBefore('?')
        if (s.contains('#')) s = s.substringBefore('#')
        if (s.contains('/')) s = s.substringBefore('/')
        return s.trim('/')
    }

    suspend fun parseIkiru(context: Context, url: String): ParsedData {
        val slug = extractIkiruMangaSlug(url)
        Log.d("IKIRU_TRACE", "parseIkiru: rawUrl='$url' -> extracted slug='$slug'")

        val response = try {
            val resp = client.ikiruService.getDetailManga(slug)
            Log.d("IKIRU_TRACE", "parseIkiru getDetailManga success: title='${resp.data?.title}', rawChaptersCount=${resp.data?.chapters?.chapters?.size ?: 0}")
            resp
        } catch (e: Exception) {
            Log.e("IKIRU_TRACE", "parseIkiru getDetailManga error for slug='$slug': ${e.message}", e)
            throw e
        }

        val data = response.data

        val title = data?.title?.takeIf { it.isNotBlank() } ?: "Komik Ikiru"
        val rawImage = data?.featuredImage.orEmpty()
        val imageUrl = if (rawImage.startsWith("http://") || rawImage.startsWith("https://")) {
            rawImage
        } else if (rawImage.isNotBlank()) {
            "${client.getIkiruUrl().trimEnd('/')}/${rawImage.trimStart('/')}"
        } else {
            ""
        }

        val localImageUri = if (imageUrl.isNotBlank()) {
            try {
                downloadAndSaveImage(context, imageUrl, System.currentTimeMillis())
            } catch (_: Exception) {
                null
            }
        } else null

        val description = data?.description.orEmpty()
        val authorList = data?.metadata?.author?.mapNotNull { it?.name }?.filter { it.isNotBlank() }
        val artistList = data?.metadata?.artist?.mapNotNull { it?.name }?.filter { it.isNotBlank() }
        val author = when {
            !authorList.isNullOrEmpty() -> authorList.joinToString(", ")
            !artistList.isNullOrEmpty() -> artistList.joinToString(", ")
            else -> "Author Tidak Diketahui"
        }

        val altTitle = data?.metadata?.alternateTitles?.filterNotNull()?.filter { it.isNotBlank() }?.joinToString(", ")
            ?: data?.searchKeywords.orEmpty()

        val typeManga = data?.type ?: "Manga"
        val statusManga = when (data?.status?.lowercase()) {
            "completed", "tamat", "complete" -> "Complete"
            else -> "onGoing"
        }

        val rating = data?.metadata?.score?.toString() ?: "0.0"

        val genres = data?.metadata?.genre?.mapNotNull { gItem ->
            val gName = gItem?.name.orEmpty()
            if (gName.isNotBlank()) {
                com.komik.komik.service.data.model.Genre(
                    comicTitle = title,
                    genre = gName,
                    location = "ikiru"
                )
            } else null
        } ?: emptyList()

        val rawChapters = data?.chapters?.chapters.orEmpty().filterNotNull()
        val chapters = rawChapters.map { chItem ->
            val numChapter = chItem.number ?: 0.0
            val formattedNum = numChapter.formatChapterNumber()
            val chTitle = chItem.title?.takeIf { it.isNotBlank() } ?: "Chapter $numChapter"
//            val chSlug = if (!chItem.slug.isNullOrBlank()) chItem.slug else "chapter-$formattedNum"
            val chLink = "$slug/${numChapter.toInt()}"
            val chDate = chItem.createdAt.orEmpty()

            com.komik.komik.service.data.model.Chapter(
                comicTitle = title,
                title = chTitle,
                location = "ikiru",
                numChapter = numChapter,
                link = chLink,
                date = chDate,
                comicId = 0
            )
        }

        Log.d("IKIRU_TRACE", "parseIkiru: parsed ${chapters.size} chapters (sample first 3: ${chapters.take(3).map { "${it.title} -> link='${it.link}' (num=${it.numChapter})" }})")

        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            localImageUri = localImageUri,
            description = description,
            author = author,
            location = "ikiru",
            chapters = chapters,
            genres = genres,
            alternativeTitle = altTitle,
            type = typeManga,
            rating = rating,
            status = statusManga
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
                location = "komiku",
                comicId = 0
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

    suspend fun parseComictoon(document: Document, url: String, context: Context): ParsedData {
        val title = document.select(".bigcontent .infox h1").text() ?: ""
        val imageUrl = document.select(".bigcontent .thumbook .thumb  img").attr("src") ?: ""
        val description = document.select(".bigcontent .infox .entry-content p").text() ?: ""
        val author = document.select(".bigcontent .infox .flex-wrap span")[1].text() ?: ""

        val alternativeTitle = "ALternative title Tidak Tersedia"
        val rating = document.select(".num").text()
        val type = document.select(".tsinfo .imptdt")[1].select(" a").text() ?: ""
        val status = document.select(".tsinfo .imptdt")[0].select("i").text() ?: ""


        val chapters = document.select("ul.clstyle li ")
            .map {
                val num = it.attr("data-num")
                Chapter(
                    title = it.select(".eph-num a span")[0].text(),
                    link = it.select(".eph-num a").attr("href"),
                    comicTitle = title,
                    numChapter = extractNumberFromDataNum(num),
                    date = it.select(".eph-num a span")[1].text(),
                    location = "comicscan",
                    comicId = 0
                )
            }.distinctBy { it.numChapter }

        val genres = document.select(".wd-full .mgen a").map {
            Genre(
                comicTitle = title,
                genre = it.text()
            )
        }

        val localImage = downloadAndSaveImage(context, imageUrl, genereteId().toLong())


        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = description,
            author = author,
            location = "comicscan",
            localImageUri = localImage,
            chapters = chapters,
            genres = genres,
            alternativeTitle = alternativeTitle,
            rating = rating,
            type = type,
            status = status
        )
    }

    suspend fun parseDoujinDesu(url: String, jsonString: String, context: Context): ParsedData {
        val json = JSONObject(jsonString)

        // 1. Ambil data dasar
        val title = json.optString("title", "No Title")
        val slug = json.optString("slug", "")
        val imageUrl = json.optString("cover_url", "")
        val status = json.optString("status", "Unknown")
        val type = json.optString("type", "Manga")
        val rating = json.optDouble("rating", 0.0).toString()

        // 2. Bersihkan deskripsi dari tag HTML (<div>, <br>, dll) & potong teks "Download Batch"
        val rawDesc = json.optString("description", "")
        val cleanDesc = Jsoup.parse(rawDesc).text()
            .substringBefore("Download Batch")
            .trim()

        // 3. Handle author / artist yang bernilai null
        val authorRaw = json.optString("author", "")
        val artistRaw = json.optString("artist", "")
        val author = when {
            authorRaw.isNotEmpty() && authorRaw != "null" -> authorRaw
            artistRaw.isNotEmpty() && artistRaw != "null" -> artistRaw
            else -> json.optString("serialization", "Unknown")
        }

        // 4. Rapikan alt_titles yang dipisah tanda "|" menjadi koma
        val altTitles = json.optString("alt_titles", "")
            .replace("|", ", ")
            .trim()

        // 5. Parsing Array Genres (nested di dalam object "genres")
        val genresList =
            mutableListOf<Genre>() // Sesuaikan tipe datanya jika ParsedData pakai List<String> atau List<Genre>
        val mangaGenresArray = json.optJSONArray("manga_genres")
        if (mangaGenresArray != null) {
            for (i in 0 until mangaGenresArray.length()) {
                val item = mangaGenresArray.optJSONObject(i)
                val genreObj = item?.optJSONObject("genres")
                val genreName = genreObj?.optString("name", "") ?: ""
                val genreSlug = genreObj?.optString("slug", "") ?: ""
                if (genreName.isNotEmpty()) {
                    // Jika genres di ParsedData adalah List<String>, cukup: genresList.add(genreName)
                    genresList.add(
                        Genre(
                            comicTitle = title,
                            genre = genreName,
                        )
                    )
                }
            }
        }

        // 6. Parsing Array Chapters
        val chaptersList = mutableListOf<Chapter>()
        val chaptersArray = json.optJSONArray("chapters")
        if (chaptersArray != null) {
            for (i in 0 until chaptersArray.length()) {
                val chObj = chaptersArray.optJSONObject(i) ?: continue
                val chId = chObj.optString("id", "")
                val chNum = chObj.optString("chapter_number", "")
                val chTitle = chObj.optString("title", "Chapter $chNum")
                val createdAt = chObj.optString("created_at", "")

                // Format URL chapter (sesuaikan dengan endpoint API baca chapter Doujindesu kamu)
                val chapterUrl = "https://doujindesu.xxx/api/chapters/$chId"

                chaptersList.add(
                    Chapter(
                        title = chTitle,
                        link = chapterUrl,
                        comicTitle = title,
                        numChapter = chNum.toDouble(),
                        date = formatIsoDate(createdAt),
                        location = "doujindesu",
                        comicId = 0,
                    )
                )
            }
        }

        // 7. Download gambar lokal untuk cache/offline (sesuai kode bawaanmu)
        val localImage = downloadAndSaveImage(context, imageUrl, genereteId().toLong())

        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = cleanDesc,
            author = author,
            location = "doujindesu",
            tags = "adult",
            localImageUri = localImage,
            chapters = chaptersList,
            genres = genresList,
            alternativeTitle = altTitles,
            rating = rating,
            type = type.replaceFirstChar { it.uppercase() }, // "manhwa" -> "Manhwa"
            status = status.replaceFirstChar { it.uppercase() } // "ongoing" -> "Ongoing"
        )
    }

    suspend fun parsemanhwaIndo(document: Document, url: String, context: Context): ParsedData {
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
                location = "manhwaindo",
                comicId = 0

            )
        }

        val genres = document.select(".wd-full .mgen a").map {
            Genre(
                comicTitle = title,
                genre = it.text()
            )
        }
        val localImage = downloadAndSaveImage(context, imageUrl, genereteId().toLong())


        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = description,
            author = author,
            location = "manhwaindo",
            chapters = chapters,
            localImageUri = localImage,
            genres = genres,
            alternativeTitle = alternativeTitle,
            rating = rating,
            type = type,
            status = status
        )
    }

    suspend fun parseBatoto(document: Document, url: String, context: Context): ParsedData {

        val baseURL = "https://bato.si"

        val title = document.select("h3.text-lg a.link-hover").text() ?: ""
        val imageUrl =
            baseURL + document.select("div.w-24").attr("q:key", "4X_2").select("img").attr("src")
        val description = document.select(".limit-html-p").text() ?: ""
        val author =
            document.select("div.text-sm").attr("q:key", "k6_4").select("a")[0].text() ?: ""

        val alternativeTitle =
            document.select(".grow .mt-1").attr("q:key", "k6_2").select("span").text()
                ?: "Tidak Ada Alternative"
        val rating = "-"
        val type = "Manhwa"
        val status = document.select("div").attr("q:key", "0S_9").select("span.text-success").text()
            ?: "onGoing"


        val chapters =
            document.select("div").attr("data-name", "chapter-list").select(".space-y-3 .group div")
                .attr("q:key", "N0_9")
                .filter { chapterItem -> // <-- Setiap `chapterItem` adalah satu `<li>`
                    val chapterLinkElement =
                        chapterItem.selectFirst("div.space-x-1 a.link-hover.link-primary")
                    val dateElement = chapterItem.selectFirst("div.ml-auto time")

                    // Memastikan elemen link chapter dan elemen tanggal ada
                    // DAN TEKS LINK CHAPTER MENGANDUNG KATA "Chapter" (case-insensitive)
                    chapterLinkElement != null &&
                            dateElement != null &&
                            chapterLinkElement.text()
                                .contains("Chapter", ignoreCase = true) // <-- FILTER BARU INI!
                }
                .map {

                    val number = it.select(".space-x-1 a").text()
                    val date = it.select(".ml-auto time").attr("data-time")

                    val time = formatWaktuLalu(date.toLong())

                    val chapterLink = it.select(".space-x-1 a").attr("href")

                    Chapter(
                        title = it.select("div.space-x-1 a.link-hover.link-primary").text(),
                        link = baseURL + chapterLink,
                        comicTitle = title,
                        numChapter = parsechapterToInt(number),
                        date = time,
                        location = "batoto-all",
                        comicId = 0

                    )
                }

        val genres =
            document.select("div").attr("q:key", "5P_2").select("span span.whitespace-nowrap")
                .map {
                    Genre(
                        comicTitle = title,
                        genre = it.text()
                    )
                }
        val localImage = downloadAndSaveImage(context, imageUrl, genereteId().toLong())


        return ParsedData(
            url = url,
            title = title,
            imageUrl = imageUrl,
            description = description,
            author = author,
            location = "batoto-all",
            chapters = chapters,
            tags = "adult",
            localImageUri = localImage,
            genres = genres,
            alternativeTitle = alternativeTitle,
            rating = rating,
            type = type,
            status = status
        )
    }

    private fun formatIsoDate(isoString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(isoString)
            if (date != null) outputFormat.format(date) else isoString
        } catch (e: Exception) {
            isoString.substringBefore("T") // Fallback ambil tanggalnya saja (YYYY-MM-DD)

        }
    }
}