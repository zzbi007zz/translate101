package com.example.zaloauto.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloauto.ZaloAutoApp
import com.example.zaloauto.data.db.MessageLogEntity
import com.example.zaloauto.data.db.ScheduledMessageEntity
import com.example.zaloauto.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val message: ScheduledMessageEntity? = null,
    val logs: List<MessageLogEntity> = emptyList(),
    val isLoading: Boolean = true
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZaloAutoApp.getInstance().database
    private val repo = MessageRepository(db.scheduledMessageDao(), db.messageLogDao())

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadMessage(messageId: Long) {
        viewModelScope.launch {
            val msg = repo.getById(messageId)
            _uiState.update { it.copy(message = msg) }
        }
        viewModelScope.launch {
            repo.getLogsFlow(messageId).collect { logs ->
                _uiState.update { it.copy(logs = logs, isLoading = false) }
            }
        }
    }
}
