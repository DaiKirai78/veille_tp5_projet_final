package com.example.veille_tp5_projet_final.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [StepRecord::class, ObjectifRecord::class, TimerRecord::class], version = 2)
abstract class StepDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao

    companion object {
        @Volatile
        private var INSTANCE: StepDatabase? = null

        fun getDatabase(context: Context): StepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StepDatabase::class.java,
                    "step_database"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `timer_record` (" +
                            "`date` TEXT PRIMARY KEY NOT NULL, " +
                            "`timeElapsed` INTEGER NOT NULL)"
                )
            }
        }
    }
}