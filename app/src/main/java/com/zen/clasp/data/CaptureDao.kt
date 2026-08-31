package com.zen.clasp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Transaction
    @Query("SELECT * FROM captures ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CaptureWithAttachments>>

    @Transaction
    @Query("SELECT * FROM captures WHERE id = :captureId")
    suspend fun getById(captureId: String): CaptureWithAttachments?

    @Transaction
    @Query("SELECT * FROM captures WHERE id IN (:captureIds)")
    suspend fun getByIds(captureIds: List<String>): List<CaptureWithAttachments>

    @Transaction
    @Query("SELECT * FROM captures ORDER BY createdAt DESC")
    suspend fun getAll(): List<CaptureWithAttachments>

    @Transaction
    @Query("SELECT * FROM captures WHERE deletionState = 'DELETING'")
    suspend fun getInterruptedDeletions(): List<CaptureWithAttachments>

    @Query(
        """
        SELECT id FROM captures
        WHERE type = 'IMAGE' AND extractionState = 'PENDING' AND deletionState = 'ACTIVE'
        """
    )
    suspend fun getPendingExtractionIds(): List<String>

    @Insert
    suspend fun insertCapture(capture: CaptureEntity)

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Transaction
    suspend fun insert(capture: CaptureEntity, attachment: AttachmentEntity?) {
        insertCapture(capture)
        attachment?.let { insertAttachment(it) }
    }

    @Query(
        """
        UPDATE captures
        SET userTitle = :title,
            userNote = :note,
            contentRevision = contentRevision + 1,
            updatedAt = :updatedAt
        WHERE id = :captureId
        """
    )
    suspend fun updateEditableFieldsRow(
        captureId: String,
        title: String?,
        note: String?,
        updatedAt: Long
    )

    @Transaction
    suspend fun updateEditableFields(captureId: String, title: String?, note: String?, updatedAt: Long) {
        updateEditableFieldsRow(captureId, title, note, updatedAt)
    }

    @Query("UPDATE captures SET isFavorite = :favorite, updatedAt = :updatedAt WHERE id = :captureId")
    suspend fun updateFavorite(captureId: String, favorite: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE captures
        SET deletionState = :state, errorCode = :errorCode, updatedAt = :updatedAt
        WHERE id = :captureId
        """
    )
    suspend fun updateDeletionState(
        captureId: String,
        state: String,
        errorCode: String?,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE captures
        SET extractionState = :state, extractionErrorCode = :errorCode, updatedAt = :updatedAt
        WHERE id = :captureId
        """
    )
    suspend fun updateExtractionState(
        captureId: String,
        state: String,
        errorCode: String?,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE captures
        SET extractedText = :text,
            extractionState = :state,
            extractionErrorCode = NULL,
            contentRevision = contentRevision + 1,
            updatedAt = :updatedAt
        WHERE id = :captureId
        """
    )
    suspend fun updateExtractionResultRow(
        captureId: String,
        text: String?,
        state: String,
        updatedAt: Long
    )

    @Transaction
    suspend fun updateExtractionResult(
        captureId: String,
        text: String?,
        state: String,
        updatedAt: Long
    ) {
        updateExtractionResultRow(captureId, text, state, updatedAt)
    }

    @Query("DELETE FROM captures WHERE id = :captureId")
    suspend fun deleteCaptureRow(captureId: String)

    @Transaction
    suspend fun deleteById(captureId: String) {
        deleteCaptureRow(captureId)
    }
}
