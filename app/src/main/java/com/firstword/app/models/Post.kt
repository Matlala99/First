package com.firstword.app.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val videoUrl: String = "",

    // User information
    val userId: String = "",
    val userHandle: String = "",
    val userDisplayName: String = "",
    val userAvatarUrl: String = "",

    // Post metadata
    val category: String = "general",
    val sourceUrl: String = "",
    val location: PostLocation? = null, // Corrected to PostLocation
    val tags: List<String> = emptyList(),
    val newsSource: String = "",

    // Engagement metrics
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,

    // Authenticity voting
    val authenticityVotes: AuthenticityVotes = AuthenticityVotes(),

    // Timestamps
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,

    // Moderation flags
    val isVerified: Boolean = false,
    val isReported: Boolean = false,
    val isHidden: Boolean = false
)

data class PostLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val placeName: String? = null
)

data class AuthenticityVotes(
    val trueCount: Int = 0,
    val fakeCount: Int = 0,
    val aiCount: Int = 0,
    val totalCount: Int = 0
) {
    // Optional: Add helper methods here if needed
    fun getAuthenticityPercentage(): Map<String, Double> {
        return if (totalCount == 0) {
            mapOf("true" to 0.0, "fake" to 0.0, "ai" to 0.0)
        } else {
            mapOf(
                "true" to (trueCount.toDouble() / totalCount * 100),
                "fake" to (fakeCount.toDouble() / totalCount * 100),
                "ai" to (aiCount.toDouble() / totalCount * 100)
            )
        }
    }
}