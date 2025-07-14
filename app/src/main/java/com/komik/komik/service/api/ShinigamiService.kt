package com.komik.komik.service.api

import com.komik.komik.service.api.model.ShinigamiHomeExplore
import retrofit2.http.GET
import retrofit2.http.Query

interface ShinigamiService {

    @GET("v1/manga/list")
    suspend fun getMangaList(
        @Query("page") page:Int=1,
        @Query("page_size") pageSize:Int,
        @Query("genre_include_mode") genreIncludeMode:String= "or",
        @Query("genre_exclude_mode") genreExcludeMode:String= "or",
        @Query("sort_order") sortOrder:String= "desc",
        @Query("sort") sortBy:String,
    ): ShinigamiHomeExplore
}