package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class IkiruExplore(

	@field:SerializedName("code")
	val code: Int? = null,

	@field:SerializedName("data")
	val data: Datas? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class GenreItemIkiru(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class ChapterItem(

	@field:SerializedName("number")
	val number: Int? = null,

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
)

data class ProjectItem(

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("chapter")
	val chapter: List<ChapterItem?>? = null,

	@field:SerializedName("metadata")
	val metadata: MetadataIkiru? = null,

	@field:SerializedName("featuredImage")
	val featuredImage: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("type")
	val type: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class MetadataIkiru(

	@field:SerializedName("score")
	val score: Any? = null,

	@field:SerializedName("genre")
	val genre: List<GenreItemIkiru?>? = null,

	@field:SerializedName("views")
	val views: Int? = null
)

data class Datas(

	@field:SerializedName("total")
	val total: Int? = null,

	@field:SerializedName("project")
	val project: List<ProjectItem?>? = null
)
