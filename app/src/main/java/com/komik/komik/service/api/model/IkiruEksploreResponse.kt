package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class IkiruEksploreResponse(

	@field:SerializedName("code")
	val code: Int? = null,

	@field:SerializedName("data")
	val data: DataExplore? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class MetadataExplore(

	@field:SerializedName("score")
	val score: Any? = null,

	@field:SerializedName("genre")
	val genre: List<GenreItemExplore?>? = null
)

data class GenreItemExplore(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class ChapterItemIkiru(

	@field:SerializedName("number")
	val number: Int? = null,

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
)

data class MangasItem(

	@field:SerializedName("chapter")
	val chapter: List<ChapterItemIkiru?>? = null,

	@field:SerializedName("metadata")
	val metadata: MetadataExplore? = null,

	@field:SerializedName("featuredImage")
	val featuredImage: String? = null,

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("type")
	val type: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class DataExplore(

	@field:SerializedName("total")
	val total: Int? = null,

	@field:SerializedName("mangas")
	val mangas: List<MangasItem?>? = null
)
