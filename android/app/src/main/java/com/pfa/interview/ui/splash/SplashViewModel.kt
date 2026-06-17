package com.pfa.interview.ui.splash

import android.util.Base64
import androidx.lifecycle.ViewModel
import com.pfa.interview.data.local.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    suspend fun isLoggedIn(): Boolean {
        val token = tokenDataStore.getToken() ?: return false
        return !isTokenExpired(token)
    }

    suspend fun isOnboardingComplete(): Boolean = tokenDataStore.isOnboardingComplete()

    private fun isTokenExpired(token: String): Boolean {
        return try {
            val payload = token.split(".").getOrNull(1) ?: return true
            val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING))
            val exp = JSONObject(decoded).getLong("exp")
            System.currentTimeMillis() / 1000 > exp
        } catch (e: Exception) {
            true
        }
    }
}
