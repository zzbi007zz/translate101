package com.example.zaloauto.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloauto.ZaloAutoApp
import com.example.zaloauto.data.db.ScheduledMessageEntity
import com.example.zaloauto.data.db.TemplateEntity
import com.example.zaloauto.data.repository.MessageRepository
import com.example.zaloauto.data.repository.TemplateRepository
import com.example.zaloauto.service.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val recipient: String = "",
    val message: String = "",
    val scheduledTime: Long? = null,
    val templates: List<TemplateEntity> = emptyList(),
    val selectedTemplateId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val scheduleResult: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZaloAutoApp.getInstance().database
    private val messageRepo = MessageRepository(db.scheduledMessageDao(), db.messageLogDao())
    private val templateRepo = TemplateRepository(db.templateDao())
    private val alarmScheduler = AlarmScheduler(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            templateRepo.getAllFlow().collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    fun updateRecipient(name: String) {
        _uiState.update { it.copy(recipient = name) }
    }

    fun updateMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun updateScheduledTime(millis: Long) {
        _uiState.update { it.copy(scheduledTime = millis) }
    }

    fun selectTemplate(templateId: Long?) {
        _uiState.update { it.copy(selectedTemplateId = templateId) }
        if (templateId != null) {
            viewModelScope.launch {
                val template = templateRepo.getById(templateId)
                val state = _uiState.value
                if (template != null && state.message.isBlank()) {
                    _uiState.update { it.copy(message = template.content) }
                }
            }
        }
    }

    fun scheduleMessage() {
        val state = _uiState.value
        val recipient = state.recipient.trim()
        val message = state.message.trim()
        val scheduledTime = state.scheduledTime

        // Validation
        if (recipient.isBlank()) {
            _uiState.update { it.copy(error = "Please enter recipient name") }
            return
        }
        if (message.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a message") }
            return
        }
        if (scheduledTime == null) {
            _uiState.update { it.copy(error = "Please select a date and time") }
            return
        }
        if (scheduledTime <= System.currentTimeMillis()) {
            _uiState.update { it.copy(error = "Scheduled time must be in the future") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val messageId = messageRepo.scheduleMessage(
                    targetName = recipient,
                    messageText = message,
                    scheduledAt = scheduledTime,
                    templateId = state.selectedTemplateId
                )
                val scheduled = alarmScheduler.scheduleMessage(messageId, scheduledTime)
                val result = if (scheduled) {
                    "Message scheduled for ${java.text.SimpleDateFormat("HH:mm, dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(scheduledTime))}"
                } else {
                    "Message saved but alarm scheduling failed. Check exact alarm permission."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scheduleResult = result,
                        recipient = "",
                        message = "",
                        scheduledTime = null,
                        selectedTemplateId = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(scheduleResult = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()
}
