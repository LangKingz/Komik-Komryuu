package com.komik.komik.service.repository

import android.util.Log
import com.komik.komik.service.client
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.modelrepository.RepositoryImage
import com.komik.komik.service.parser.comic.ImageParser

import com.komik.komik.service.parser.model.getImages
import org.jsoup.Jsoup

class Image(private val client: client) : RepositoryImage {
    override suspend fun getImage(url: String, type: sourceType): List<getImages> {
        try {
            return when (type) {
                sourceType.ikiru -> {
                    try {
                        val cleanUrl = url.trim().trimEnd('/')
                        val pathParts = cleanUrl.substringAfter("://").substringAfter('/')
                            .split('/')
                            .filter { it.isNotBlank() && it != "manga" && it != "api" && it != "public" }

                        val (mangaSlug, chapterSlug) = if (pathParts.size >= 2) {
                            Pair(pathParts[pathParts.size - 2], pathParts[pathParts.size - 1])
                        } else if (cleanUrl.contains('/')) {
                            val segments = cleanUrl.split('/').filter { it.isNotBlank() }
                            if (segments.size >= 2) {
                                Pair(segments[segments.size - 2], segments[segments.size - 1])
                            } else {
                                Pair("", "")
                            }
                        } else {
                            Pair("", "")
                        }

                        if (mangaSlug.isNotBlank() && chapterSlug.isNotBlank()) {
                            val response = client.ikiruService.getChapterImages(mangaSlug, chapterSlug)
                            val medias = response.data?.medias.orEmpty().filterNotNull()
                            val sortedMedias = medias.sortedBy { it.pageNumber ?: 0 }
                            val imagesList = sortedMedias.mapIndexedNotNull { index, mediaItem ->
                                val rawPath = mediaItem.filePath?.trim().orEmpty()
                                if (rawPath.isNotBlank()) {
                                    val fullUrl = if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
                                        rawPath
                                    } else {
                                        "${client.getIkiruUrl().trimEnd('/')}/${rawPath.trimStart('/')}"
                                    }
                                    getImages(
                                        index = mediaItem.pageNumber ?: (index + 1),
                                        image = fullUrl
                                    )
                                } else null
                            }

                            if (imagesList.isNotEmpty()) {
                                Log.d(TAG, "Ikiru API getImage: ${imagesList.size} pages")
                                return imagesList
                            }
                        }

                        // Fallback ke scraping HTML jika slug tidak valid atau medias kosong
                        val html = client.Ikiru.getHtml(url)
                        val document = Jsoup.parse(html)
                        val pagedData = ImageParser.ikiruParse(document)
                        Log.d(TAG, "Ikiru HTML fallback getImage: $pagedData")
                        pagedData
                    } catch (e: Exception) {
                        Log.e(TAG, "Ikiru API getChapterImages error for $url: ${e.message}, fallback to HTML")
                        val html = client.Ikiru.getHtml(url)
                        val document = Jsoup.parse(html)
                        ImageParser.ikiruParse(document)
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

    companion object{
        private const val TAG = "Image"
    }
}