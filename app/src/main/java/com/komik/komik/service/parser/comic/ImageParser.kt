package com.komik.komik.service.parser.comic

import com.komik.komik.service.parser.model.getImages
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
}