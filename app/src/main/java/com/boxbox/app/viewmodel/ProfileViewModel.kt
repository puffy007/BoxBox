package com.boxbox.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxbox.app.data.model.UiState
import com.boxbox.app.data.model.UserProfile
import com.boxbox.app.data.repository.BoxBoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: BoxBoxRepository = BoxBoxRepository()
) : ViewModel() {

    private val _profile = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val profile: StateFlow<UiState<UserProfile>> = _profile

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult

    init {
        checkAuth()
    }

    private fun checkAuth() {
        val user = repository.getCurrentUser()
        if (user != null) {
            _authState.value = AuthState.Authenticated(user.uid)
            loadProfile(user.uid)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    // AUTH
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                val result = repository.signIn(email, password)
                val uid = result.user?.uid ?: return@launch
                _authState.value = AuthState.Authenticated(uid)
                loadProfile(uid)
            } catch (e: Exception) {
                _actionResult.value = "Sign in failed: ${e.message}"
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            try {
                val result = repository.signUp(email, password)
                val uid = result.user?.uid ?: return@launch
                // CREATE profile in Firestore
                val newProfile = UserProfile(
                    uid = uid,
                    displayName = displayName,
                    email = email
                )
                repository.createUserProfile(newProfile)
                _authState.value = AuthState.Authenticated(uid)
                _profile.value = UiState.Success(newProfile)
                _actionResult.value = "Account created!"
            } catch (e: Exception) {
                _actionResult.value = "Sign up failed: ${e.message}"
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Unauthenticated
        _profile.value = UiState.Loading
    }

    // READ
    fun loadProfile(uid: String) {
        viewModelScope.launch {
            _profile.value = UiState.Loading
            try {
                val p = repository.getUserProfile(uid)
                _profile.value = if (p != null) UiState.Success(p) else UiState.Error("Profile not found")
            } catch (e: Exception) {
                _profile.value = UiState.Error(e.message ?: "Error")
            }
        }
    }

    // UPDATE
    fun updateProfile(displayName: String, favouriteDriver: String, favouriteTeam: String, notificationsEnabled: Boolean) {
        val uid = repository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "displayName" to displayName,
                    "favouriteDriver" to favouriteDriver,
                    "favouriteTeam" to favouriteTeam,
                    "notificationsEnabled" to notificationsEnabled
                )
                repository.updateUserProfile(uid, updates)
                loadProfile(uid)
                _actionResult.value = "Profile updated!"
            } catch (e: Exception) {
                _actionResult.value = "Update failed: ${e.message}"
            }
        }
    }

    // UPDATE PHOTO
    fun uploadPhoto(uri: Uri) {
        val uid = repository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                val url = repository.uploadProfilePhoto(uid, uri)
                repository.updateUserProfile(uid, mapOf("photoUrl" to url))
                loadProfile(uid)
                _actionResult.value = "Photo updated!"
            } catch (e: Exception) {
                _actionResult.value = "Photo upload failed: ${e.message}"
            }
        }
    }

    // DELETE
    fun deleteAccount() {
        val uid = repository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                repository.deleteProfilePhoto(uid)
                repository.deleteUserProfile(uid)
                _authState.value = AuthState.Unauthenticated
                _actionResult.value = "Account deleted"
            } catch (e: Exception) {
                _actionResult.value = "Delete failed: ${e.message}"
            }
        }
    }

    fun clearActionResult() { _actionResult.value = null }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val uid: String) : AuthState()
}
