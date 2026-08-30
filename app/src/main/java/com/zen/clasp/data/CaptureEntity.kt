package com.zen.clasp.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "captures",
    indices = [
        Index("createdAt"),
        Index("type"),
        Index("isFavorite"),
        Index("deletionState")
    ]
)
data class CaptureEntity(
    @PrimaryKey val id: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sourcePackage: String?,
    val originalText: String?,
    val userTitle: String?,
    val userNote: String?,
    val isFavorite: Boolean,
    val processingState: String,
    val deletionState: String,
    val errorCode: String?
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("captureId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val relativePath: String,
    val originalDisplayName: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String
)

data class CaptureWithAttachments(
    @Embedded val capture: CaptureEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "captureId"
    )
    val attachments: List<AttachmentEntity>
)
