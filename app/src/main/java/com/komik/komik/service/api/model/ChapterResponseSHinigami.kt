package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class ChapterResponseSHinigami(

	@field:SerializedName("data")
	val data: List<DataIteme?>? = null,

	@field:SerializedName("meta")
	val meta: Metae? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("retcode")
	val retcode: Int? = null
)

data class DataIteme(

	@field:SerializedName("thumbnail_image_url")
	val thumbnailImageUrl: String? = null,

	@field:SerializedName("release_date")
	val releaseDate: String? = null,

	@field:SerializedName("chapter_number")
	val chapterNumber: Int? = null,

	@field:SerializedName("chapter_title")
	val chapterTitle: String? = null,

	@field:SerializedName("chapter_id")
	val chapterId: String? = null,

	@field:SerializedName("manga_id")
	val mangaId: String? = null,

	@field:SerializedName("view_count")
	val viewCount: Int? = null
)

data class Metae(

	@field:SerializedName("total_page")
	val totalPage: Int? = null,

	@field:SerializedName("page")
	val page: Int? = null,

	@field:SerializedName("request_id")
	val requestId: String? = null,

	@field:SerializedName("process_time")
	val processTime: String? = null,

	@field:SerializedName("timestamp")
	val timestamp: Long? = null,

	@field:SerializedName("page_size")
	val pageSize: Int? = null,

	@field:SerializedName("total_record")
	val totalRecord: Int? = null
)
