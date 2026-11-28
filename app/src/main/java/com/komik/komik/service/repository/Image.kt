package com.komik.komik.service.repository

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositoryImage
import com.komik.komik.service.parser.comic.ImageParser

import com.komik.komik.service.parser.model.getImages
import org.jsoup.Jsoup

class Image(private val client: client) : RepositoryImage {

    companion object {
        private const val TAG = "Image"

        fun extractIkiruChapterSlugs(rawUrl: String): Pair<String, String> {
            val clean = rawUrl.trim().trimEnd('/')
            val withoutProtocol = if (clean.contains("://")) clean.substringAfter("://").substringAfter('/') else clean
            val withoutQuery = withoutProtocol.substringBefore('?').substringBefore('#')
            val segments = withoutQuery
                .split('/')
                .map { it.trim() }
                .filter { it.isNotBlank() && it != "manga" && it != "api" && it != "public" && !it.contains("ikiru.") }
            return when {
                segments.size >= 2 -> Pair(segments[segments.size - 2], segments[segments.size - 1])
                segments.size == 1 -> Pair("", segments[0])
                else -> Pair("", "")
            }
        }
    }

    override suspend fun getImage(url: String, type: sourceType): List<getImages> {
        try {
            return when (type) {
                sourceType.ikiru -> {
                    try {
                        val (mangaSlug, chapterSlug) = extractIkiruChapterSlugs(url)
                        val baseUrl = if (client.getIkiruUrl().isNotBlank()) client.getIkiruUrl().trimEnd('/') else "https://11.ikiru.wtf"
                        Log.d("IKIRU_TRACE", "Image.getImage called: url='$url' -> extracted mangaSlug='$mangaSlug', chapterSlug='$chapterSlug'")

                        if (mangaSlug.isNotBlank() && chapterSlug.isNotBlank()) {
                            Log.d("IKIRU_TRACE", "Image.getImage: hitting IkiruService.getChapterImages(mangaSlug='$mangaSlug', chapterSlug='$chapterSlug')")
                            val response = client.ikiruService.getChapterImages(
                                chapterSlug = chapterSlug,
                                mangaSlug = mangaSlug
                            )
                            val medias = response.data?.medias.orEmpty().filterNotNull()
                            val sortedMedias = medias.sortedBy { it.pageNumber ?: 0 }
                            val imagesList = sortedMedias.mapIndexedNotNull { index, mediaItem ->
                                val rawPath = mediaItem.filePath?.replace("&amp;", "&")?.trim().orEmpty()
                                if (rawPath.isNotBlank()) {
                                    val fullUrl = when {
                                        rawPath.startsWith("//") -> "https:$rawPath"
                                        rawPath.startsWith("http://", ignoreCase = true) || rawPath.startsWith("https://", ignoreCase = true) -> rawPath
                                        rawPath.startsWith("/") -> "$baseUrl$rawPath"
                                        else -> "$baseUrl/$rawPath"
                                    }
                                    getImages(
                                        index = mediaItem.pageNumber ?: (index + 1),
                                        image = fullUrl
                                    )
                                } else null
                            }

                            Log.d("IKIRU_TRACE", "Image.getImage: received ${imagesList.size} images from API")
                            imagesList.take(3).forEachIndexed { idx, item ->
                                Log.d("IKIRU_TRACE", "Image URL [$idx]: ${item.image}")
                            }

                            if (imagesList.isNotEmpty()) {
                                return imagesList
                            }
                        }

                        // Fallback ke scraping HTML jika slug tidak valid atau medias kosong
                        Log.w("IKIRU_TRACE", "Image.getImage: API returned no images or invalid slugs for url='$url', trying HTML fallback")
                        val html = client.Ikiru.getHtml(url)
                        val document = Jsoup.parse(html)
                        val pagedData = ImageParser.ikiruParse(document).map { img ->
                            val clean = img.image.replace("&amp;", "&").trim()
                            val fullUrl = when {
                                clean.startsWith("//") -> "https:$clean"
                                clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true) -> clean
                                clean.startsWith("/") -> "$baseUrl$clean"
                                else -> "$baseUrl/$clean"
                            }
                            img.copy(image = fullUrl)
                        }
                        Log.d("IKIRU_TRACE", "Image.getImage: Ikiru HTML fallback count=${pagedData.size}")
                        pagedData
                    } catch (e: Exception) {
                        Log.e("IKIRU_TRACE", "Image.getImage error for url='$url': ${e.message}, fallback to HTML", e)
                        try {
                            val baseUrl = if (client.getIkiruUrl().isNotBlank()) client.getIkiruUrl().trimEnd('/') else "https://11.ikiru.wtf"
                            val html = client.Ikiru.getHtml(url)
                            val document = Jsoup.parse(html)
                            val fallback = ImageParser.ikiruParse(document).map { img ->
                                val clean = img.image.replace("&amp;", "&").trim()
                                val fullUrl = when {
                                    clean.startsWith("//") -> "https:$clean"
                                    clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true) -> clean
                                    clean.startsWith("/") -> "$baseUrl$clean"
                                    else -> "$baseUrl/$clean"
                                }
                                img.copy(image = fullUrl)
                            }
                            Log.d("IKIRU_TRACE", "Image.getImage: Ikiru HTML fallback in catch count=${fallback.size}")
                            fallback
                        } catch (ex: Exception) {
                            Log.e("IKIRU_TRACE", "Image.getImage HTML fallback failed: ${ex.message}", ex)
                            emptyList()
                        }
                    }
                }

                sourceType.komikcast-> {
                    val html = client.Komikcast.getHtml(url)
                    val document = Jsoup.parse(html)
                    val pageData = ImageParser.komikcastParse(document)

                    Log.d(TAG, "getImage: $pageData")

                    pageData
                }

                sourceType.comicscan->{
                    val html = client.Comicscan.getHtml(url)
                    val document = Jsoup.parse(html)
                    val pageData = ImageParser.comicscanParse(document)

                    Log.d(TAG, "getImage: $pageData")

                    pageData
                }

                sourceType.kiryuu->{
                    val html = client.Ikiru.getHtml(url)
                    val document = Jsoup.parse(html)
                    val pageData = ImageParser.kiryuuParse(document)

                    Log.d(TAG, "getImage: $pageData")

                    pageData
                }

                sourceType.manhwaindo -> {
                    val html = client.Ikiru.getHtml(url)
                    val document = Jsoup.parse(html)
                    val pageData = ImageParser.manhwaIndoParse(document)

                    Log.d(TAG, "getImage: $pageData")
                    pageData
                }

                sourceType.doujindesu-> {
                    val response = client.Doujindesu.getHtml(url)
                    Log.d(TAG, "getImage: $response")

                    val pageData = ImageParser.doujindesuParseAsync(url,response)

                    pageData
                }

                sourceType.batoto_all -> {
                    val html = client.ScrapeDataImage.getHtml(url)
                    val document = Jsoup.parse(html)
                    val pageData = ImageParser.batotoParse(document)
                    Log.d(TAG, "getImage: $pageData")
                    pageData
                }

                sourceType.shinigami -> {
                    val pageData = ImageParser.parseShinigami(client,url)
                    Log.d(TAG, "getImage: $pageData")
                    pageData
                }

                else -> emptyList()
            }
        }catch (e:Exception){
            e.printStackTrace()
            Log.e(TAG, "getImage: ${e.message}")
            return emptyList()
        }
    }
}