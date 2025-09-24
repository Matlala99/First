// utils/FirebaseDatabaseManager.kt
package com.firstword.app.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp

class FirebaseDatabaseManager {

    private val db = FirebaseFirestore.getInstance()

    // Create user profile - collection will be auto-created
    fun createUserProfile(
        userId: String,
        displayName: String,
        email: String,
        avatarUrl: String = "",
        isGuest: Boolean = false
    ) {
        val userHandle = generateHandle(displayName)

        val user = hashMapOf(
            "id" to userId,
            "handle" to userHandle,
            "displayName" to displayName,
            "email" to email,
            "avatarUrl" to avatarUrl,
            "isGuest" to isGuest,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        // This will auto-create the "users" collection
        db.collection("users").document(userId).set(user)
    }

    // Create post - collections will be auto-created
    fun createPost(
        userId: String,
        title: String,
        content: String,
        category: String = "general",
        imageUrl: String = ""
    ) {
        val postId = db.collection("posts").document().id

        val post = hashMapOf(
            "id" to postId,
            "title" to title,
            "content" to content,
            "imageUrl" to imageUrl,
            "userId" to userId,
            "category" to category,
            "likesCount" to 0,
            "commentsCount" to 0,
            "createdAt" to Timestamp.now()
        )

        // This will auto-create the "posts" collection
        db.collection("posts").document(postId).set(post)
    }

    // Like a post - collections will be auto-created
    fun likePost(postId: String, userId: String) {
        val likeData = hashMapOf(
            "postId" to postId,
            "userId" to userId,
            "createdAt" to Timestamp.now()
        )

        // Auto-creates "likes" collection
        db.collection("likes").document("${postId}_$userId").set(likeData)

        // Auto-updates the post likes count
        db.collection("posts").document(postId)
            .update("likesCount", FieldValue.increment(1))
    }

    private fun generateHandle(displayName: String): String {
        val baseHandle = displayName.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
        return "${baseHandle}_${System.currentTimeMillis().toString().takeLast(4)}"
    }
}