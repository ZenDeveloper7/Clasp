package com.zen.clasp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `captures` ADD COLUMN `extractedText` TEXT")
        db.execSQL(
            "ALTER TABLE `captures` ADD COLUMN `extractionState` TEXT NOT NULL " +
                "DEFAULT 'NOT_APPLICABLE'"
        )
        db.execSQL("ALTER TABLE `captures` ADD COLUMN `extractionErrorCode` TEXT")
        db.execSQL(
            "ALTER TABLE `captures` ADD COLUMN `contentRevision` INTEGER NOT NULL DEFAULT 1"
        )
        db.execSQL(
            "UPDATE `captures` SET `extractionState` = 'PENDING' WHERE `type` = 'IMAGE'"
        )
    }
}
