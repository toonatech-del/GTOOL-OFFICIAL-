package com.example.data.auth

import com.example.BuildConfig

/**
 * Firebase & Google Identity Configuration.
 *
 * All credentials are fully externalized to BuildConfig / .env / Secrets panel:
 * - FIREBASE_API_KEY
 * - FIREBASE_PROJECT_ID
 * - FIREBASE_AUTH_DOMAIN
 * - FIREBASE_APP_ID
 * - FIREBASE_WEB_CLIENT_ID
 *
 * Never hardcode real API keys or sensitive project IDs in source files.
 */
object FirebaseConfig {
    const val DEFAULT_API_KEY = ""
    const val DEFAULT_AUTH_DOMAIN = ""
    const val DEFAULT_PROJECT_ID = ""
    const val DEFAULT_APP_ID = ""
    const val DEFAULT_WEB_CLIENT_ID = ""

    /**
     * Use direct BuildConfig references. 
     * These are populated from environment variables during build time.
     */
    val apiKey: String
        get() = BuildConfig.FIREBASE_API_KEY.ifBlank { DEFAULT_API_KEY }

    val authDomain: String
        get() = BuildConfig.FIREBASE_AUTH_DOMAIN.ifBlank { DEFAULT_AUTH_DOMAIN }

    val projectId: String
        get() = BuildConfig.FIREBASE_PROJECT_ID.ifBlank { DEFAULT_PROJECT_ID }

    val appId: String
        get() = BuildConfig.FIREBASE_APP_ID.ifBlank { DEFAULT_APP_ID }

    val webClientId: String
        get() = BuildConfig.FIREBASE_WEB_CLIENT_ID.ifBlank { DEFAULT_WEB_CLIENT_ID }
}
