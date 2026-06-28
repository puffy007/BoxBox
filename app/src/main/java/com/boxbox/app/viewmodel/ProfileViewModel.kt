package com.boxbox.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boxbox.app.data.model.UiState
import com.boxbox.app.data.model.UserProfile
import com.boxbox.app.data.repository.BoxBoxRepository
import com.boxbox.app.ui.theme.ThemeState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
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

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto

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
                // CREATE profile in Firestore - isDarkMode defaults to whatever the
                // device is currently showing, so a brand-new account starts from the
                // theme the person was already looking at rather than a hardcoded value.
                val newProfile = UserProfile(
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    isDarkMode = ThemeState.isDarkMode
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
                android.util.Log.d("BoxBoxTheme", "ProfileViewModel.loadProfile fetched isDarkMode=${p?.isDarkMode}")
                if (p != null) {
                    _profile.value = UiState.Success(p)
                    // Sync the global theme state to match this account's saved
                    // preferences (team color + dark/light mode) as soon as the
                    // profile loads - covers both "just logged in" and "app restarted
                    // while already logged in" since checkAuth() also calls this.
                    ThemeState.favouriteTeam = p.favouriteTeam
                    ThemeState.isDarkMode = p.isDarkMode
                    android.util.Log.d("BoxBoxTheme", "ProfileViewModel.loadProfile SET ThemeState.isDarkMode=${ThemeState.isDarkMode}")
                } else {
                    _profile.value = UiState.Error("Profile not found")
                }
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

    /**
     * Saves the dark/light mode preference to this account's Firestore profile, so it
     * follows the user across devices the same way favouriteTeam already does - rather
     * than being a local, device-only setting.
     */
    /**
     * Saves the dark/light mode preference to this account's Firestore profile, so it
     * follows the user across devices.
     */
    fun updateThemePreference(isDarkMode: Boolean) {
        // 1. Odmah ažuriraj globalni state da se UI promijeni (Optimistic update)
        ThemeState.isDarkMode = isDarkMode

        // 2. Ažuriraj lokalni profil state flow tako da Switch na ekranu ne "skakuće"
        val current = _profile.value
        if (current is UiState.Success) {
            _profile.value = UiState.Success(current.data.copy(isDarkMode = isDarkMode))
        }

        // 3. Uzmi UID samo jednom (ovdje je bila greška s duplom deklaracijom)
        val uid = repository.getCurrentUser()?.uid ?: return

        // 4. Pokreni snimanje u bazu preko coroutine
        viewModelScope.launch {
            try {
                // Koristimo repository metodu koju već imaš (isDarkMode umjesto newMode)
                repository.updateUserProfile(uid, mapOf("isDarkMode" to isDarkMode))
            } catch (e: Exception) {
                // Ako snimanje ne uspije, vrati sve na staro (Rollback)
                val rollback = _profile.value
                if (rollback is UiState.Success) {
                    _profile.value = UiState.Success(rollback.data.copy(isDarkMode = !isDarkMode))
                }
                ThemeState.isDarkMode = !isDarkMode
                _actionResult.value = "Couldn't save theme: ${e.message}"
            }
        }
    }

    /**
     * Toggles the race-countdown test notifications on/off. Saves the preference to
     * Firestore (so it persists and syncs like favouriteTeam/isDarkMode), and starts or
     * stops the actual WorkManager chain immediately so the switch has visible effect
     * without needing an app restart.
     */
    fun updateNotificationsEnabled(context: Context, enabled: Boolean) {
        val current = _profile.value
        if (current is UiState.Success) {
            _profile.value = UiState.Success(current.data.copy(notificationsEnabled = enabled))
        }

        if (enabled) {
            com.boxbox.app.notifications.RaceCountdownScheduler.start(context)
        } else {
            com.boxbox.app.notifications.RaceCountdownScheduler.stop(context)
        }

        val uid = repository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                repository.updateUserProfile(uid, mapOf("notificationsEnabled" to enabled))
            } catch (e: Exception) {
                val rollback = _profile.value
                if (rollback is UiState.Success) {
                    _profile.value = UiState.Success(rollback.data.copy(notificationsEnabled = !enabled))
                }
                if (enabled) {
                    com.boxbox.app.notifications.RaceCountdownScheduler.stop(context)
                } else {
                    com.boxbox.app.notifications.RaceCountdownScheduler.start(context)
                }
                _actionResult.value = "Couldn't save notification setting: ${e.message}"
            }
        }
    }

    // UPDATE PHOTO
    fun uploadPhoto(context: Context, uri: Uri) {
        val uid = repository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            try {
                val url = repository.uploadProfilePhoto(context, uid, uri)
                repository.updateUserProfile(uid, mapOf("photoUrl" to url))
                loadProfile(uid)
                _actionResult.value = "Photo updated!"
            } catch (e: Exception) {
                _actionResult.value = "Photo upload failed: ${e.message}"
            } finally {
                _isUploadingPhoto.value = false
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
