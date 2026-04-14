package com.mtc.termosonda.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KeyStore(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("key")
        private val KEY = stringPreferencesKey("myKey")
    }
    val getKey: Flow<String> = context.dataStore.data.map {preferences ->
        preferences[KEY] ?: ""
    }
    suspend fun saveKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY] = key
        }
    }
}