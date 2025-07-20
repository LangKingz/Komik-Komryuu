package com.komik.komik.service.api.model

import com.google.gson.annotations.SerializedName

data class ShinigamiHomeExplore(

    @field:SerializedName("data")
	val data: List<DataItem?>? = null,

    @field:SerializedName("meta")
	val meta: Meta? = null,

    @field:SerializedName("message")
	val message: String? = null,

    @field:SerializedName("facet")
	val facet: Facet? = null,

    @field:SerializedName("retcode")
	val retcode: Int? = null
)

data class TaxonomyTypeName(

	@field:SerializedName("Project")
	val project: Int? = null
)

data class Taxonomy(

    @field:SerializedName("Artist")
	val artist: List<ArtistItem?>? = null,

    @field:SerializedName("Type")
	val type: List<TypeItem?>? = null,

    @field:SerializedName("Format")
	val format: List<FormatItem?>? = null,

    @field:SerializedName("Author")
	val author: List<AuthorItem?>? = null,

    @field:SerializedName("Genre")
	val genre: List<GenreItem?>? = null
)

data class DataItem(

    @field:SerializedName("latest_chapter_time")
	val latestChapterTime: String? = null,

    @field:SerializedName("cover_image_url")
	val coverImageUrl: String? = null,

    @field:SerializedName("release_year")
	val releaseYear: String? = null,

    @field:SerializedName("created_at")
	val createdAt: String? = null,

    @field:SerializedName("description")
	val description: String? = null,

    @field:SerializedName("bookmark_count")
	val bookmarkCount: Int? = null,

    @field:SerializedName("cover_portrait_url")
	val coverPortraitUrl: String? = null,

    @field:SerializedName("taxonomy")
	val taxonomy: Taxonomy? = null,

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
	val latestChapterNumber: Any? = null,

    @field:SerializedName("is_recommended")
	val isRecommended: Boolean? = null,

    @field:SerializedName("alternative_title")
	val alternativeTitle: String? = null,

    @field:SerializedName("updated_at")
	val updatedAt: String? = null,

    @field:SerializedName("rank")
	val rank: Int? = null,

    @field:SerializedName("country_id")
	val countryId: String? = null,

    @field:SerializedName("view_count")
	val viewCount: Int? = null,

    @field:SerializedName("status")
	val status: Int? = null
)

data class TaxonomyAuthorName(

	@field:SerializedName("Valen")
	val valen: Int? = null,

	@field:SerializedName("JerryM")
	val jerryM: Int? = null,

	@field:SerializedName("Team Croissant")
	val teamCroissant: Int? = null,

	@field:SerializedName("Denpi")
	val denpi: Int? = null,

	@field:SerializedName("Emong")
	val emong: Int? = null
)

data class TaxonomyArtistName(

	@field:SerializedName("Meona")
	val meona: Int? = null,

	@field:SerializedName("Bill K")
	val billK: Int? = null,

	@field:SerializedName("Midnight Studio")
	val midnightStudio: Int? = null,

	@field:SerializedName("SilSil")
	val silSil: Int? = null,

	@field:SerializedName("Sanjimseung")
	val sanjimseung: Int? = null
)

data class TaxonomyFormatSlug(

	@field:SerializedName("manhwa")
	val manhwa: Int? = null
)

data class Meta(

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

data class TaxonomyTypeSlug(

	@field:SerializedName("project")
	val project: Int? = null
)

data class TypeItem(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class FormatItem(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class TaxonomyGenreSlug(

	@field:SerializedName("adventure")
	val adventure: Int? = null,

	@field:SerializedName("magic")
	val magic: Int? = null,

	@field:SerializedName("fantasy")
	val fantasy: Int? = null,

	@field:SerializedName("action")
	val action: Int? = null,

	@field:SerializedName("shounen")
	val shounen: Int? = null
)

data class ReleaseYear(

	@field:SerializedName("2025")
	val jsonMember2025: Int? = null,

	@field:SerializedName("2024")
	val jsonMember2024: Int? = null,

	@field:SerializedName("2022")
	val jsonMember2022: Int? = null,

	@field:SerializedName("2021")
	val jsonMember2021: Int? = null,

	@field:SerializedName("2020")
	val jsonMember2020: Int? = null
)

data class TaxonomyFormatName(

	@field:SerializedName("Manhwa")
	val manhwa: Int? = null
)

data class ArtistItem(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class Facet(

    @field:SerializedName("taxonomy.Artist.name")
	val taxonomyArtistName: TaxonomyArtistName? = null,

    @field:SerializedName("taxonomy.Genre.slug")
	val taxonomyGenreSlug: TaxonomyGenreSlug? = null,

    @field:SerializedName("taxonomy.Artist.slug")
	val taxonomyArtistSlug: TaxonomyArtistSlug? = null,

    @field:SerializedName("taxonomy.Genre.name")
	val taxonomyGenreName: TaxonomyGenreName? = null,

    @field:SerializedName("taxonomy.Type.name")
	val taxonomyTypeName: TaxonomyTypeName? = null,

    @field:SerializedName("release_year")
	val releaseYear: ReleaseYear? = null,

    @field:SerializedName("taxonomy.Format.slug")
	val taxonomyFormatSlug: TaxonomyFormatSlug? = null,

    @field:SerializedName("taxonomy.Type.slug")
	val taxonomyTypeSlug: TaxonomyTypeSlug? = null,

    @field:SerializedName("taxonomy.Author.name")
	val taxonomyAuthorName: TaxonomyAuthorName? = null,

    @field:SerializedName("taxonomy.Author.slug")
	val taxonomyAuthorSlug: TaxonomyAuthorSlug? = null,

    @field:SerializedName("taxonomy.Format.name")
	val taxonomyFormatName: TaxonomyFormatName? = null
)

data class AuthorItem(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class GenreItem(

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("slug")
	val slug: String? = null
)

data class TaxonomyArtistSlug(

	@field:SerializedName("bill-k")
	val billK: Int? = null,

	@field:SerializedName("meona")
	val meona: Int? = null,

	@field:SerializedName("midnight-studio")
	val midnightStudio: Int? = null,

	@field:SerializedName("silsil")
	val silsil: Int? = null,

	@field:SerializedName("sanjimseung")
	val sanjimseung: Int? = null
)

data class TaxonomyGenreName(

	@field:SerializedName("Action")
	val action: Int? = null,

	@field:SerializedName("Adventure")
	val adventure: Int? = null,

	@field:SerializedName("Magic")
	val magic: Int? = null,

	@field:SerializedName("Fantasy")
	val fantasy: Int? = null,

	@field:SerializedName("Shounen")
	val shounen: Int? = null
)

data class TaxonomyAuthorSlug(

	@field:SerializedName("emong")
	val emong: Int? = null,

	@field:SerializedName("denpi")
	val denpi: Int? = null,

	@field:SerializedName("valen")
	val valen: Int? = null,

	@field:SerializedName("jerrym")
	val jerrym: Int? = null,

	@field:SerializedName("team-croissant")
	val teamCroissant: Int? = null
)
