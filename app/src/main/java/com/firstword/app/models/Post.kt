package com.firstword.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class AuthenticityVotes(
    val trueCount: Int = 0,
    val fakeCount: Int = 0,
    val aiCount: Int = 0
) {
    @Exclude
    fun getTotalCount(): Int = trueCount + fakeCount + aiCount

    @Exclude
    fun getAuthenticityPercentage(): Map<String, Double> {
        val total = getTotalCount().toDouble() // FIXED: Convert to Double for division
        return if (total > 0) {
            mapOf(
                "true" to (trueCount.toDouble() / total * 100),
                "fake" to (fakeCount.toDouble() / total * 100),
                "ai" to (aiCount.toDouble() / total * 100)
            )
        } else {
            mapOf("true" to 0.0, "fake" to 0.0, "ai" to 0.0)
        }
    }
}

data class Post(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userHandle: String = "",
    val userAvatarUrl: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val category: String = "general",

    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,

    val authenticityVotes: AuthenticityVotes = AuthenticityVotes(),

    // Location field
    val location: PostLocation? = null,

    // Additional metadata
    val isHidden: Boolean = false,
    val reportCount: Int = 0,

    @ServerTimestamp
    val createdAt: Date? = null,

    @ServerTimestamp
    val updatedAt: Date? = null
) {
    // Firebase requires a no-argument constructor
    constructor() : this("", "", "", "", "", "", "", "", "general",
        0, 0, 0, AuthenticityVotes(), null, false, 0, null, null)

    companion object {
        fun fromDocument(document: com.google.firebase.firestore.DocumentSnapshot): Post {
            return try {
                val data = document.data ?: return Post()

                // Extract authenticity votes
                val votesData = data["authenticityVotes"] as? Map<String, Any>
                val authenticityVotes = AuthenticityVotes(
                    trueCount = (votesData?.get("trueCount") as? Long)?.toInt() ?: 0,
                    fakeCount = (votesData?.get("fakeCount") as? Long)?.toInt() ?: 0,
                    aiCount = (votesData?.get("aiCount") as? Long)?.toInt() ?: 0
                )

                // Extract location
                val locationData = data["location"] as? Map<String, Any>
                val location = if (locationData != null) {
                    PostLocation(
                        latitude = (locationData["latitude"] as? Double) ?: 0.0,
                        longitude = (locationData["longitude"] as? Double) ?: 0.0,
                        address = locationData["address"] as? String,
                        placeName = locationData["placeName"] as? String,
                        accuracy = (locationData["accuracy"] as? Double) ?: 0.0
                    )
                } else null

                // Convert timestamp to date
                fun toDate(timestamp: Any?): Date? {
                    return when (timestamp) {
                        is Timestamp -> timestamp.toDate()
                        is Date -> timestamp
                        else -> null
                    }
                }

                Post(
                    id = document.id,
                    userId = data["userId"] as? String ?: "",
                    userDisplayName = data["userDisplayName"] as? String ?: "",
                    userHandle = data["userHandle"] as? String ?: "",
                    userAvatarUrl = data["userAvatarUrl"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    content = data["content"] as? String ?: "",
                    imageUrl = data["imageUrl"] as? String ?: "",
                    category = data["category"] as? String ?: "general",
                    likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0,
                    commentsCount = (data["commentsCount"] as? Long)?.toInt() ?: 0,
                    sharesCount = (data["sharesCount"] as? Long)?.toInt() ?: 0,
                    authenticityVotes = authenticityVotes,
                    location = location,
                    isHidden = data["isHidden"] as? Boolean ?: false,
                    reportCount = (data["reportCount"] as? Long)?.toInt() ?: 0,
                    createdAt = toDate(data["createdAt"]),
                    updatedAt = toDate(data["updatedAt"])
                )
            } catch (e: Exception) {
                // Return empty post on error
                Post()
            }
        }
    }

    fun toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("userId", userId)
            put("userDisplayName", userDisplayName)
            put("userHandle", userHandle)
            put("userAvatarUrl", userAvatarUrl)
            put("title", title)
            put("content", content)
            put("imageUrl", imageUrl)
            put("category", category)
            put("likesCount", likesCount)
            put("commentsCount", commentsCount)
            put("sharesCount", sharesCount)
            put("authenticityVotes", mapOf(
                "trueCount" to authenticityVotes.trueCount,
                "fakeCount" to authenticityVotes.fakeCount,
                "aiCount" to authenticityVotes.aiCount
            ))
            location?.let {
                put("location", mapOf(
                    "latitude" to it.latitude,
                    "longitude" to it.longitude,
                    "address" to (it.address ?: ""),
                    "placeName" to (it.placeName ?: ""),
                    "accuracy" to it.accuracy
                ))
            }
            put("isHidden", isHidden)
            put("reportCount", reportCount)
        }
    }

    @Exclude
    fun hasLocation(): Boolean {
        return location != null
    }
}

data class PostLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val placeName: String? = null,
    val accuracy: Double = 0.0
)