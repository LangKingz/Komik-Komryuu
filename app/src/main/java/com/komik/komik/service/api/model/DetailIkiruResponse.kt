package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class DetailIkiruResponse(

	@field:SerializedName("code")
	val code: Int? = null,

	@field:SerializedName("data")
	val data: DataDetailIkiru? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class DataDetailIkiru(

	@field:SerializedName("isBookmarked")
	val isBookmarked: Any? = null,

	@field:SerializedName("metadata")
	val metadata: Metadata? = null,

	@field:SerializedName("wpId")
	val wpId: Any? = null,

	@field:SerializedName("searchKeywords")
	val searchKeywords: String? = null,

	@field:SerializedName("chapters")
	val chapters: Chapters? = null,

	@field:SerializedName("backgroundImage")
	val backgroundImage: Any? = null,

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("chaptersCount")
	val chaptersCount: Int? = null,

	@field:SerializedName("firstChapter")
	val firstChapter: FirstChapter? = null,

	@field:SerializedName("published")
	val published: Boolean? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("authorId")
	val authorId: String? = null,

	@field:SerializedName("type")
	val type: String? = null,

	@field:SerializedName("lastChapter")
	val lastChapter: Double? = null,

	@field:SerializedName("isProject")
	val isProject: Boolean? = null,

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("lastUpdated")
	val lastUpdated: String? = null,

	@field:SerializedName("featuredImage")
	val featuredImage: String? = null,

	@field:SerializedName("latestChapter")
	val latestChapter: LatestChapter? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("isAdult")
	val isAdult: Boolean? = null,

	@field:SerializedName("slug")
	val slug: String? = null,

	@field:SerializedName("status")
	val status: String? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
)

data class FirstChapter(

	@field:SerializedName("number")
	val number: Double? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("label")
	val label: Any? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
)

data class AuthorItemIkiru(

	@field:SerializedName("imageUrl")
	val imageUrl: Any? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("biography")
	val biography: Any? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class GenreItemIkirus(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("description")
	val description: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class Chapters(

	@field:SerializedName("chapters")
	val chapters: List<ChaptersItem?>? = null,

	@field:SerializedName("hasMore")
	val hasMore: Boolean? = null,

	@field:SerializedName("count")
	val count: Int? = null,

	@field:SerializedName("lastIndex")
	val lastIndex: String? = null
)

data class LatestChapter(

	@field:SerializedName("number")
	val number: Double? = null,

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("downloadUrl")
	val downloadUrl: Any? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("label")
	val label: Any? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("views")
	val views: Int? = null,

	@field:SerializedName("downloadCount")
	val downloadCount: Int? = null,

	@field:SerializedName("slug")
	val slug: String? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
)

data class Metadata(

	@field:SerializedName("monthlyViews")
	val monthlyViews: Int? = null,

	@field:SerializedName("weeklyViews")
	val weeklyViews: Int? = null,

	@field:SerializedName("alternateTitles")
	val alternateTitles: List<String?>? = null,

	@field:SerializedName("artist")
	val artist: List<ArtistItemIKiru?>? = null,

	@field:SerializedName("year")
	val year: Int? = null,

	@field:SerializedName("author")
	val author: List<AuthorItemIkiru?>? = null,

	@field:SerializedName("galleryUrl")
	val galleryUrl: List<Any?>? = null,

	@field:SerializedName("score")
	val score: Int? = null,

	@field:SerializedName("dailyViews")
	val dailyViews: Int? = null,

	@field:SerializedName("genre")
	val genre: List<GenreItemIkirus?>? = null,

	@field:SerializedName("rank")
	val rank: Int? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("views")
	val views: Int? = null
)

data class ChaptersItem(

	@field:SerializedName("number")
	val number: Double? = null,

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("downloadUrl")
	val downloadUrl: Any? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("label")
	val label: Any? = null,

	@field:SerializedName("title")
	val title: String? = null,

	@field:SerializedName("views")
	val views: Int? = null,

	@field:SerializedName("downloadCount")
	val downloadCount: Int? = null,

	@field:SerializedName("slug")
	val slug: String? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
)

data class ArtistItemIKiru(

	@field:SerializedName("imageUrl")
	val imageUrl: Any? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("biography")
	val biography: Any? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)
