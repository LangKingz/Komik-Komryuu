package com.komik.komik.service.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.LibraryDetail

@Dao
interface LibraryDao {
    @Transaction
    @Query("SELECT * FROM Library ORDER BY createdAt DESC")
    fun getLibrary(): LiveData<List<LibraryDetail>>

    @Query("SELECT COUNT(*) FROM Library")
    fun getLibraryCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLibrary(library: Library)

    @Query("DELETE FROM Library WHERE comicId = :title")
    suspend fun deleteLibrary(title: Int)

    @Query("SELECT * FROM Library WHERE comicId = :comicId")
    suspend fun getLibraryByComicId(comicId: Int): Library?

    @Transaction
    @Query("""
        SELECT * FROM Library
        INNER JOIN comic ON Library.comicId = Comic.id
        WHERE (comic.title LIKE '%' || :query || '%')
        ORDER BY Library.createdAt DESC
    """)
    fun searchFavoriteLibraryComics(query: String): LiveData<List<LibraryDetail>>

    @Query("DELETE FROM Library")
    suspend fun DeleteALlLibrary()

    @Transaction
    @Query("SELECT * FROM Library ORDER BY createdAt DESC")
    suspend fun getAllLibraryList(): List<LibraryDetail>

    @Query("UPDATE Library SET comicId = :newComicId WHERE comicId = :oldComicId")
    suspend fun migrateLibrary(oldComicId: Int, newComicId: Int)
}