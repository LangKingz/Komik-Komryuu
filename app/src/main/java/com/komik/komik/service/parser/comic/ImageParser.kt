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
}