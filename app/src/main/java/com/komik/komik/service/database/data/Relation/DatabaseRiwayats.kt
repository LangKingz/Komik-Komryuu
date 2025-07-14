package com.komik.komik.service.database.data.Relation

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.komik.komik.service.database.dao.ComicDao
import com.komik.komik.service.database.dao.chapterDao
import com.komik.komik.service.database.dao.riwayatRelationDao
import com.komik.komik.service.database.model.Chapter
import com.komik.komik.service.database.model.Comic
import com.komik.komik.service.database.model.Riwayats

@Database(entities = [Riwayats::class,Comic::class,Chapter::class], version = 1, exportSchema = false)
abstract class DatabaseRiwayats: RoomDatabase() {

    abstract fun riwayatdao(): riwayatRelationDao
    abstract fun comicdao(): ComicDao
    abstract fun chapterdao(): chapterDao

    companion object{
        @Volatile
        private var INSTANCE: DatabaseRiwayats? = null

        fun getDatabase(context: Context):DatabaseRiwayats{
            return INSTANCE?: synchronized(this){
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseRiwayats::class.java,
                    "riwayat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }

    }

}