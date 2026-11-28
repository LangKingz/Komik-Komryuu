package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class SourceResponse(
	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("error")
	val error: String? = null,

	@field:SerializedName("list")
	val list: List<ListItem?>? = null
)

data class ListItem(
	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("location")
	val location: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("extensi")
	val extensi: String? = null,

	@field:SerializedName("url")
	val url: String? = null,

	@field:SerializedName("baseUrl", alternate = ["BaseUrl"])
	val baseUrl: String? = null,

	@field:SerializedName("mirrorUrls")
	val mirrorUrls: List<String>? = null,

	@field:SerializedName("isActive")
	val isActive: Boolean? = true,

	@field:SerializedName("isVipOnly")
	val isVipOnly: Boolean? = false,

	@field:SerializedName("customHeaders")
	val customHeaders: CustomHeaders? = null
)

