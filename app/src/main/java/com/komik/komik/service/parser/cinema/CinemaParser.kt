package com.komik.komik.service.parser.cinema

import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.parser.model.getListUpdate
import org.jsoup.nodes.Document

object CinemaParser {

    fun parswerOtakuDesu(document: Document): List<getListUpdate>{
        return document.select(".rapi ul li").mapIndexed { index, element ->
            getListUpdate(
                index = index,
                title = element.select("h2.jdlflm").text(),
                urlImage = element.select(".thumbz img").attr("src"),
                link = element.select("a").attr("href"),
                location = "otaku",
                tagModel = "cinema",
                chapter = element.select(".epz").text(),
            )
        }
    }
}