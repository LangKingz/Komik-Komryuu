package com.komik.komik.service.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.komik.komik.service.data.model.RiwayatSource

@Dao
interface sourceDao {

    @Query("SELECT * FROM riwayatsource ORDER BY id DESC LIMIT 1")
     fun getRiwayatSourceLive(): LiveData<List<RiwayatSource>>

    @Query("SELECT * FROM riwayatsource ORDER BY id DESC LIMIT 1")
    suspend fun getRiwayatSource(): RiwayatSource?

    @Insert
    suspend fun insertRiwayatSource(riwayatSource: RiwayatSource)

    @Update
    suspend fun updateRiwayatSource(riwayatSource: RiwayatSource)


}