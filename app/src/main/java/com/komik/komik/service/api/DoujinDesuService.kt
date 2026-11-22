package com.komik.komik.service.api

import com.komik.komik.service.api.model.DoujindesuEncrypt
import com.komik.komik.service.api.model.ShinigamiHomeExplore
import retrofit2.http.GET
import retrofit2.http.Query

interface DoujinDesuService {

    @GET("api/manga")
    suspend fun getMangaList(
        @Query("limit") page: Int,
        @Query("type") type: String = "Manhwa",
        @Query("sort") sort: String = "latest_chapter",
        @Query("offset") offset: Int,
    ): DoujindesuEncrypt
}