package com.zen.clasp.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClaspMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClaspDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesCaptureAndAddsExtractionState() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO captures(
                    id, type, createdAt, updatedAt, sourcePackage, originalText,
                    userTitle, userNote, isFavorite, processingState, deletionState, errorCode
                ) VALUES(
                    'capture-1', 'IMAGE', 1, 1, NULL, 'Legacy searchable text',
                    'Legacy title', NULL, 0, 'STORED', 'ACTIVE', NULL
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { database ->
            database.query(
                "SELECT extractionState, contentRevision FROM captures WHERE id = 'capture-1'"
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("PENDING", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }
        }
    }

    private companion object {
        const val TEST_DB = "clasp-migration-test"
    }
}
