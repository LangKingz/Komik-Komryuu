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

	@field:SerializedName("BaseUrl")
	val baseUrl: String? = null,

	@field:SerializedName("type")
	val type: String? = null
)
