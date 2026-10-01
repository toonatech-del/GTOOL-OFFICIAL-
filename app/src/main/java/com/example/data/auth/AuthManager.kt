package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(
    private val context: Context,
    private val googleAuthService: GoogleAuthService = GoogleAuthService()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(loadSession())
    val currentUser: StateFlow<UserSession> = _currentUser.asStateFlow()

    private fun loadSession(): UserSession {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val id = prefs.getString(KEY_USER_ID, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val avatarUrl = prefs.getString(KEY_AVATAR_URL, null)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)

        return if (isLoggedIn && email.isNotBlank()) {
            UserSession(
                id = id.ifBlank { "google_${System.currentTimeMillis()}" },
                name = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } },
                email = email,
                avatarUrl = avatarUrl,
                accessToken = accessToken,
                refreshToken = refreshToken,
                isLoggedIn = true
            )
        } else {
            UserSession(
                id = "",
                name = "",
                email = "",
                avatarUrl = null,
                accessToken = null,
                refreshToken = null,
                isLoggedIn = false
            )
        }
    }

    /**
     * Direct Firebase / Google One-Tap sign in
     */
    suspend fun signInWithGoogle(activityContext: Context): FirebaseAuthResult {
        val result = googleAuthService.signInWithGoogle(activityContext)
        if (result is FirebaseAuthResult.Success) {
            saveSession(result.session)
        }
        return result
    }

    /**
     * Explicitly set an authenticated user session
     */
    fun setAuthenticatedUser(name: String, email: String, avatarUrl: String? = null) {
        val session = UserSession(
            id = "google_${email.hashCode().toString().replace("-", "0")}",
            name = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } },
            email = email,
            avatarUrl = avatarUrl,
            accessToken = "firebase_token_${System.currentTimeMillis()}",
            refreshToken = null,
            isLoggedIn = true
        )
        saveSession(session)
    }

    fun saveSession(session: UserSession) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, session.id)
            .putString(KEY_USER_NAME, session.name)
            .putString(KEY_USER_EMAIL, session.email)
            .putString(KEY_AVATAR_URL, session.avatarUrl)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .apply()

        _currentUser.value = session
    }

    suspend fun signOut(activityContext: Context) {
        googleAuthService.signOut(activityContext)

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()

        _currentUser.value = UserSession(
            id = "",
            name = "",
            email = "",
            avatarUrl = null,
            accessToken = null,
            refreshToken = null,
            isLoggedIn = false
        )
    }

    companion object {
        private const val PREFS_NAME = "gtool_x_firebase_auth_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
