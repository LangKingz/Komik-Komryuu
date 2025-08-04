package com.komik.komik.service.parser.model

import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Genre

data class detailComic(
    val title: String,
    val imageUrl: String,
    val description: String,
    val author: String,
    val location: String

)

data class ParsedData(
    val url: String,
    val title: String,
    val alternativeTitle: String? = null,
    val imageUrl: String,
    val localImageUri: String? = null,
    val description: String,
    val author: String? = null,
    val location: String,
    val tags : String? = null,
    val totalView : String? = null,
    val type: String = "Manhwa",
    val status: String = "Ongoing",
    val rating: String? = null,
    val chapters: List<Chapter>,
    val genres: List<Genre>
)

data class imageModel(
    val index: Int,
    val imageUrl: List<getImages>
)

data class getListUpdate(
    val index : Int,
    val title : String,
    val urlImage : String,
    val link : String,
    val location: String? = null,
    val tagModel : String? = null,
    val chapter : String? = null
)

data class getChapter(
    val index : Int,
    val title : String,
    val date : String,
    val link : String,
)

data class getImages(
    val index : Int,
    val image: String,
)