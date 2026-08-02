package com.example.zaloauto.ui.screens.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloauto.ZaloAutoApp
import com.example.zaloauto.service.AlarmScheduler
import com.example.zaloauto.service.accessibility.ZaloAutomationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionsState(
    val accessibilityEnabled: Boolean = false,
    val overlayGranted: Boolean = false,
    val exactAlarmGranted: Boolean = false,
    val notificationsGranted: Boolean = false
)

data class SettingsUiState(
    val permissions: PermissionsState = PermissionsState(),
    val autoSend: Boolean = false,
    val appVersion: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ZaloAutoApp
    private val prefsRepo = app.preferencesRepo
    private val alarmScheduler = AlarmScheduler(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            permissions = checkAllPermissions(application),
            appVersion = try {
                application.packageManager.getPackageInfo(application.packageName, 0)
                    .versionName ?: "1.0.0"
            } catch (e: Exception) { "1.0.0" }
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.autoSend.collect { enabled ->
                _uiState.update { it.copy(autoSend = enabled) }
            }
        }
    }

    fun refreshPermissions() {
        _uiState.update { it.copy(permissions = checkAllPermissions(getApplication())) }
    }

    fun setAutoSend(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoSend(enabled) }
    }

    fun openAccessibilitySettings() {
        val context = getApplication<Application>()
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openOverlaySettings() {
        val context = getApplication<Application>()
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openExactAlarmSettings() {
        val context = getApplication<Application>()
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openAppSettings() {
        val context = getApplication<Application>()
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun checkAllPermissions(context: Context): PermissionsState {
        return PermissionsState(
            accessibilityEnabled = ZaloAutomationService.instance != null,
            overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true,
            exactAlarmGranted = alarmScheduler.canScheduleExactAlarms(),
            notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
}
