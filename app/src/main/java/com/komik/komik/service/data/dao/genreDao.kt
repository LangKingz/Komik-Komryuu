package com.komik.komik.service.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.komik.komik.service.data.model.Genre


@Dao
interface genreDao {

    @Query("SELECT * FROM Genre")
    fun getAll(): List<Genre>

    @Query("SELECT * FROM Genre WHERE comicTitle = :comicTitle")
    fun getGenreByComicTitle(comicTitle: String): List<Genre>

    @Query("SELECT * FROM Genre WHERE genre = :genre")
    fun getComicByGenre(genre: String): List<Genre>

    @Insert
    fun insert(genre: List<Genre>)

    @Query("DELETE FROM Genre WHERE comicTitle = :comicTitle")
    fun deleteComic(comicTitle: String)

    @Query("DELETE FROM Genre")
    fun deleteAll()
}