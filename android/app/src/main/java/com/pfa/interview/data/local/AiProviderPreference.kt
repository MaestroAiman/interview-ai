package com.pfa.interview.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiProviderDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_provider_prefs")

@Singleton
class AiProviderPreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val AI_PROVIDER_KEY = stringPreferencesKey("ai_provider")

    val aiProvider: Flow<String> = context.aiProviderDataStore.data
        .map { it[AI_PROVIDER_KEY] ?: "ollama" }

    suspend fun setAiProvider(provider: String) {
        context.aiProviderDataStore.edit { it[AI_PROVIDER_KEY] = provider }
    }
}
