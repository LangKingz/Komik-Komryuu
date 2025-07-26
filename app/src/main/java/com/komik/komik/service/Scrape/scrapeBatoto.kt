package com.komik.komik.service.Scrape

import com.komik.komik.service.client
import com.komik.komik.service.parser.comic.ImageParser
import org.jsoup.Jsoup

suspend fun main(){
    val url = "https://bato.si/title/198136-en-food-truck-owner-inside-the-dungeon-ken-manga/3485745-ch_25"
    val html = client.ScrapeDataImage.getHtml(url)

    val document = Jsoup.parse(html)

    val imageParser = ImageParser.batotoParse(document)

    println(imageParser)
}