package com.firstword.app.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeedViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private val _posts = MutableLiveData<List<com.firstword.app.models.Post>>()
    val posts: LiveData<List<com.firstword.app.models.Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var postsListener: ListenerRegistration? = null

    init {
        checkAuthAndLoadPosts()
    }

    private fun checkAuthAndLoadPosts() {
        if (auth.currentUser != null) {
            loadPosts()
        } else {
            _error.value = "Please sign in to view posts"
        }
    }

    fun loadPosts() {
        _isLoading.value = true
        _error.value = null

        try {
            // Remove old listener
            postsListener?.remove()

            // Query posts collection
            val query = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)

            postsListener = query.addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                if (error != null) {
                    // Handle the error directly
                    handleFirestoreError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Use fromDocument instead of toObject
                    val postsList = snapshot.documents.map { document ->
                        try {
                            com.firstword.app.models.Post.fromDocument(document)
                        } catch (e: Exception) {
                            // Log the error but return empty post
                            println("Error parsing post ${document.id}: ${e.message}")
                            com.firstword.app.models.Post()
                        }
                    }
                    _posts.value = postsList
                } else {
                    _posts.value = emptyList()
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _error.value = "Failed to load posts: ${e.message}"
        }
    }

    private fun handleFirestoreError(error: Exception) {
        when (error) {
            is FirebaseFirestoreException -> {
                when (error.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        _error.value = "Permission denied. Please check if you're signed in."
                    }
                    FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                        _error.value = "Please sign in to continue"
                    }
                    FirebaseFirestoreException.Code.UNAVAILABLE -> {
                        _error.value = "Network unavailable. Please check your connection."
                    }
                    else -> {
                        _error.value = "Error loading posts: ${error.message}"
                    }
                }
            }
            else -> {
                _error.value = "Error: ${error.message}"
            }
        }
    }

    fun loadFollowingPosts() {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Get list of users the current user is following
                val followingIds = getFollowingIds(currentUserId)

                if (followingIds.isEmpty()) {
                    _posts.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                // Query posts from followed users
                val query = db.collection("posts")
                    .whereIn("userId", followingIds)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)

                setupPostsListener(query)
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Failed to load following posts: ${e.message}"
            }
        }
    }

    private suspend fun getFollowingIds(userId: String): List<String> {
        return db.collection("follows")
            .whereEqualTo("followerId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("followedId") }
    }

    private fun setupPostsListener(query: Query) {
        postsListener?.remove()

        postsListener = query.addSnapshotListener { snapshot, error ->
            _isLoading.value = false

            if (error != null) {
                handleFirestoreError(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val postsList = snapshot.documents.map { document ->
                    com.firstword.app.models.Post.fromDocument(document)
                }
                _posts.value = postsList
            } else {
                _posts.value = emptyList()
            }
        }
    }

    fun likePost(postId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Please sign in to like posts"
            return
        }

        viewModelScope.launch {
            try {
                val likeRef = db.collection("posts")
                    .document(postId)
                    .collection("likes")
                    .document(currentUser.uid)

                // Check if already liked
                val existingLike = likeRef.get().await()

                val postRef = db.collection("posts").document(postId)

                if (existingLike.exists()) {
                    // Unlike
                    likeRef.delete().await()
                    postRef.update("likesCount", FieldValue.increment(-1)).await()

                    // Update local state
                    updatePostLikes(postId, -1)
                } else {
                    // Like
                    val likeData = mapOf(
                        "userId" to currentUser.uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    likeRef.set(likeData).await()
                    postRef.update("likesCount", FieldValue.increment(1)).await()

                    // Update local state
                    updatePostLikes(postId, 1)
                }
            } catch (e: Exception) {
                _error.value = "Failed to like post: ${e.message}"
            }
        }
    }

    private fun updatePostLikes(postId: String, delta: Int) {
        val currentPosts = _posts.value ?: return
        val updatedPosts = currentPosts.map { post ->
            if (post.id == postId) {
                post.copy(likesCount = post.likesCount + delta)
            } else {
                post
            }
        }
        _posts.value = updatedPosts
    }

    fun voteAuthenticity(postId: String, voteType: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Please sign in to vote"
            return
        }

        if (voteType !in listOf("authentic", "inauthentic", "unsure")) {
            _error.value = "Invalid vote type"
            return
        }

        viewModelScope.launch {
            try {
                val voteRef = db.collection("posts")
                    .document(postId)
                    .collection("authenticity_votes")
                    .document(currentUser.uid)

                val existingVote = voteRef.get().await()
                val postRef = db.collection("posts").document(postId)

                if (existingVote.exists()) {
                    // Update existing vote
                    val previousVote = existingVote.getString("vote")
                    if (previousVote != voteType) {
                        // Decrement previous vote count
                        val previousField = when (previousVote) {
                            "authentic" -> "authenticityVotes.trueCount"
                            "inauthentic" -> "authenticityVotes.fakeCount"
                            "unsure" -> "authenticityVotes.aiCount"
                            else -> return@launch
                        }
                        postRef.update(previousField, FieldValue.increment(-1)).await()

                        // Update vote
                        voteRef.update("vote", voteType).await()

                        // Increment new vote count
                        val newField = when (voteType) {
                            "authentic" -> "authenticityVotes.trueCount"
                            "inauthentic" -> "authenticityVotes.fakeCount"
                            "unsure" -> "authenticityVotes.aiCount"
                            else -> return@launch
                        }
                        postRef.update(newField, FieldValue.increment(1)).await()

                        // Update local state
                        updateAuthenticityVotes(postId, previousVote, voteType)
                    }
                } else {
                    // Create new vote
                    val voteData = mapOf(
                        "userId" to currentUser.uid,
                        "vote" to voteType,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    voteRef.set(voteData).await()

                    // Increment vote count
                    val field = when (voteType) {
                        "authentic" -> "authenticityVotes.trueCount"
                        "inauthentic" -> "authenticityVotes.fakeCount"
                        "unsure" -> "authenticityVotes.aiCount"
                        else -> return@launch
                    }
                    postRef.update(field, FieldValue.increment(1)).await()

                    // Update local state
                    updateAuthenticityVotes(postId, null, voteType)
                }
            } catch (e: Exception) {
                _error.value = "Failed to vote: ${e.message}"
            }
        }
    }

    private fun updateAuthenticityVotes(postId: String, previousVote: String?, newVote: String) {
        val currentPosts = _posts.value ?: return
        val updatedPosts = currentPosts.map { post ->
            if (post.id == postId) {
                val currentVotes = post.authenticityVotes
                val updatedVotes = when {
                    previousVote == null -> when (newVote) {
                        "authentic" -> currentVotes.copy(trueCount = currentVotes.trueCount + 1)
                        "inauthentic" -> currentVotes.copy(fakeCount = currentVotes.fakeCount + 1)
                        "unsure" -> currentVotes.copy(aiCount = currentVotes.aiCount + 1)
                        else -> currentVotes
                    }
                    else -> {
                        val votesAfterRemove = when (previousVote) {
                            "authentic" -> currentVotes.copy(trueCount = currentVotes.trueCount - 1)
                            "inauthentic" -> currentVotes.copy(fakeCount = currentVotes.fakeCount - 1)
                            "unsure" -> currentVotes.copy(aiCount = currentVotes.aiCount - 1)
                            else -> currentVotes
                        }
                        when (newVote) {
                            "authentic" -> votesAfterRemove.copy(trueCount = votesAfterRemove.trueCount + 1)
                            "inauthentic" -> votesAfterRemove.copy(fakeCount = votesAfterRemove.fakeCount + 1)
                            "unsure" -> votesAfterRemove.copy(aiCount = votesAfterRemove.aiCount + 1)
                            else -> votesAfterRemove
                        }
                    }
                }
                post.copy(authenticityVotes = updatedVotes)
            } else {
                post
            }
        }
        _posts.value = updatedPosts
    }

    fun refreshPosts() {
        loadPosts()
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        postsListener?.remove()
    }
}