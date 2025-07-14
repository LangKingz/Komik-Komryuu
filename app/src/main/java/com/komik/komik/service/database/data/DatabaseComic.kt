package com.komik.komik.service.database.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.komik.komik.service.database.dao.ComicDao
import com.komik.komik.service.database.model.Comic

@Database(entities = [Comic::class], version = 2, exportSchema = false)
abstract class DatabaseComic : RoomDatabase() {

    abstract fun comicdao(): ComicDao

    companion object {
        @Volatile
        private var INSTANCE: DatabaseComic? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Instruksi untuk "tukang" (database)
                // 1. Tambah kolom alternativeTitle, tipe datanya TEXT
                database.execSQL("ALTER TABLE comic ADD COLUMN alternativeTitle TEXT")
                // 2. Tambah kolom type, tipe TEXT, gak boleh null, defaultnya 'Manhwa'
                database.execSQL("ALTER TABLE comic ADD COLUMN type TEXT ")
                // 3. Tambah kolom status, tipe TEXT, gak boleh null, defaultnya 'Ongoing'
                database.execSQL("ALTER TABLE comic ADD COLUMN status TEXT ")
            }
        }

        fun getDatabase(context: Context): DatabaseComic {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseComic::class.java,
                    "comic_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }

}