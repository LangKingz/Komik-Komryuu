package com.komik.komik.service.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.komik.komik.service.data.model.Riwayat


@Dao
interface riwayatDao {

    @Query("SELECT * FROM Riwayat")
    fun getAllRiwayatLiveData(): LiveData<List<Riwayat>>

    @Insert
    suspend fun insertRiwayat(riwayat: Riwayat)

    @Update
    suspend fun updateRiwayat(riwayat: Riwayat)

    @Query("DELETE FROM Riwayat WHERE id = :id")
    suspend fun deleteRiwayat(id: Int)

    @Query("SELECT * FROM Riwayat WHERE title =:title AND location =:location LIMIT 1")
    suspend fun getRiwayatByTitle(title: String, location: String): Riwayat?

    @Query("SELECT * FROM Riwayat ORDER BY id DESC LIMIT 1")
    suspend fun getLastRiwayat(): Riwayat?


}