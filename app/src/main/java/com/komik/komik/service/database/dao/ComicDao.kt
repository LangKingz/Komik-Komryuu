package com.komik.komik.service.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.komik.komik.service.database.model.Comic


@Dao
interface ComicDao {
    @Insert
    suspend fun insertComic(comic: Comic)

    @Query("SELECT * FROM comic WHERE url = :url")
    fun getComicByUrl(url: String): Comic?

    @Query("DELETE FROM comic ")
    fun deleteAllComic()
}