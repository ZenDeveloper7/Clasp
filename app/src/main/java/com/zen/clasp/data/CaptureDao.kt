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
    @Query("SELECT * FROM captures WHERE deletionState = 'DELETING'")
    suspend fun getInterruptedDeletions(): List<CaptureWithAttachments>

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
        SET userTitle = :title, userNote = :note, updatedAt = :updatedAt
        WHERE id = :captureId
        """
    )
    suspend fun updateEditableFields(captureId: String, title: String?, note: String?, updatedAt: Long)

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

    @Query("DELETE FROM captures WHERE id = :captureId")
    suspend fun deleteById(captureId: String)
}
