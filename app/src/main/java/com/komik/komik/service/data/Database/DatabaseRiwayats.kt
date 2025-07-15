package com.komik.komik.service.data.Database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.genreDao
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.Riwayats

@Database(
    entities = [Riwayats::class, Comic::class, Chapter::class, Genre::class,Library::class],
    version = 1,
    exportSchema = false
)
abstract class DatabaseRiwayats : RoomDatabase() {

    abstract fun riwayatdao(): riwayatRelationDao
    abstract fun comicdao(): ComicDao
    abstract fun chapterdao(): chapterDao
    abstract fun genreDao(): genreDao


    companion object {
        @Volatile
        private var INSTANCE: DatabaseRiwayats? = null

        fun getDatabase(context: Context): DatabaseRiwayats {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseRiwayats::class.java,
                    "riwayat_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }

}