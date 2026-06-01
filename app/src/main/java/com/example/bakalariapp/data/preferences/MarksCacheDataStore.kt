package com.example.bakalariapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bakalariapp.data.model.MarksResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.marksCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "marks_cache")

class MarksCacheDataStore(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        private val MARKS_JSON_KEY = stringPreferencesKey("marks_json")
        private val CACHE_TIMESTAMP_KEY = longPreferencesKey("cache_timestamp")
    }
    
    val cachedMarks: Flow<MarksResponse?> = context.marksCacheDataStore.data.map { preferences ->
        val json = preferences[MARKS_JSON_KEY]
        if (json != null) {
            try {
                gson.fromJson(json, MarksResponse::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    val cacheTimestamp: Flow<Long> = context.marksCacheDataStore.data.map { preferences ->
        preferences[CACHE_TIMESTAMP_KEY] ?: 0L
    }
    
    suspend fun saveMarks(marksResponse: MarksResponse) {
        context.marksCacheDataStore.edit { preferences ->
            preferences[MARKS_JSON_KEY] = gson.toJson(marksResponse)
            preferences[CACHE_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }
    
    suspend fun clearCache() {
        context.marksCacheDataStore.edit { preferences ->
            preferences.remove(MARKS_JSON_KEY)
            preferences.remove(CACHE_TIMESTAMP_KEY)
        }
    }
}
