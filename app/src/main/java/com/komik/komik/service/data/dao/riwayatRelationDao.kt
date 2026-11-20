package com.komik.komik.service.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.data.model.Riwayats

@Dao
interface riwayatRelationDao {
    @Transaction
    @Query("SELECT * FROM RiwayatRelasi ORDER BY readAt DESC")
    fun getRiwayatDetail(): LiveData<List<RiwayatDetail>>

    @Transaction
    @Query("SELECT * FROM RiwayatRelasi ORDER BY readAt DESC")
    suspend fun getAllRiwayatDetailList(): List<RiwayatDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiwayatRelasi(riwayatRelasi: Riwayats)

    @Update
    suspend fun updateRiwayatRelasi(riwayatRelasi: Riwayats)

    @Query("DELETE FROM RiwayatRelasi WHERE id = :id")
    suspend fun deleteRiwayatRelasi(id: Int)

    @Query("DELETE FROM RiwayatRelasi")
    suspend fun deleteAllRiwayatRelasi()

    @Query("SELECT * FROM RiwayatRelasi WHERE comicId = :chapterId LIMIT 1")
    suspend fun getRiwayatByChapterId(chapterId: Int): Riwayats?

    @Query("SELECT * FROM RiwayatRelasi ORDER BY readAt DESC LIMIT 1")
    fun getLastRiwayats(): LiveData<RiwayatDetail>

    @Query("SELECT * FROM RiwayatRelasi WHERE comicId = :comicId ORDER BY readAt DESC LIMIT 1")
    fun getLastRiwayatsByComicId(comicId: Int): LiveData<RiwayatDetail>

    @Query("UPDATE RiwayatRelasi SET pageRead = :pageRead, totalpage = :totalpage WHERE comicId = :id")
    suspend fun updateProgress(id: Int, pageRead: Int, totalpage: Int)

    // --- Statistik Membaca (Reading Insights) ---
    @Query("SELECT COUNT(*) FROM RiwayatRelasi WHERE readAt >= :sinceTimestamp")
    suspend fun getWeeklyChaptersRead(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM RiwayatRelasi")
    suspend fun getTotalChaptersRead(): Int

    @Query("SELECT COUNT(DISTINCT comicId) FROM RiwayatRelasi")
    suspend fun getTotalComicsRead(): Int

    @Query("SELECT readAt FROM RiwayatRelasi ORDER BY readAt DESC")
    suspend fun getAllReadTimestamps(): List<Long>

    @Query("""
        SELECT comic.location 
        FROM RiwayatRelasi 
        INNER JOIN comic ON RiwayatRelasi.comicId = comic.id 
        GROUP BY comic.location 
        ORDER BY COUNT(RiwayatRelasi.id) DESC 
        LIMIT 1
    """)
    suspend fun getMostReadSource(): String?

    @Query("""
        SELECT Genre.genre
        FROM RiwayatRelasi
        INNER JOIN Genre ON Genre.comicTitle = (SELECT title FROM comic WHERE comic.id = RiwayatRelasi.comicId LIMIT 1)
        GROUP BY Genre.genre
        ORDER BY COUNT(RiwayatRelasi.id) DESC
        LIMIT 1
    """)
    suspend fun getMostReadGenre(): String?

    @Query("UPDATE RiwayatRelasi SET comicId = :newComicId, chapterId = :newChapterId WHERE comicId = :oldComicId")
    suspend fun migrateRiwayatRelasi(oldComicId: Int, newComicId: Int, newChapterId: Int)
}
