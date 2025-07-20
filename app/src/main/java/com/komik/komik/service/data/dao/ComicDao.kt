package com.komik.komik.service.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM comic WHERE url = :url")
    fun getComicByUrl(url: String): Comic

    @Query("DELETE FROM comic ")
    fun deleteAllComic()

    @Transaction
    suspend fun insertComicwithChapterAndgenre(comic: Comic,genre: List<Genre>,chapter: List<Chapter>){
        insertComic(comic)
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



}