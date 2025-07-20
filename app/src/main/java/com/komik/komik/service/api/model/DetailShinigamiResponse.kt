package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class DetailShinigamiResponse(

	@field:SerializedName("data")
	val data: Data? = null,

	@field:SerializedName("meta")
	val meta: Metas? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("retcode")
	val retcode: Int? = null
)

data class TypeItems(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class FormatItems(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class Metas(

	@field:SerializedName("request_id")
	val requestId: String? = null,

	@field:SerializedName("process_time")
	val processTime: String? = null,

	@field:SerializedName("timestamp")
	val timestamp: Long? = null
)

data class GenreItems(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class Taxonomys(

	@field:SerializedName("Artist")
	val artist: List<ArtistItems?>? = null,

	@field:SerializedName("Type")
	val type: List<TypeItems?>? = null,

	@field:SerializedName("Format")
	val format: List<FormatItems?>? = null,

	@field:SerializedName("Author")
	val author: List<AuthorItems?>? = null,

	@field:SerializedName("Genre")
	val genre: List<GenreItems?>? = null
)

data class Data(

	@field:SerializedName("latest_chapter_time")
	val latestChapterTime: String? = null,

	@field:SerializedName("cover_image_url")
	val coverImageUrl: String? = null,

	@field:SerializedName("release_year")
	val releaseYear: String? = null,

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("cover_portrait_url")
	val coverPortraitUrl: String? = null,

	@field:SerializedName("bookmark_count")
	val bookmarkCount: Int? = null,

	@field:SerializedName("taxonomy")
	val taxonomy: Taxonomys? = null,

	@field:SerializedName("manga_id")
	val mangaId: String? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("deleted_at")
	val deletedAt: Any? = null,

	@field:SerializedName("latest_chapter_id")
	val latestChapterId: String? = null,

	@field:SerializedName("user_rate")
	val userRate: Any? = null,

	@field:SerializedName("latest_chapter_number")
	val latestChapterNumber: Int? = null,

	@field:SerializedName("alternative_title")
	val alternativeTitle: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("rank")
	val rank: Int? = null,

	@field:SerializedName("view_count")
	val viewCount: Int? = null,

	@field:SerializedName("country_id")
	val countryId: String? = null,

	@field:SerializedName("status")
	val status: Int? = null
)

data class ArtistItems(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class AuthorItems(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)
