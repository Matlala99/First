package com.firstword.app.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstword.app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadUserProfile() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            _user.value = null
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val document = db.collection("users").document(firebaseUser.uid).get().await()

                if (document.exists()) {
                    val user = document.toObject(User::class.java)?.copy(id = document.id)
                    _user.value = user
                } else {
                    // Create user profile if it doesn't exist
                    createUserProfile(firebaseUser)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun createUserProfile(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        try {
            val user = User(
                id = firebaseUser.uid,
                handle = generateHandle(firebaseUser.displayName ?: "user"),
                displayName = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                isGuest = firebaseUser.isAnonymous
            )

            db.collection("users").document(firebaseUser.uid).set(user).await()
            _user.value = user
        } catch (e: Exception) {
            _error.value = "Failed to create profile: ${e.message}"
        }
    }

    private fun generateHandle(displayName: String): String {
        val baseHandle = displayName.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
        return "${baseHandle}_${System.currentTimeMillis().toString().takeLast(4)}"
    }

    fun clearError() {
        _error.value = null
    }
}