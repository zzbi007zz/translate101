package com.example.zaloauto.data.repository

import com.example.zaloauto.data.db.MessageLogEntity
import com.example.zaloauto.data.db.ScheduledMessageDao
import com.example.zaloauto.data.db.ScheduledMessageEntity
import com.example.zaloauto.data.db.MessageLogDao
import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: ScheduledMessageDao,
    private val logDao: MessageLogDao
) {
    fun getAllMessagesFlow(): Flow<List<ScheduledMessageEntity>> = messageDao.getAllFlow()

    fun getPendingMessagesFlow(): Flow<List<ScheduledMessageEntity>> = messageDao.getPendingFlow()

    suspend fun getPendingMessages(): List<ScheduledMessageEntity> = messageDao.getPendingMessages()

    suspend fun getById(id: Long): ScheduledMessageEntity? = messageDao.getById(id)

    suspend fun scheduleMessage(
        targetName: String,
        messageText: String,
        scheduledAt: Long,
        templateId: Long? = null
    ): Long {
        val entity = ScheduledMessageEntity(
            targetName = targetName,
            messageText = messageText,
            scheduledAt = scheduledAt,
            templateId = templateId
        )
        return messageDao.insert(entity)
    }

    suspend fun markSent(id: Long) {
        messageDao.updateStatus(id, ScheduledMessageEntity.STATUS_SENT)
        logDao.insert(
            MessageLogEntity(
                scheduledMessageId = id,
                status = MessageLogEntity.STATUS_SENT,
                sentAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markFailed(id: Long, error: String?) {
        messageDao.updateStatus(id, ScheduledMessageEntity.STATUS_FAILED)
        logDao.insert(
            MessageLogEntity(
                scheduledMessageId = id,
                status = MessageLogEntity.STATUS_FAILED,
                error = error
            )
        )
    }

    suspend fun incrementRetry(id: Long): Int {
        messageDao.incrementRetry(id, ScheduledMessageEntity.STATUS_PENDING)
        logDao.insert(
            MessageLogEntity(
                scheduledMessageId = id,
                status = MessageLogEntity.STATUS_RETRYING,
                retryCount = 1
            )
        )
        val msg = messageDao.getById(id)
        return msg?.retryCount ?: 0
    }

    suspend fun deleteAndCancelAlarm(id: Long) {
        messageDao.delete(id)
    }

    suspend fun cancelMessage(id: Long) {
        messageDao.updateStatus(id, ScheduledMessageEntity.STATUS_CANCELED)
    }

    fun getLogsFlow(messageId: Long): Flow<List<MessageLogEntity>> =
        logDao.getByScheduledMessageIdFlow(messageId)
}
