package com.example.zaloauto.ui.screens.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloauto.ZaloAutoApp
import com.example.zaloauto.data.db.ScheduledMessageEntity
import com.example.zaloauto.data.repository.MessageRepository
import com.example.zaloauto.service.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListUiState(
    val messages: List<ScheduledMessageEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZaloAutoApp.getInstance().database
    private val repo = MessageRepository(db.scheduledMessageDao(), db.messageLogDao())
    private val scheduler = AlarmScheduler(application)

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllMessagesFlow().collect { messages ->
                _uiState.update { it.copy(messages = messages, isLoading = false) }
            }
        }
    }

    fun cancelMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                scheduler.cancelMessage(messageId)
                repo.cancelMessage(messageId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                scheduler.cancelMessage(messageId)
                repo.deleteAndCancelAlarm(messageId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
