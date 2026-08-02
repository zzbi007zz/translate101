package com.example.zaloauto.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageLogDao {

    @Insert
    suspend fun insert(log: MessageLogEntity): Long

    @Query("SELECT * FROM message_logs WHERE scheduledMessageId = :scheduledMessageId ORDER BY id DESC")
    fun getByScheduledMessageIdFlow(scheduledMessageId: Long): Flow<List<MessageLogEntity>>
}
