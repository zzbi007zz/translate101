package com.example.zaloauto.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val autoSend: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SEND] ?: false
    }

    suspend fun setAutoSend(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_AUTO_SEND] = enabled }
    }

    companion object {
        private val KEY_AUTO_SEND = booleanPreferencesKey("auto_send")
    }
}
