package com.komik.komik.service.api

import com.komik.komik.service.api.model.DetailIkiruResponse
import com.komik.komik.service.api.model.IkiruEksploreResponse
import com.komik.komik.service.api.model.IkiruExplore
import com.komik.komik.service.api.model.ImageIkiruResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface IkiruService {
    @GET("api/public/library/search")
    suspend fun getProjectManga(
        @Query("page") page: Int? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("q") query: String? = null,
        @Query("search") search: String? = null
    ): IkiruEksploreResponse

    @GET("api/public/library/search")
    suspend fun searchManga(
        @Query("q") query: String,
        @Query("page") page: Int? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sort") sort: String? = null
    ): IkiruEksploreResponse

    @GET("api/public/project")
    suspend fun getProjects(
        @Query("page") page: Int? = null
    ): IkiruExplore

    @GET("api/public/manga/{slug}")
    suspend fun getDetailManga(
        @Path("slug") slug: String
    ): DetailIkiruResponse

    // Endpoint untuk mengambil gambar per chapter
    // Contoh pemanggilan: getChapterImages("bersinar-terang", "chapter-14")
    @GET("api/public/manga/{mangaSlug}/{chapterSlug}")
    suspend fun getChapterImages(
        @Path("mangaSlug") mangaSlug: String,
        @Path("chapterSlug") chapterSlug: String
    ): ImageIkiruResponse
}
