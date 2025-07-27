package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class ImageResponsShinigami(

	@field:SerializedName("data")
	val data: Datan? = null,

	@field:SerializedName("meta")
	val meta: Metan? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("retcode")
	val retcode: Int? = null
)

data class Metan(

	@field:SerializedName("request_id")
	val requestId: String? = null,

	@field:SerializedName("process_time")
	val processTime: String? = null,

	@field:SerializedName("timestamp")
	val timestamp: Long? = null
)

data class Chapter(

	@field:SerializedName("path")
	val path: String? = null,

	@field:SerializedName("data")
	val data: List<String?>? = null
)

data class Datan(

	@field:SerializedName("base_url_low")
	val baseUrlLow: String? = null,

	@field:SerializedName("chapter")
	val chapter: Chapter? = null,

	@field:SerializedName("prev_chapter_number")
	val prevChapterNumber: Int? = null,

	@field:SerializedName("chapter_number")
	val chapterNumber: Int? = null,

	@field:SerializedName("base_url")
	val baseUrl: String? = null,

	@field:SerializedName("chapter_title")
	val chapterTitle: String? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("manga_id")
	val mangaId: String? = null,

	@field:SerializedName("prev_chapter_id")
	val prevChapterId: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("thumbnail_image_url")
	val thumbnailImageUrl: String? = null,

	@field:SerializedName("release_date")
	val releaseDate: String? = null,

	@field:SerializedName("chapter_id")
	val chapterId: String? = null,

	@field:SerializedName("next_chapter_number")
	val nextChapterNumber: Any? = null,

	@field:SerializedName("view_count")
	val viewCount: Int? = null,

	@field:SerializedName("next_chapter_id")
	val nextChapterId: Any? = null
)
