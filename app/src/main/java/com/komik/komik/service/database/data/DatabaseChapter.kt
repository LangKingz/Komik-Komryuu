package com.komik.komik.service.database.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.komik.komik.service.database.dao.chapterDao
import com.komik.komik.service.database.dao.pustakaDao
import com.komik.komik.service.database.model.Chapter
import com.komik.komik.service.database.model.Pustaka

@Database(entities = [Chapter::class], version = 2, exportSchema = false)
abstract class DatabaseChapter: RoomDatabase() {

    abstract fun chapterdao(): chapterDao

    companion object{
        @Volatile
        private var INSTANCE: DatabaseChapter? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Chapter ADD COLUMN location TEXT")
            }
        }

        fun getDatabase(context: Context):DatabaseChapter{
            return INSTANCE?: synchronized(this){
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DatabaseChapter::class.java,
                    "chapter_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }

}