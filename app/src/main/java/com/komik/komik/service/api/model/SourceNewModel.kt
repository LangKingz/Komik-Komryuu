package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class SourceNewModel(
	@field:SerializedName("total")
	val total: Int? = null,

	@field:SerializedName("data")
	val data: List<DataItems?>? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class DataItems(
	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("UrlSearch")
	val urlSearch: String? = null,

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("logo")
	val logo: String? = null,

	@field:SerializedName("location")
	val location: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("BaseUrl", alternate = ["baseUrl"])
	val baseUrl: String? = null,

	@field:SerializedName("mirrorUrls")
	val mirrorUrls: List<String>? = null,

	@field:SerializedName("isActive")
	val isActive: Boolean? = true,

	@field:SerializedName("isVipOnly")
	val isVipOnly: Boolean? = false,

	@field:SerializedName("customHeaders")
	val customHeaders: CustomHeaders? = null,

	@field:SerializedName("type")
	val type: String? = null
)

data class CustomHeaders(
	@field:SerializedName("referer", alternate = ["Referer"])
	val referer: String? = null,

	@field:SerializedName("userAgent", alternate = ["User-Agent"])
	val userAgent: String? = null,

	@field:SerializedName("cookie", alternate = ["Cookie"])
	val cookie: String? = null
)
