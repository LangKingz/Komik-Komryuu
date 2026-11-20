package com.komik.komik.service.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.komik.komik.service.data.model.Riwayat

@Dao
interface riwayatDao {

    @Query("SELECT * FROM Riwayat")
    fun getAllRiwayatLiveData(): LiveData<List<Riwayat>>

    @Query("SELECT * FROM Riwayat ORDER BY createdAt DESC")
    suspend fun getAllRiwayatList(): List<Riwayat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiwayat(riwayat: Riwayat)

    @Update
    suspend fun updateRiwayat(riwayat: Riwayat)

    @Query("DELETE FROM Riwayat WHERE id = :id")
    suspend fun deleteRiwayat(id: Int)

    @Query("DELETE FROM Riwayat")
    suspend fun deleteAllRiwayat()

    @Query("SELECT * FROM Riwayat WHERE title =:title AND location =:location LIMIT 1")
    suspend fun getRiwayatByTitle(title: String, location: String): Riwayat?

    @Query("SELECT * FROM Riwayat ORDER BY id DESC LIMIT 1")
    suspend fun getLastRiwayat(): Riwayat?

    // --- Statistik Membaca (Reading Insights) ---
    @Query("SELECT COUNT(*) FROM Riwayat")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM Riwayat WHERE createdAt >= :sinceTimestamp")
    suspend fun getWeeklyCount(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(DISTINCT title) FROM Riwayat")
    suspend fun getTotalTitlesCount(): Int

    @Query("SELECT createdAt FROM Riwayat ORDER BY createdAt DESC")
    suspend fun getAllTimestamps(): List<Long>

    @Query("SELECT location FROM Riwayat GROUP BY location ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getMostReadSource(): String?

    @Query("UPDATE Riwayat SET location = :newLocation, url = :newUrl WHERE title = :title")
    suspend fun migrateRiwayatSource(title: String, newLocation: String, newUrl: String)
}
