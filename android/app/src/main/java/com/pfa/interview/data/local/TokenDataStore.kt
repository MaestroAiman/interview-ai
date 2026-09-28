package com.pfa.interview.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    suspend fun saveToken(token: String) {
        dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun getToken(): String? {
        return dataStore.data.first()[TOKEN_KEY]
    }

    suspend fun clearToken() {
        dataStore.edit { it.remove(TOKEN_KEY) }
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { it[USER_ID_KEY] = userId }
    }

    suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit { it[USER_NAME_KEY] = name }
    }

    suspend fun getUserName(): String? {
        return dataStore.data.first()[USER_NAME_KEY]
    }

    suspend fun saveUserRole(role: String) {
        dataStore.edit { it[USER_ROLE_KEY] = role }
    }

    suspend fun getUserRole(): String {
        return dataStore.data.first()[USER_ROLE_KEY] ?: "USER"
    }

    suspend fun isAdmin(): Boolean = getUserRole() == "ADMIN"

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[ONBOARDING_KEY] = complete }
    }

    suspend fun isOnboardingComplete(): Boolean {
        return dataStore.data.map { it[ONBOARDING_KEY] ?: false }.first()
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    companion object {
        val TOKEN_KEY = stringPreferencesKey("jwt_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_ROLE_KEY = stringPreferencesKey("user_role")
        val ONBOARDING_KEY = booleanPreferencesKey("onboarding_complete")
    }
}
