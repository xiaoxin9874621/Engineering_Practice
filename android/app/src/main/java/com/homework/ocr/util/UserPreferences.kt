package com.homework.ocr.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = longPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val REAL_NAME = stringPreferencesKey("real_name")
        val ROLE = intPreferencesKey("role")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val userId: Flow<Long?> = context.dataStore.data.map { it[USER_ID] }
    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME] }
    val realName: Flow<String?> = context.dataStore.data.map { it[REAL_NAME] }
    val role: Flow<Int?> = context.dataStore.data.map { it[ROLE] }

    suspend fun saveLoginInfo(
        accessToken: String, refreshToken: String,
        userId: Long, username: String, realName: String, role: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[USER_ID] = userId
            prefs[USERNAME] = username
            prefs[REAL_NAME] = realName
            prefs[ROLE] = role
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
