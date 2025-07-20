package com.komik.komik.service.api

import retrofit2.http.GET
import retrofit2.http.Url

interface ScrapeIkiru {

    @GET
    suspend fun getHtml(@Url url: String): String

}

interface ScrapeKomikcast {

    @GET
    suspend fun getHtml(@Url url: String): String
}

interface ScrapeComicscan{

    @GET
    suspend fun getHtml(@Url url: String): String
}

interface Scrape {
    @GET
    suspend fun getHtml(@Url url: String): String
}