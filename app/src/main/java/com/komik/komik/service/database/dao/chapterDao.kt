package com.komik.komik.service.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.komik.komik.service.database.model.Chapter


@Dao
interface chapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Query("UPDATE chapter SET isRead = :isRead WHERE id = :chapterId")
    suspend fun updateIsRead(chapterId: Int, isRead: Boolean)

    @Query("SELECT * FROM Chapter WHERE  comicTitle = :comicTitle ORDER BY id ASC")
    fun getChaptersForComic(comicTitle: String): List<Chapter>

    @Query("SELECT * FROM Chapter WHERE comicTitle = :comicTitle ORDER BY id ASC")
    fun getChaptersForComicLiveData(comicTitle: String): LiveData<List<Chapter>>

    @Query("SELECT * FROM Chapter WHERE title = :comicTitle AND link = :link")
    suspend fun getChapterByLink(comicTitle: String, link: String): Chapter?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChapter(chapter: Chapter)

    @Query("DELETE FROM Chapter WHERE title = :comicTitle")
    suspend fun deleteChapters(comicTitle: String)

    @Query("DELETE FROM chapter")
    suspend fun deleteAllChapters()

    @Query("SELECT * FROM chapter")
    suspend fun getAllChapters(): List<Chapter>
}