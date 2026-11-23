package com.komik.komik.service.data.Database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.komik.komik.service.data.dao.ComicDao
import com.komik.komik.service.data.dao.DownloadDao
import com.komik.komik.service.data.dao.ExtensiDao
import com.komik.komik.service.data.dao.LibraryDao
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.genreDao
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.dao.ChapterUpdateDao
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.ChapterUpdateLog
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.DownloadChapter
import com.komik.komik.service.data.model.DownloadLog
import com.komik.komik.service.data.model.Extensi
import com.komik.komik.service.data.model.Genre
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.Riwayats

@Database(
    entities = [
        Riwayats::class,
        Comic::class,
        Chapter::class,
        Genre::class,
        Library::class,
        Extensi::class,
        DownloadChapter::class,
        DownloadLog::class,
        ChapterUpdateLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DatabaseRiwayats : RoomDatabase() {

    abstract fun riwayatdao(): riwayatRelationDao
    abstract fun comicdao(): ComicDao
    abstract fun chapterdao(): chapterDao
    abstract fun genreDao(): genreDao
    abstract fun libraryDao(): LibraryDao
    abstract fun extensiDao(): ExtensiDao
    abstract fun downloadDao(): DownloadDao
    abstract fun chapterUpdateDao(): ChapterUpdateDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Buat tabel chapter_update_logs jika belum ada
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chapter_update_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        comicUrl TEXT NOT NULL,
                        comicTitle TEXT NOT NULL,
                        coverUrl TEXT NOT NULL,
                        sourceName TEXT NOT NULL,
                        chapterTitle TEXT NOT NULL,
                        chapterUrl TEXT NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
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
