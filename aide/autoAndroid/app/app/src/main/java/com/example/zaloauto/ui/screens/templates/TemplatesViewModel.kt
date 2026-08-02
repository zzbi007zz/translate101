package com.example.zaloauto.ui.screens.templates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloauto.ZaloAutoApp
import com.example.zaloauto.data.db.TemplateEntity
import com.example.zaloauto.data.repository.TemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplatesUiState(
    val templates: List<TemplateEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class TemplatesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZaloAutoApp.getInstance().database
    private val repo = TemplateRepository(db.templateDao())

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllFlow().collect { templates ->
                _uiState.update { it.copy(templates = templates, isLoading = false) }
            }
        }
    }

    fun addTemplate(name: String, content: String) {
        viewModelScope.launch {
            try {
                repo.insert(name, content)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateTemplate(template: TemplateEntity) {
        viewModelScope.launch {
            try {
                repo.update(template)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch {
            try {
                repo.delete(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
