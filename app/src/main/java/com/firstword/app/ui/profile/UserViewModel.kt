package com.firstword.app.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstword.app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _viewedUser = MutableLiveData<User?>()
    val viewedUser: LiveData<User?> = _viewedUser

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Load current user profile
    fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val document = db.collection("users").document(userId).get().await()

                if (document.exists()) {
                    val user = User.fromDocument(document)
                    _currentUser.value = user
                } else {
                    // Create user profile if it doesn't exist
                    createUserProfile()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load any user profile by ID
    fun loadUserProfile(userId: String) {
        if (userId == auth.currentUser?.uid) {
            loadCurrentUser()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val document = db.collection("users").document(userId).get().await()

                if (document.exists()) {
                    val user = User.fromDocument(document)
                    _viewedUser.value = user
                } else {
                    _error.value = "User not found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load user profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun createUserProfile() {
        val firebaseUser = auth.currentUser ?: return

        try {
            val user = User.fromFirebaseUser(firebaseUser)

            db.collection("users").document(firebaseUser.uid).set(user.toMap()).await()
            _currentUser.value = user
        } catch (e: Exception) {
            _error.value = "Failed to create profile: ${e.message}"
        }
    }

    // Update user profile
    fun updateProfile(updates: Map<String, Any>) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Add updatedAt timestamp
                val updateData = updates.toMutableMap()
                updateData["updatedAt"] = FieldValue.serverTimestamp()

                db.collection("users").document(userId).update(updateData).await()

                // Update local user data
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    val updatedUser = createUpdatedUser(currentUser, updates)
                    _currentUser.value = updatedUser
                }

                loadCurrentUser() // Refresh data
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun createUpdatedUser(currentUser: User, updates: Map<String, Any>): User {
        return currentUser.copy(
            displayName = updates["displayName"] as? String ?: currentUser.displayName,
            bio = updates["bio"] as? String ?: currentUser.bio,
            website = updates["website"] as? String ?: currentUser.website,
            location = updates["location"] as? String ?: currentUser.location,
            avatarUrl = updates["avatarUrl"] as? String ?: currentUser.avatarUrl,
            bannerUrl = updates["bannerUrl"] as? String ?: currentUser.bannerUrl,
            interests = updates["interests"] as? List<String> ?: currentUser.interests,
            theme = updates["theme"] as? String ?: currentUser.theme
        )
    }

    // Follow/Unfollow functionality
    fun toggleFollow(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val followRef = db.collection("follows")
                    .document("${currentUserId}_$targetUserId")

                val existingFollow = followRef.get().await()

                if (existingFollow.exists()) {
                    // Unfollow
                    followRef.delete().await()

                    // Update counts
                    db.collection("users").document(currentUserId)
                        .update("followingCount", FieldValue.increment(-1)).await()

                    db.collection("users").document(targetUserId)
                        .update("followersCount", FieldValue.increment(-1)).await()

                    // Update local state
                    updateLocalUserCounts(targetUserId, followingDelta = -1, followersDelta = -1)
                } else {
                    // Follow
                    val followData = mapOf(
                        "followerId" to currentUserId,
                        "followedId" to targetUserId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    followRef.set(followData).await()

                    // Update counts
                    db.collection("users").document(currentUserId)
                        .update("followingCount", FieldValue.increment(1)).await()

                    db.collection("users").document(targetUserId)
                        .update("followersCount", FieldValue.increment(1)).await()

                    // Update local state
                    updateLocalUserCounts(targetUserId, followingDelta = 1, followersDelta = 1)
                }
            } catch (e: Exception) {
                _error.value = "Failed to update follow status: ${e.message}"
            }
        }
    }

    private fun updateLocalUserCounts(
        targetUserId: String,
        followingDelta: Int,
        followersDelta: Int
    ) {
        // Update current user's following count
        val currentUser = _currentUser.value
        if (currentUser != null && currentUser.id == auth.currentUser?.uid) {
            _currentUser.value = currentUser.copy(
                followingCount = currentUser.followingCount + followingDelta
            )
        }

        // Update viewed user's followers count
        val viewedUser = _viewedUser.value
        if (viewedUser != null && viewedUser.id == targetUserId) {
            _viewedUser.value = viewedUser.copy(
                followersCount = viewedUser.followersCount + followersDelta
            )
        }
    }

    fun clearError() {
        _error.value = null
    }
}