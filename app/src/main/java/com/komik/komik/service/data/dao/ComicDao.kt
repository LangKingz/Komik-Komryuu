package com.komik.komik.service.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.LibraryDetail


@Dao
interface ComicDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertComic(comic: Comic)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenre(genre: List<Genre>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChapter(chapter: List<Chapter>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComicId(comic: Comic): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLatestChapter(chapter: List<Chapter>)

    @Query("SELECT * FROM Chapter WHERE comicTitle = :comicTitle AND location = :location")
    suspend fun getChapterByLink(comicTitle: String,location: String): List<Chapter>

    @Query("SELECT * FROM comic WHERE url = :url")
    fun getComicByUrl(url: String): Comic

    @Query("DELETE FROM comic ")
    fun deleteAllComic()

    @Update
    suspend fun updateComic(comic: Comic)

    @Transaction
    suspend fun insertComicwithChapterAndgenre(genre: List<Genre>,chapter: List<Chapter>){
        insertGenre(genre)
        insertChapter(chapter)
    }

    @Query("DELETE FROM Chapter WHERE comicTitle = :comicTitle")
    suspend fun deleteChaptersByTitle(comicTitle: String)

    @Transaction
    suspend fun updateChaptersForComic(comicTitle: String, newChapters: List<Chapter>,location: String) {
        deleteChaptersByTitle(comicTitle)
        insertChapter(newChapters)

    }

    @Query("SELECT * FROM comic")
    suspend fun getComic(): List<Comic>

    @Query("DELETE FROM comic WHERE id = :id")
    suspend fun deleteComicByUrl(id:Int)

    @Query("DELETE FROM genre WHERE comicTitle = :comicTitle")
    suspend fun deleteGenreByTitle(comicTitle: String)

    @Query("DELETE FROM chapter WHERE comicId = :id")
    suspend fun deleteChapterById(id: Int)





}