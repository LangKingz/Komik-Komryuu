package com.komik.komik.service.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation


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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val link: String,
    val comicTitle: String,
    val location: String?=null,
    val date: String,
    val numChapter : Double = 0.0,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "Riwayat")
data class Riwayat(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val chapter: String,
    val url: String? = null,
    val imageUrl: String? = null,
    val urlWebsite: String? = null,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "RiwayatSource")
data class RiwayatSource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val extensi: String,
    val url: String,
    val imageUrl: String? = null,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "Genre")
data class Genre(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val comicTitle: String,
    val genre: String,
    val location: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comic")
data class Comic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val alternativeTitle: String? = null,
    val url: String,
    val rating: String,
    val description: String,
    val imageUrl: String,
    val author: String,
    val type: String? = null,
    val tags : String? = null,
    val totalView : String? = null,
    val status: String? = null,
    val location: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSelected: Boolean = false
)


@Entity(
    tableName = "Comic",
    indices = [Index(
        value = ["url"],
        unique = true
    )] // URL harus unik biar nggak ada komik duplikat
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

@Entity(
    tableName = "RiwayatRelasi",
    foreignKeys = [
        ForeignKey(
            entity = Comic::class,         // Tabel induknya adalah Comics
            parentColumns = ["id"],       // Kolom Primary Key di tabel Comics
            childColumns = ["comicId"],     // Kolom Foreign Key di tabel Riwayat ini
            onDelete = ForeignKey.CASCADE // Jika komik dihapus, riwayatnya ikut terhapus
        ),
        ForeignKey(
            entity = Chapter::class,        // Tabel induknya adalah Chapter
            parentColumns = ["id"],       // Kolom Primary Key di tabel Chapter
            childColumns = ["chapterId"],   // Kolom Foreign Key di tabel Riwayat ini
            onDelete = ForeignKey.CASCADE // Jika chapter dihapus, riwayatnya ikut terhapus
        )
    ],
    indices = [Index("comicId"), Index("chapterId")]
)
data class Riwayats(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val comicId: Int,
    val chapterId: Int,
    val pageRead: Int? = null,
    val totalPage: Int? = null,
    val isRead: Boolean = false,
    val readAt: Long = System.currentTimeMillis()
)

data class RiwayatDetail(
    @Embedded val riwayat: Riwayats,
    @Relation(
        parentColumn = "comicId",
        entityColumn = "id"
    )
    val comic: Comic,
    @Relation(
        parentColumn = "chapterId",
        entityColumn = "id"
    )
    val chapter: Chapter
)

@Entity(
    tableName = "Library",
    foreignKeys = [ForeignKey(
        entity = Comic::class,
        parentColumns = ["id"],
        childColumns = ["comicId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("comicId")]
)
data class Library(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val comicId: Int,
    val imageLokal: String? = null,
    val isSelected: Boolean = false,
    val isFavorite: Boolean = false,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)


data class LibraryDetail(
    @Embedded val library: Library,
    @Relation(
        parentColumn = "comicId",
        entityColumn = "id"
    )
    val comic: Comic
)