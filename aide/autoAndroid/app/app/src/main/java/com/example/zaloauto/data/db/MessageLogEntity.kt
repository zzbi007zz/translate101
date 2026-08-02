package com.example.zaloauto.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_logs",
    foreignKeys = [
        ForeignKey(
            entity = ScheduledMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduledMessageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scheduledMessageId")]
)
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledMessageId: Long,
    val status: String,
    val sentAt: Long? = null,
    val error: String? = null,
    val retryCount: Int = 0
) {
    companion object {
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_RETRYING = "RETRYING"
    }
}
