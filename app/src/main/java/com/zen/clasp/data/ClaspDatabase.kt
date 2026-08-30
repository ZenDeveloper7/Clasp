package com.zen.clasp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CaptureEntity::class, AttachmentEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ClaspDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao

    companion object {
        @Volatile
        private var instance: ClaspDatabase? = null

        fun create(context: Context): ClaspDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ClaspDatabase::class.java,
                "clasp.db"
            ).build().also { instance = it }
        }
    }
}
