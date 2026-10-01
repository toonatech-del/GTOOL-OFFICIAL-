package com.example.model

data class UserSession(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isLoggedIn: Boolean = false
)
