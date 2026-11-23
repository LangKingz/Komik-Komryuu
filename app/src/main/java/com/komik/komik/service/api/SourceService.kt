package com.komik.komik.service.api

import com.komik.komik.service.api.model.AppConfigResponse
import com.komik.komik.service.api.model.AuthRequest
import com.komik.komik.service.api.model.AuthResponse
import com.komik.komik.service.api.model.GenericAdminResponse
import com.komik.komik.service.api.model.ReportItem
import com.komik.komik.service.api.model.ReportListResponse
import com.komik.komik.service.api.model.ReportRequest
import com.komik.komik.service.api.model.ReportResponse
import com.komik.komik.service.api.model.SourceNewModel
import com.komik.komik.service.api.model.SourceResponse
import com.komik.komik.service.api.model.SyncRequest
import com.komik.komik.service.api.model.SyncResponse
import com.komik.komik.service.api.model.UpdateConfigRequest
import com.komik.komik.service.api.model.UpdateExtensionRequest
import com.komik.komik.service.api.model.UpdateReportRequest
import com.komik.komik.service.api.model.UpdateUserRoleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @Headers("Content-Type: application/json")
    @POST("api/v1/sync")
    suspend fun syncData(
        @Body request: SyncRequest
    ): Response<SyncResponse>

    @GET("api/v1/sync")
    suspend fun restoreData(
        @Query("uid") uid: String
    ): Response<SyncResponse>

    @POST("api/v1/reports")
    suspend fun submitReport(
        @Body request: ReportRequest
    ): Response<ReportResponse>
}

interface newSourceService {

    @GET("api/v1/extensi")
    suspend fun getSource(
        @Query("type") type: String = "",
        @Query("includeInactive") includeInactive: Boolean = false
    ): SourceNewModel

    @PATCH("api/v1/extensi")
    suspend fun updateExtension(
        @Body request: UpdateExtensionRequest
    ): Response<GenericAdminResponse>

    @PUT("api/v1/extensi")
    suspend fun putExtension(
        @Body request: UpdateExtensionRequest
    ): Response<GenericAdminResponse>

    @POST("api/v1/auth")
    suspend fun authenticateUser(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @GET("api/v1/config")
    suspend fun getAppConfig(): Response<AppConfigResponse>

    @PATCH("api/v1/config")
    suspend fun updateAppConfig(
        @Body request: UpdateConfigRequest
    ): Response<GenericAdminResponse>

    @POST("api/v1/config")
    suspend fun postAppConfig(
        @Body request: UpdateConfigRequest
    ): Response<GenericAdminResponse>

    @Headers("Content-Type: application/json")
    @POST("api/v1/sync")
    suspend fun syncData(
        @Body request: SyncRequest
    ): Response<SyncResponse>

    @GET("api/v1/sync")
    suspend fun restoreData(
        @Query("uid") uid: String
    ): Response<SyncResponse>

    @POST("api/v1/reports")
    suspend fun submitReport(
        @Body request: ReportRequest
    ): Response<ReportResponse>

    @GET("api/v1/reports")
    suspend fun getReports(
        @Query("status") status: String? = null
    ): Response<ReportListResponse>

    @PATCH("api/v1/reports")
    suspend fun updateReport(
        @Body request: UpdateReportRequest
    ): Response<GenericAdminResponse>

    @DELETE("api/v1/reports")
    suspend fun deleteReport(
        @Query("id") id: String
    ): Response<GenericAdminResponse>

    @PATCH("api/v1/users")
    suspend fun updateUserRole(
        @Body request: UpdateUserRoleRequest
    ): Response<GenericAdminResponse>
}
