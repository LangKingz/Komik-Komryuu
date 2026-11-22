package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class ImageIkiruResponse(

	@field:SerializedName("code")
	val code: Int? = null,

	@field:SerializedName("data")
	val data: DataImageIkiru? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class DataImageIkiru(

	@field:SerializedName("wpId")
	val wpId: Any? = null,

	@field:SerializedName("mangaId")
	val mangaId: String? = null,

	@field:SerializedName("nextChapter")
	val nextChapter: NextChapter? = null,

	@field:SerializedName("downloadUrl")
	val downloadUrl: Any? = null,

	@field:SerializedName("prevChapter")
	val prevChapter: PrevChapter? = null,

	@field:SerializedName("label")
	val label: Any? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("userId")
	val userId: Any? = null,

	@field:SerializedName("chapterStatus")
	val chapterStatus: String? = null,

	@field:SerializedName("sourceUrl")
	val sourceUrl: Any? = null,

	@field:SerializedName("number")
	val number: Int? = null,

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("medias")
	val medias: List<MediasItem?>? = null,

	@field:SerializedName("music")
	val music: Any? = null,

	@field:SerializedName("musicId")
	val musicId: Any? = null,

	@field:SerializedName("manga")
	val manga: Manga? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null,

	@field:SerializedName("views")
	val views: Int? = null,

	@field:SerializedName("downloadCount")
	val downloadCount: Int? = null,

	@field:SerializedName("scheduledAt")
	val scheduledAt: Any? = null,

	@field:SerializedName("isAdult")
	val isAdult: Boolean? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
)

data class PrevChapter(

	@field:SerializedName("number")
	val number: Int? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class NextChapter(

	@field:SerializedName("number")
	val number: Int? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class MediasItem(

	@field:SerializedName("external")
	val external: Boolean? = null,

	@field:SerializedName("pageNumber")
	val pageNumber: Int? = null,

	@field:SerializedName("filePath")
	val filePath: String? = null,

	@field:SerializedName("serverId")
	val serverId: Any? = null
)

data class Manga(

	@field:SerializedName("featuredImage")
	val featuredImage: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("isAdult")
	val isAdult: Boolean? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)
