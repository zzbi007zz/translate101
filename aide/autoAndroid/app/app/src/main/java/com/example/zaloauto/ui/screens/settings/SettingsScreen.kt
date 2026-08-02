package com.example.zaloauto.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh permissions when screen resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Permissions section
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            val perm = state.permissions
            SettingsItem(
                icon = Icons.Outlined.Accessibility,
                title = "Accessibility Service",
                subtitle = if (perm.accessibilityEnabled) "Enabled" else "Disabled — Tap to enable",
                statusColor = if (perm.accessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                onClick = viewModel::openAccessibilitySettings
            )
            SettingsItem(
                icon = Icons.Outlined.ScreenShare,
                title = "Display Over Other Apps",
                subtitle = if (perm.overlayGranted) "Granted" else "Not granted — Tap to grant",
                statusColor = if (perm.overlayGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                onClick = viewModel::openOverlaySettings
            )
            SettingsItem(
                icon = Icons.Outlined.Alarm,
                title = "Exact Alarm Permission",
                subtitle = if (perm.exactAlarmGranted) "Granted" else "Not granted — Tap to grant",
                statusColor = if (perm.exactAlarmGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                onClick = viewModel::openExactAlarmSettings
            )
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                title = "Notification Permission",
                subtitle = if (perm.notificationsGranted) "Granted" else "Not granted",
                statusColor = if (perm.notificationsGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                onClick = viewModel::openAppSettings
            )

            HorizontalDivider()

            // Preferences section
            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-send", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Skip confirmation before sending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.autoSend,
                    onCheckedChange = viewModel::setAutoSend
                )
            }

            HorizontalDivider()

            // About section
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Zalo Auto Sender v${state.appVersion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Automates sending scheduled messages in Zalo using Android Accessibility Service.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    statusColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
