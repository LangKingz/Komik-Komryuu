package com.komik.komik.service.database.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.komik.komik.service.database.dao.pustakaDao
import com.komik.komik.service.database.model.Pustaka

@Database(entities = [Pustaka::class], version = 1, exportSchema = false)
abstract class DatabasePustaka:RoomDatabase() {

    abstract fun pustakadao(): pustakaDao

    companion object{
        @Volatile
        private var INSTANCE: DatabasePustaka? = null

        fun getDatabase(context:Context):DatabasePustaka{
            return INSTANCE?: synchronized(this){
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabasePustaka::class.java,
                    "pustaka_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }

    }

}