package com.komik.komik.service.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "Pustaka")
data class Pustaka(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val urlWebsite: String,
    val imageUrl: String,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "Chapter")
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Int=0,
    val title: String,
    val link: String,
    val comicTitle: String,
    val date : String,
    val location: String?=null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "Riwayat")
data class Riwayat(
    @PrimaryKey(autoGenerate = true) val id: Int=0,
    val title: String,
    val chapter: String,
    val url: String,
    val imageUrl: String,
    val urlWebsite: String?=null,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "RiwayatSource")
data class RiwayatSource(
    @PrimaryKey(autoGenerate = true) val id: Int=0,
    val extensi: String,
    val url: String,
    val imageUrl: String?=null,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "Genre")
data class Genre(
    @PrimaryKey(autoGenerate = true) val id: Int=0,
    val comicTitle: String,
    val genre: String,
    val location: String?=null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comic")
data class Comic(
    @PrimaryKey(autoGenerate = true) val id: Int=0,
    val title: String,
    val alternativeTitle: String? = null,
    val url: String,
    val rating: String,
    val description: String,
    val imageUrl: String,
    val author  : String,
    val type: String? = null,
    val status: String? = null,
    val location: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSelected: Boolean = false
)


@Entity(
    tableName = "Comic",
    indices = [Index(value = ["url"], unique = true)] // URL harus unik biar nggak ada komik duplikat
)
data class Comics(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val imageUrl: String,
    val description: String,
    val author: String,
    val rating: String,
    val location: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)


