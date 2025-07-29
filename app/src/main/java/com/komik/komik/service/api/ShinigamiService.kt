package com.komik.komik.service.api

import com.komik.komik.service.api.model.ChapterResponseSHinigami
import com.komik.komik.service.api.model.DetailShinigamiResponse
import com.komik.komik.service.api.model.ImageResponsShinigami
import com.komik.komik.service.api.model.ShinigamiHomeExplore
import retrofit2.http.GET
import retrofit2.http.Path
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
        @Query("q") query:String?=null,
    ): ShinigamiHomeExplore

    @GET("v1/manga/detail/{id}")
    suspend fun getMangaDetail(
        @Path("id") id:String,
    ): DetailShinigamiResponse

    @GET("v1/chapter/{id}/list")
    suspend fun getChapter(
        @Path("id") id:String,
        @Query("page") page:Int=1,
        @Query("page_size") pageSize:Int,
        @Query("sort_by") sortBy:String = "chapter_number",
        @Query("sort_order") sortOrder:String= "desc",
    ): ChapterResponseSHinigami

    @GET("v1/chapter/detail/{id}")
    suspend fun getChapterDetail(
        @Path("id") id:String,
    ): ImageResponsShinigami
}