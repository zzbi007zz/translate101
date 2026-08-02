package com.example.zaloauto.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object ListRoute
@Serializable object TemplatesRoute
@Serializable object SettingsRoute
@Serializable data class DetailRoute(val messageId: Long)
