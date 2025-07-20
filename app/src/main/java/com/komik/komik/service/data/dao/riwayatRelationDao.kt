package com.komik.komik.service.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.data.model.Riwayats

@Dao
interface riwayatRelationDao {
    @Transaction // Penting biar prosesnya aman
    @Query("SELECT * FROM RiwayatRelasi ORDER BY readAt DESC")
    fun getRiwayatDetail(): LiveData<List<RiwayatDetail>>

    @Insert
    suspend fun insertRiwayatRelasi(riwayatRelasi: Riwayats)

    @Update
    suspend fun updateRiwayatRelasi(riwayatRelasi: Riwayats)

    @Query("DELETE FROM RiwayatRelasi WHERE id = :id")
    suspend fun deleteRiwayatRelasi(id: Int)

    @Query("SELECT * FROM RiwayatRelasi WHERE comicId = :chapterId LIMIT 1")
    suspend fun getRiwayatByChapterId(chapterId: Int): Riwayats?

    @Query("SELECT * FROM RiwayatRelasi ORDER BY readAt DESC LIMIT 1")
     fun getLastRiwayats(): LiveData<RiwayatDetail>


}