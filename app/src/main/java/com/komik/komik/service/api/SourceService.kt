package com.komik.komik.service.api

import com.komik.komik.service.api.model.AppConfigResponse
import com.komik.komik.service.api.model.AuthRequest
import com.komik.komik.service.api.model.AuthResponse
import com.komik.komik.service.api.model.SourceNewModel
import com.komik.komik.service.api.model.SourceResponse
import com.komik.komik.service.api.model.SyncRequest
import com.komik.komik.service.api.model.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SourceService {

    @GET("api/website")
    suspend fun getSource(): SourceResponse

    @POST("api/v1/auth")
    suspend fun authenticateUser(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @GET("api/v1/config")
    suspend fun getAppConfig(): Response<AppConfigResponse>

    @POST("api/v1/sync")
    suspend fun syncData(
        @Body request: SyncRequest
    ): Response<SyncResponse>

    @GET("api/v1/sync")
    suspend fun restoreData(
        @Query("uid") uid: String
    ): Response<SyncResponse>
}

interface newSourceService {

    @GET("api/v1/extensi")
    suspend fun getSource(
        @Query("type") type: String = "",
    ): SourceNewModel

    @POST("api/v1/auth")
    suspend fun authenticateUser(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @GET("api/v1/config")
    suspend fun getAppConfig(): Response<AppConfigResponse>

    @POST("api/v1/sync")
    suspend fun syncData(
        @Body request: SyncRequest
    ): Response<SyncResponse>

    @GET("api/v1/sync")
    suspend fun restoreData(
        @Query("uid") uid: String
    ): Response<SyncResponse>
}
