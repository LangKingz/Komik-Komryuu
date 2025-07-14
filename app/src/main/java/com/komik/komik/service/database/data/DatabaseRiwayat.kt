package com.komik.komik.service.database.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.komik.komik.service.database.dao.pustakaDao
import com.komik.komik.service.database.dao.riwayatDao
import com.komik.komik.service.database.dao.sourceDao
import com.komik.komik.service.database.model.Pustaka
import com.komik.komik.service.database.model.Riwayat
import com.komik.komik.service.database.model.RiwayatSource

@Database(entities = [Riwayat::class], version = 1, exportSchema = false)
abstract class DatabaseRiwayat: RoomDatabase() {

    abstract fun riwayatdao(): riwayatDao

    companion object{
        @Volatile
        private var INSTANCE: DatabaseRiwayat? = null

        fun getDatabase(context: Context):DatabaseRiwayat{
            return INSTANCE?: synchronized(this){
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseRiwayat::class.java,
                    "riwayat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }

    }

}

@Database(entities = [RiwayatSource::class], version = 1, exportSchema = false)
abstract class DatabaseRiwayatSource: RoomDatabase() {

    abstract fun riwayatdao(): sourceDao

    companion object{
        @Volatile
        private var INSTANCE: DatabaseRiwayatSource? = null

        fun getDatabase(context: Context):DatabaseRiwayatSource{
            return INSTANCE?: synchronized(this){
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseRiwayatSource::class.java,
                    "riwayatSource_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }

    }

}