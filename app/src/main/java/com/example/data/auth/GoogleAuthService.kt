package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.model.UserSession
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class FirebaseAuthResult {
    data class Success(val session: UserSession, val message: String? = null) : FirebaseAuthResult()
    data class Error(val message: String) : FirebaseAuthResult()
}

class GoogleAuthService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        private const val TAG = "GoogleAuthService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Trigger real Google Sign-In & Firebase Auth verification:
     * 1. Opens Google Account selector popup via Android Credential Manager.
     * 2. Extracts real user's Google ID Token, email, name, and profile photo.
     * 3. Exchanges the ID token with Firebase Auth Identity Toolkit (signInWithIdp) when API key is present.
     */
    suspend fun signInWithGoogle(context: Context): FirebaseAuthResult = withContext(Dispatchers.Main) {
        val credentialManager = CredentialManager.create(context)
        val serverClientId = FirebaseConfig.webClientId

        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName
                    ?: googleIdTokenCredential.givenName
                    ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                val avatarUrl = googleIdTokenCredential.profilePictureUri?.toString()
                val googleIdToken = googleIdTokenCredential.idToken

                // If Firebase Web API Key is configured, verify with Firebase Identity Toolkit
                val firebaseApiKey = FirebaseConfig.apiKey
                if (firebaseApiKey.isNotBlank()) {
                    val firebaseResult = verifyWithFirebaseAuth(googleIdToken, firebaseApiKey, email, displayName, avatarUrl)
                    return@withContext firebaseResult
                }

                // Direct Google Identity User Session
                val session = UserSession(
                    id = "google_${email.hashCode().toString().replace("-", "0")}",
                    name = displayName,
                    email = email,
                    avatarUrl = avatarUrl,
                    accessToken = googleIdToken,
                    refreshToken = null,
                    isLoggedIn = true
                )
                Log.d(TAG, "Google Sign-In successful for $email")
                return@withContext FirebaseAuthResult.Success(session, "Signed in as $displayName")
            } else {
                return@withContext FirebaseAuthResult.Error("Unsupported credential type received from Google.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In was cancelled by user.")
            return@withContext FirebaseAuthResult.Error("Google Sign-In was cancelled.")
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credentials available on device: ${e.message}")
            return@withContext FirebaseAuthResult.Error("No Google account found on device. Please sign into Google in your device Settings.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error (${e.type}): ${e.message}", e)
            val msg = when {
                e.message?.contains("16:") == true || e.message?.contains("Canceled") == true -> "Google Sign-In cancelled."
                e.message?.contains("10:") == true || e.message?.contains("DEVELOPER_ERROR") == true ->
                    "Google OAuth configuration error. Please verify your SHA-1 fingerprint and Web Client ID in Firebase Console."
                else -> e.localizedMessage ?: "Google Sign-In failed (${e.type})."
            }
            return@withContext FirebaseAuthResult.Error(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in Google Sign-In: ${e.message}", e)
            return@withContext FirebaseAuthResult.Error(e.localizedMessage ?: "Authentication failed.")
        }
    }

    /**
     * Authenticate Google ID token with Firebase Auth REST endpoint:
     * POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=[API_KEY]
     */
    private suspend fun verifyWithFirebaseAuth(
        googleIdToken: String,
        apiKey: String,
        fallbackEmail: String,
        fallbackName: String,
        fallbackAvatar: String?
    ): FirebaseAuthResult = withContext(Dispatchers.IO) {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("postBody", "id_token=$googleIdToken&providerId=google.com")
            put("requestUri", "http://localhost")
            put("returnSecureToken", true)
            put("returnIdpCredential", true)
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Firebase Auth response code: ${response.code}")

                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val json = JSONObject(responseBody)
                    val localId = json.optString("localId", "firebase_${System.currentTimeMillis()}")
                    val email = json.optString("email", fallbackEmail)
                    val displayName = json.optString("displayName", fallbackName)
                    val photoUrl = json.optString("photoUrl", fallbackAvatar ?: "")
                    val idToken = json.optString("idToken", googleIdToken)
                    val refreshToken = json.optString("refreshToken", "")

                    val session = UserSession(
                        id = localId,
                        name = if (displayName.isNotBlank()) displayName else fallbackName,
                        email = if (email.isNotBlank()) email else fallbackEmail,
                        avatarUrl = if (photoUrl.isNotBlank()) photoUrl else fallbackAvatar,
                        accessToken = idToken,
                        refreshToken = refreshToken,
                        isLoggedIn = true
                    )
                    return@withContext FirebaseAuthResult.Success(session, "Authenticated with Firebase as $displayName")
                } else {
                    val errorMsg = parseFirebaseError(responseBody, response.code)
                    return@withContext FirebaseAuthResult.Error("Firebase Auth error: $errorMsg")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during Firebase verification", e)
            return@withContext FirebaseAuthResult.Error("Network error contacting Firebase: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Firebase verification", e)
            return@withContext FirebaseAuthResult.Error(e.localizedMessage ?: "Firebase verification failed.")
        }
    }

    private fun parseFirebaseError(responseBody: String, responseCode: Int): String {
        return try {
            if (responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val errorObj = json.optJSONObject("error")
                val message = errorObj?.optString("message") ?: json.optString("error_description")
                if (!message.isNullOrBlank()) {
                    when {
                        message.contains("API_KEY_INVALID", ignoreCase = true) -> "Invalid Firebase API Key. Please check your FirebaseConfig."
                        message.contains("PROJECT_NOT_FOUND", ignoreCase = true) -> "Firebase project not found."
                        message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) -> "Google Sign-In is not enabled in your Firebase Console (Authentication -> Sign-in method)."
                        else -> message
                    }
                } else {
                    "HTTP $responseCode"
                }
            } else {
                "HTTP $responseCode"
            }
        } catch (_: Exception) {
            "HTTP $responseCode"
        }
    }

    /**
     * Clear Google & Firebase credential state on sign out
     */
    suspend fun signOut(context: Context) = withContext(Dispatchers.Main) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear credential state: ${e.localizedMessage}")
        }
    }
}
