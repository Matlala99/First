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

class FollowersViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _currentUserFollowingIds = MutableLiveData<Set<String>>()
    val currentUserFollowingIds: LiveData<Set<String>> = _currentUserFollowingIds

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Also load current user's following list to show follow status
            loadCurrentUserFollowing()

            try {
                val followersIds = getFollowerIds(userId)
                val usersList = getUsersByIds(followersIds)
                _users.value = usersList
            } catch (e: Exception) {
                _error.value = "Failed to load followers: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFollowing(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Also load current user's following list to show follow status
            loadCurrentUserFollowing()

            try {
                val followingIds = getFollowingIds(userId)
                val usersList = getUsersByIds(followingIds)
                _users.value = usersList
            } catch (e: Exception) {
                _error.value = "Failed to load following: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadCurrentUserFollowing() {
        val currentUserId = auth.currentUser?.uid ?: return
        try {
            val followingIds = getFollowingIds(currentUserId).toSet()
            _currentUserFollowingIds.value = followingIds
        } catch (e: Exception) {
            // Silently fail or log
        }
    }

    private suspend fun getFollowerIds(userId: String): List<String> {
        return db.collection("follows")
            .whereEqualTo("followedId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("followerId") }
    }

    private suspend fun getFollowingIds(userId: String): List<String> {
        return db.collection("follows")
            .whereEqualTo("followerId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("followedId") }
    }

    private suspend fun getUsersByIds(userIds: List<String>): List<User> {
        if (userIds.isEmpty()) return emptyList()

        val usersList = mutableListOf<User>()

        // Batch get users
        val batches = userIds.chunked(10)

        for (batch in batches) {
            val usersSnapshot = db.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                .get()
                .await()

            usersList.addAll(usersSnapshot.documents.mapNotNull { document ->
                document.toObject(User::class.java)?.copy(id = document.id)
            })
        }

        return usersList
    }

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
                } else {
                    // Follow
                    val followData = mapOf(
                        "followerId" to currentUserId,
                        "followedId" to targetUserId,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    followRef.set(followData).await()

                    // Update counts
                    db.collection("users").document(currentUserId)
                        .update("followingCount", FieldValue.increment(1)).await()

                    db.collection("users").document(targetUserId)
                        .update("followersCount", FieldValue.increment(1)).await()
                }

                // Refresh current list and current user's following set
                refreshCurrentList(targetUserId, existingFollow.exists())

                // Update the following state for the UI
                val currentSet = _currentUserFollowingIds.value?.toMutableSet() ?: mutableSetOf()
                if (existingFollow.exists()) {
                    currentSet.remove(targetUserId)
                } else {
                    currentSet.add(targetUserId)
                }
                _currentUserFollowingIds.value = currentSet

            } catch (e: Exception) {
                _error.value = "Failed to update follow status: ${e.message}"
            }
        }
    }

    private fun refreshCurrentList(targetUserId: String, wasFollowing: Boolean) {
        val currentList = _users.value ?: return

        val updatedList = currentList.map { user ->
            if (user.id == targetUserId) {
                if (wasFollowing) {
                    user.copy(followersCount = user.followersCount - 1)
                } else {
                    user.copy(followersCount = user.followersCount + 1)
                }
            } else {
                user
            }
        }

        _users.value = updatedList
    }

    fun clearError() {
        _error.value = null
    }
}