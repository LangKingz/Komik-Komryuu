package com.komik.komik.service.Scrape

import com.komik.komik.service.client
import com.komik.komik.service.parser.model.getListUpdate
import org.jsoup.Jsoup

suspend fun main() {
    try {
        val html = client.Ikiru.getHtml("https://otakudesu.best/ongoing-anime/")
        val document = Jsoup.parse(html)

        val pagedData = document.select(".rapi ul li").mapIndexed { index, element ->
            getListUpdate(
                index = index,
                title = element.select("h2.jdlflm").text(),
                urlImage = element.select(".thumbz img").attr("src"),
                link = element.select("a").attr("href"),
                location = "otaku"
            )
       }

        println(pagedData)


    }catch (e: Exception){
        e.printStackTrace()
        println(e.message)
    }

}