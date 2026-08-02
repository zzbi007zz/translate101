package com.example.zaloauto.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetName: String,
    val messageText: String,
    val scheduledAt: Long,
    val status: String = STATUS_PENDING,
    val templateId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELED = "CANCELED"
    }
}
