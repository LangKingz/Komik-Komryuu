package com.komik.komik.service.Scrape

import com.komik.komik.service.client
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.parser.model.getListUpdate
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

//              https://doujindesu.tv/manhwa/

//            https://doujindesu.tv/beautiful-days-chapter-41/

suspend fun main() {

    val url = "https://doujindesu.tv/beautiful-days-chapter-41/"
    val html =
        client.Ikiru.getHtml(url)
    val document = Jsoup.parse(html)

    val id = document.select("main").attr("data-id")

    val parseImage = client.Ikiru.getHtml("")

    val pageData = document.select(".entries article").mapIndexed { index, element ->
        getListUpdate(
            index = index,
            title = element.select(".entry a").attr("title"),
            urlImage = element.select(".thumbnail img").attr("src"),
            link = element.select(".entry a").attr("href"),

            )
    }
    val data =
        document.select("div#anu img").mapIndexed { index, element ->
            getImages(
                index = index,
                image = element.attr("src"),
            )
        }

    val client = OkHttpClient()
    val formBuilder = FormBody.Builder()
        .add("id", id)
        .build()

    val request = Request.Builder()
        .url("https://doujindesu.tv/themes/ajax/ch.php")
        .post(formBuilder)
        .header(
            "Referer",url
        )
        .header(
            "X-Requested-With","XMLHttpRequest"
        )
        .header(
            "Accept","image/avif,image/webp,*/*"
        ).build()

    try {

        println("pageData: $id")
        val result = client.newCall(request).execute()

        val body = result.body?.string()

        if(body != null){
            val doc = Jsoup.parse(body)
            val imageParser = doc.select("img").mapIndexed { index, element ->
                getImages(
                    index = index,
                    image = element.attr("src"),
                )
            }

            println("imageParser: $imageParser")
        }



    }catch (e:Exception){
        e.printStackTrace()
    }





//            Log.d(TAG, "fakecookie: ${generateFakeCookie()}")

}
