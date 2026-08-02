package com.example.zaloauto.ui.screens.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.zaloauto.ui.components.PermissionStatusChip
import com.example.zaloauto.ui.navigation.ListRoute
import com.example.zaloauto.ui.navigation.SettingsRoute
import com.example.zaloauto.ui.navigation.TemplatesRoute
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.scheduleResult) {
        state.scheduleResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearResult()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Schedule Message") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission status chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionStatusChip(
                    label = "Accessibility",
                    isGranted = true, // checked at app start
                    onClick = { navController.navigate(SettingsRoute) },
                    modifier = Modifier.weight(1f)
                )
                PermissionStatusChip(
                    label = "Overlay",
                    isGranted = android.provider.Settings.canDrawOverlays(context),
                    onClick = { navController.navigate(SettingsRoute) },
                    modifier = Modifier.weight(1f)
                )
                PermissionStatusChip(
                    label = "Alarms",
                    isGranted = viewModel.canScheduleExactAlarms(),
                    onClick = { navController.navigate(SettingsRoute) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Recipient input
            OutlinedTextField(
                value = state.recipient,
                onValueChange = viewModel::updateRecipient,
                label = { Text("Zalo User Name") },
                placeholder = { Text("Enter the Zalo user's display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Template selector
            if (state.templates.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.templates.find { it.id == state.selectedTemplateId }?.name ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Template (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                viewModel.selectTemplate(null)
                                expanded = false
                            }
                        )
                        state.templates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    viewModel.selectTemplate(template.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Message input
            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::updateMessage,
                label = { Text("Message") },
                placeholder = { Text("Enter your message") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            // Date & Time picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val time = state.scheduledTime ?: System.currentTimeMillis()
                                val c = Calendar.getInstance().apply { timeInMillis = time }
                                c.set(year, month, day)
                                viewModel.updateScheduledTime(c.timeInMillis)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = state.scheduledTime?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Pick Date"
                    )
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        if (state.scheduledTime != null) {
                            cal.timeInMillis = state.scheduledTime
                        }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val newCal = Calendar.getInstance().apply {
                                    if (state.scheduledTime != null) {
                                        timeInMillis = state.scheduledTime
                                    }
                                    set(Calendar.HOUR_OF_DAY, hour)
                                    set(Calendar.MINUTE, minute)
                                }
                                viewModel.updateScheduledTime(newCal.timeInMillis)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = state.scheduledTime?.let {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                        } ?: "Pick Time"
                    )
                }
            }

            // Scheduled time display
            state.scheduledTime?.let { time ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )) {
                    Text(
                        text = "Scheduled for: ${
                            SimpleDateFormat("EEEE, dd MMM yyyy 'at' HH:mm", Locale.getDefault())
                                .format(Date(time))
                        }",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Schedule button
            Button(
                onClick = viewModel::scheduleMessage,
                enabled = !state.isLoading && state.recipient.isNotBlank() && state.message.isNotBlank() && state.scheduledTime != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.isLoading) "Scheduling..." else "Schedule Send")
            }
        }
    }
}
