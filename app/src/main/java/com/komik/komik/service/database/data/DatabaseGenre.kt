package com.komik.komik.service.database.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.komik.komik.service.database.dao.genreDao
import com.komik.komik.service.database.model.Genre

@Database(entities = [Genre::class], version = 1, exportSchema = false)
abstract class DatabaseGenre:RoomDatabase() {

    abstract fun genredao(): genreDao

    companion object{
        @Volatile
        private var INSTANCE: DatabaseGenre? = null

        fun getDatabase(context:Context):DatabaseGenre{
            return INSTANCE?: synchronized(this){
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseGenre::class.java,
                    "genre_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }

    }

}