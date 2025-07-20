package com.komik.komik.service.data.Database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.dao.LibraryDao
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
    version = 2,
    exportSchema = false
)
abstract class DatabaseRiwayats : RoomDatabase() {

    abstract fun riwayatdao(): riwayatRelationDao
    abstract fun comicdao(): ComicDao
    abstract fun chapterdao(): chapterDao
    abstract fun genreDao(): genreDao
    abstract fun libraryDao(): LibraryDao


    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Tambah kolom isFavorite ke tabel Library.
                // Tipe datanya INTEGER (representasi Boolean di SQLite), tidak boleh null, dan defaultnya 0 (false).
                db.execSQL("ALTER TABLE Library ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                // 2. Tambah kolom isRead ke tabel Library.
                // Logikanya sama seperti isFavorite.
                db.execSQL("ALTER TABLE Library ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0")
                // 3. Tambah kolom pageRead ke tabel RiwayatRelasi.
                // Tipe datanya INTEGER dan boleh null, jadi tidak perlu default value.
                db.execSQL("ALTER TABLE RiwayatRelasi ADD COLUMN pageRead INTEGER")

                // 4. Tambah kolom totalPage ke tabel RiwayatRelasi.
                // Sama seperti pageRead, boleh null.
                db.execSQL("ALTER TABLE RiwayatRelasi ADD COLUMN totalPage INTEGER")

                // 5. Tambah kolom isRead ke tabel RiwayatRelasi.
                // Tipe data INTEGER, tidak boleh null, defaultnya 0 (false).
                db.execSQL("ALTER TABLE RiwayatRelasi ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: DatabaseRiwayats? = null

        fun getDatabase(context: Context): DatabaseRiwayats {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseRiwayats::class.java,
                    "riwayat_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }

}