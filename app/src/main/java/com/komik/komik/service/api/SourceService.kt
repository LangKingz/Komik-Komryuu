package com.komik.komik.service.api

import com.komik.komik.service.api.model.SourceResponse
import retrofit2.http.GET

interface SourceService {

    @GET("api/website")
    suspend fun getSource(): SourceResponse
}