package com.komik.komik.service.api

import com.komik.komik.service.api.model.SourceNewModel
import com.komik.komik.service.api.model.SourceResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SourceService {

    @GET("api/website")
    suspend fun getSource(): SourceResponse
}

interface  newSourceService {

    @GET("api/v1/extensi")
    suspend fun getSource(
        @Query("type") type: String = "",
    ): SourceNewModel

}