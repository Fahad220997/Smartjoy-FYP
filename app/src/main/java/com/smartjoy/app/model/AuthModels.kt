package com.smartjoy.app.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = 0L
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isEmailVerified: Boolean = false,
    val isPasswordResetEmailSent: Boolean = false,
    val user: UserProfile? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
