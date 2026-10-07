package com.smartjoy.app.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.smartjoy.app.R
import com.smartjoy.app.model.AuthUiState
import com.smartjoy.app.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        auth.currentUser?.let { firebaseUser ->
            _uiState.value = _uiState.value.copy(
                isLoggedIn = true,
                isEmailVerified = firebaseUser.isEmailVerified,
                user = UserProfile(
                    uid = firebaseUser.uid, 
                    email = firebaseUser.email.orEmpty(),
                    name = firebaseUser.displayName.orEmpty(),
                    photoUrl = firebaseUser.photoUrl?.toString().orEmpty()
                )
            )
            refreshEmailVerified(silent = true)
            loadUserProfile(firebaseUser.uid)
        }
    }

    fun login(email: String, password: String) {
        val emailError = validateEmail(email)
        if (emailError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = emailError)
            return
        }
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().getString(R.string.err_password_required))
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                firebaseUser?.reload()?.await()
                val verified = firebaseUser?.isEmailVerified ?: false
                loadUserProfile(firebaseUser?.uid.orEmpty())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isEmailVerified = verified
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = mapAuthError(e))
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        val validationError = when {
            name.isBlank() -> getApplication<Application>().getString(R.string.err_name_required)
            emailError != null -> emailError
            passwordError != null -> passwordError
            password != confirmPassword -> getApplication<Application>().getString(R.string.err_passwords_dont_match)
            else -> null
        }
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validationError)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                val uid = firebaseUser?.uid.orEmpty()

                // Update Auth Profile
                firebaseUser?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                )?.await()

                val profile = UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    createdAt = System.currentTimeMillis()
                )
                try {
                    firestore.collection("users").document(uid).set(profile).await()
                } catch (_: Exception) {
                }

                var verificationSent = true
                try {
                    firebaseUser?.sendEmailVerification()?.await()
                } catch (_: Exception) {
                    verificationSent = false
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    isEmailVerified = false,
                    user = profile,
                    infoMessage = if (verificationSent) {
                        getApplication<Application>().getString(R.string.info_verification_sent, email)
                    } else {
                        getApplication<Application>().getString(R.string.info_account_created_email_failed)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = mapAuthError(e))
            }
        }
    }

    fun resendVerificationEmail() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                user.sendEmailVerification().await()
                _uiState.value = _uiState.value.copy(
                    infoMessage = getApplication<Application>().getString(R.string.info_verification_resent, user.email.orEmpty()),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = mapAuthError(e))
            }
        }
    }

    fun refreshEmailVerified(silent: Boolean = false) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                user.reload().await()
                _uiState.value = _uiState.value.copy(
                    isEmailVerified = user.isEmailVerified,
                    errorMessage = when {
                        user.isEmailVerified -> null
                        silent -> _uiState.value.errorMessage
                        else -> getApplication<Application>().getString(R.string.err_email_not_verified)
                    }
                )
            } catch (e: Exception) {
                if (!silent) {
                    _uiState.value = _uiState.value.copy(errorMessage = mapAuthError(e))
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val emailError = validateEmail(email)
        if (emailError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = emailError)
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPasswordResetEmailSent = true,
                    infoMessage = getApplication<Application>().getString(R.string.info_password_reset_sent, email)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = mapAuthError(e))
            }
        }
    }

    fun updateDisplayName(newName: String) {
        val user = auth.currentUser ?: return
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().getString(R.string.err_name_empty))
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                // Update Auth Profile
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()
                ).await()

                // Update Firestore
                firestore.collection("users").document(user.uid)
                    .set(mapOf("name" to newName), SetOptions.merge())
                    .await()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = _uiState.value.user?.copy(name = newName),
                    infoMessage = getApplication<Application>().getString(R.string.info_name_updated),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = mapAuthError(e))
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        val user = auth.currentUser ?: return
        _uiState.value = _uiState.value.copy(isUploadingPhoto = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { 
                    it.readBytes() 
                } ?: throw IllegalArgumentException("Could not read photo data.")

                val storageInstance = try {
                    FirebaseStorage.getInstance("gs://smartjoy-d0a60.firebasestorage.app")
                } catch (_: Exception) {
                    try {
                        FirebaseStorage.getInstance("gs://smartjoy-d0a60.appspot.com")
                    } catch (_: Exception) {
                        FirebaseStorage.getInstance()
                    }
                }

                val ref = storageInstance.reference.child("profile_photos/${user.uid}.jpg")
                ref.putBytes(bytes).await()
                
                val downloadUrl = ref.downloadUrl.await().toString()

                // Update Auth Profile
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setPhotoUri(Uri.parse(downloadUrl))
                        .build()
                ).await()

                // Update Firestore
                val current = _uiState.value.user
                val updatedProfile = UserProfile(
                    uid = user.uid,
                    name = user.displayName.orEmpty().ifBlank { current?.name.orEmpty() },
                    email = user.email.orEmpty().ifBlank { current?.email.orEmpty() },
                    photoUrl = downloadUrl,
                    createdAt = current?.createdAt ?: System.currentTimeMillis()
                )

                firestore.collection("users").document(user.uid)
                    .set(updatedProfile, SetOptions.merge())
                    .await()

                _uiState.value = _uiState.value.copy(
                    isUploadingPhoto = false,
                    user = updatedProfile,
                    infoMessage = getApplication<Application>().getString(R.string.info_photo_updated),
                    errorMessage = null
                )
            } catch (e: StorageException) {
                val msg = if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND || e.httpResultCode == 404) {
                    "Firebase Storage default bucket not found (404). Please ensure Storage is created in Firebase Console."
                } else {
                    e.localizedMessage ?: mapAuthError(e)
                }
                _uiState.value = _uiState.value.copy(
                    isUploadingPhoto = false,
                    errorMessage = msg
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingPhoto = false, 
                    errorMessage = e.localizedMessage ?: mapAuthError(e)
                )
            }
        }
    }

    fun logout() {
        auth.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }

    fun resetPasswordResetFlag() {
        _uiState.value = _uiState.value.copy(isPasswordResetEmailSent = false)
    }

    private fun loadUserProfile(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).get().await()
                val profile = snapshot.toObject(UserProfile::class.java)
                val firebaseUser = auth.currentUser
                
                if (profile != null) {
                    val photoUrl = profile.photoUrl.ifBlank {
                        firebaseUser?.photoUrl?.toString().orEmpty()
                    }
                    val name = profile.name.ifBlank {
                        firebaseUser?.displayName.orEmpty()
                    }
                    val email = profile.email.ifBlank {
                        firebaseUser?.email.orEmpty()
                    }
                    _uiState.value = _uiState.value.copy(
                        user = profile.copy(photoUrl = photoUrl, name = name, email = email)
                    )
                } else {
                    // Fallback to Auth data
                    if (firebaseUser != null) {
                        _uiState.value = _uiState.value.copy(
                            user = UserProfile(
                                uid = uid,
                                name = firebaseUser.displayName.orEmpty(),
                                email = firebaseUser.email.orEmpty(),
                                photoUrl = firebaseUser.photoUrl?.toString().orEmpty()
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return getApplication<Application>().getString(R.string.err_email_required)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return getApplication<Application>().getString(R.string.err_invalid_email)
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 6) return getApplication<Application>().getString(R.string.err_password_too_short)
        return null
    }

    private fun mapAuthError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException ->
            getApplication<Application>().getString(R.string.err_auth_weak_password)
        is FirebaseAuthInvalidCredentialsException ->
            getApplication<Application>().getString(R.string.err_auth_invalid_credentials)
        is FirebaseAuthUserCollisionException ->
            getApplication<Application>().getString(R.string.err_auth_user_collision)
        is FirebaseAuthInvalidUserException ->
            getApplication<Application>().getString(R.string.err_auth_invalid_user)
        is FirebaseNetworkException ->
            getApplication<Application>().getString(R.string.err_auth_network)
        is FirebaseAuthException ->
            e.message ?: getApplication<Application>().getString(R.string.err_auth_failed)
        else -> e.message ?: getApplication<Application>().getString(R.string.err_something_went_wrong)
    }
}
