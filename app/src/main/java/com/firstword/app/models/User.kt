package com.firstword.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class User(
    @DocumentId
    val id: String = "",
    val handle: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val bio: String = "",
    val website: String = "",
    val location: String = "",
    val authMethods: List<String> = emptyList(), // "google", "phone", "email", "anonymous"
    val region: String = "",
    val interests: List<String> = emptyList(),
    val isGuest: Boolean = false,
    val isVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val isBanned: Boolean = false,
    val privacyFlags: Map<String, Boolean> = mapOf(
        "showEmail" to false,
        "showPhone" to false,
        "showFollowers" to true,
        "showFollowing" to true,
        "showPosts" to true,
        "allowMessages" to true,
        "allowTags" to true,
        "showOnlineStatus" to true
    ),
    val referralId: String = "",
    val referredBy: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val notificationsEnabled: Boolean = true,
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val theme: String = "system", // "light", "dark", "system"
    val language: String = "en",
    val lastSeen: Date? = null,
    val isOnline: Boolean = false,

    @ServerTimestamp
    val createdAt: Date? = null,

    @ServerTimestamp
    val updatedAt: Date? = null
) {
    // Firebase requires a no-argument constructor
    constructor() : this(
        "", "", "", "", "", "", "", "", "", "",
        emptyList(), "", emptyList(), false, false, false, false,
        emptyMap(), "", "", 0, 0, 0, 0, 0, 0,
        true, true, true, "system", "en", null, false, null, null
    )

    companion object {
        // Factory method to create User from Firestore document
        fun fromDocument(document: com.google.firebase.firestore.DocumentSnapshot): User {
            return try {
                val data = document.data ?: return User()

                // Extract privacy flags
                val privacyFlagsData = data["privacyFlags"] as? Map<String, Boolean> ?: emptyMap()

                User(
                    id = document.id,
                    handle = data["handle"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    phoneNumber = data["phoneNumber"] as? String ?: "",
                    avatarUrl = data["avatarUrl"] as? String ?: "",
                    bannerUrl = data["bannerUrl"] as? String ?: "",
                    bio = data["bio"] as? String ?: "",
                    website = data["website"] as? String ?: "",
                    location = data["location"] as? String ?: "",
                    authMethods = (data["authMethods"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    region = data["region"] as? String ?: "",
                    interests = (data["interests"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    isGuest = data["isGuest"] as? Boolean ?: false,
                    isVerified = data["isVerified"] as? Boolean ?: false,
                    isPrivate = data["isPrivate"] as? Boolean ?: false,
                    isBanned = data["isBanned"] as? Boolean ?: false,
                    privacyFlags = privacyFlagsData,
                    referralId = data["referralId"] as? String ?: "",
                    referredBy = data["referredBy"] as? String ?: "",
                    followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0,
                    postsCount = (data["postsCount"] as? Long)?.toInt() ?: 0,
                    likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0,
                    commentsCount = (data["commentsCount"] as? Long)?.toInt() ?: 0,
                    sharesCount = (data["sharesCount"] as? Long)?.toInt() ?: 0,
                    notificationsEnabled = data["notificationsEnabled"] as? Boolean ?: true,
                    emailNotifications = data["emailNotifications"] as? Boolean ?: true,
                    pushNotifications = data["pushNotifications"] as? Boolean ?: true,
                    theme = data["theme"] as? String ?: "system",
                    language = data["language"] as? String ?: "en",
                    lastSeen = convertToDate(data["lastSeen"]),
                    isOnline = data["isOnline"] as? Boolean ?: false,
                    createdAt = convertToDate(data["createdAt"]),
                    updatedAt = convertToDate(data["updatedAt"])
                )
            } catch (e: Exception) {
                // Return empty user on error
                User()
            }
        }

        // Factory method to create User from Map
        fun fromMap(map: Map<String, Any>): User {
            return try {
                // Extract privacy flags
                val privacyFlagsData = map["privacyFlags"] as? Map<String, Boolean> ?: emptyMap()

                User(
                    id = map["id"] as? String ?: "",
                    handle = map["handle"] as? String ?: "",
                    displayName = map["displayName"] as? String ?: "",
                    email = map["email"] as? String ?: "",
                    phoneNumber = map["phoneNumber"] as? String ?: "",
                    avatarUrl = map["avatarUrl"] as? String ?: "",
                    bannerUrl = map["bannerUrl"] as? String ?: "",
                    bio = map["bio"] as? String ?: "",
                    website = map["website"] as? String ?: "",
                    location = map["location"] as? String ?: "",
                    authMethods = (map["authMethods"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    region = map["region"] as? String ?: "",
                    interests = (map["interests"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    isGuest = map["isGuest"] as? Boolean ?: false,
                    isVerified = map["isVerified"] as? Boolean ?: false,
                    isPrivate = map["isPrivate"] as? Boolean ?: false,
                    isBanned = map["isBanned"] as? Boolean ?: false,
                    privacyFlags = privacyFlagsData,
                    referralId = map["referralId"] as? String ?: "",
                    referredBy = map["referredBy"] as? String ?: "",
                    followersCount = (map["followersCount"] as? Long)?.toInt() ?: 0,
                    followingCount = (map["followingCount"] as? Long)?.toInt() ?: 0,
                    postsCount = (map["postsCount"] as? Long)?.toInt() ?: 0,
                    likesCount = (map["likesCount"] as? Long)?.toInt() ?: 0,
                    commentsCount = (map["commentsCount"] as? Long)?.toInt() ?: 0,
                    sharesCount = (map["sharesCount"] as? Long)?.toInt() ?: 0,
                    notificationsEnabled = map["notificationsEnabled"] as? Boolean ?: true,
                    emailNotifications = map["emailNotifications"] as? Boolean ?: true,
                    pushNotifications = map["pushNotifications"] as? Boolean ?: true,
                    theme = map["theme"] as? String ?: "system",
                    language = map["language"] as? String ?: "en",
                    lastSeen = convertToDate(map["lastSeen"]),
                    isOnline = map["isOnline"] as? Boolean ?: false,
                    createdAt = convertToDate(map["createdAt"]),
                    updatedAt = convertToDate(map["updatedAt"])
                )
            } catch (e: Exception) {
                // Return empty user on error
                User()
            }
        }

        private fun convertToDate(timestamp: Any?): Date? {
            return when (timestamp) {
                is Timestamp -> timestamp.toDate()
                is Date -> timestamp
                is Long -> Date(timestamp)
                else -> null
            }
        }

        // Create a default user from Firebase Auth user
        fun fromFirebaseUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
            return User(
                id = firebaseUser.uid,
                handle = generateHandle(firebaseUser.displayName ?: "user"),
                displayName = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                phoneNumber = firebaseUser.phoneNumber ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                isGuest = firebaseUser.isAnonymous,
                authMethods = listOf(if (firebaseUser.isAnonymous) "anonymous" else "email"),
                referralId = generateReferralId(),
                createdAt = Date(),
                updatedAt = Date()
            )
        }

        private fun generateHandle(displayName: String): String {
            val baseHandle = displayName.lowercase()
                .replace(" ", "_")
                .replace(Regex("[^a-z0-9_]"), "")
                .take(15) // Limit length
            return if (baseHandle.isEmpty()) {
                "user_${System.currentTimeMillis().toString().takeLast(6)}"
            } else {
                "${baseHandle}_${System.currentTimeMillis().toString().takeLast(4)}"
            }
        }

        private fun generateReferralId(): String {
            return "REF_${System.currentTimeMillis()}_${(1000..9999).random()}"
        }
    }

    // Convert User to Map for Firestore
    fun toMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("handle", handle)
            put("displayName", displayName)
            put("email", email)
            put("phoneNumber", phoneNumber)
            put("avatarUrl", avatarUrl)
            put("bannerUrl", bannerUrl)
            put("bio", bio)
            put("website", website)
            put("location", location)
            put("authMethods", authMethods)
            put("region", region)
            put("interests", interests)
            put("isGuest", isGuest)
            put("isVerified", isVerified)
            put("isPrivate", isPrivate)
            put("isBanned", isBanned)
            put("privacyFlags", privacyFlags)
            put("referralId", referralId)
            put("referredBy", referredBy)
            put("followersCount", followersCount)
            put("followingCount", followingCount)
            put("postsCount", postsCount)
            put("likesCount", likesCount)
            put("commentsCount", commentsCount)
            put("sharesCount", sharesCount)
            put("notificationsEnabled", notificationsEnabled)
            put("emailNotifications", emailNotifications)
            put("pushNotifications", pushNotifications)
            put("theme", theme)
            put("language", language)
            put("isOnline", isOnline)
            // Note: createdAt, updatedAt, lastSeen should be set with FieldValue.serverTimestamp()
        }
    }

    // Excluded properties (not stored in Firestore)
    @Exclude
    fun getFullName(): String {
        return if (displayName.isNotEmpty()) displayName else handle
    }

    @Exclude
    fun getFormattedHandle(): String {
        return "@$handle"
    }

    @Exclude
    fun getInitials(): String {
        return if (displayName.isNotEmpty()) {
            displayName.split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
        } else if (handle.isNotEmpty()) {
            handle.take(2).uppercase()
        } else {
            "U"
        }
    }

    @Exclude
    fun hasProfilePicture(): Boolean {
        return avatarUrl.isNotEmpty()
    }

    @Exclude
    fun hasBannerPicture(): Boolean {
        return bannerUrl.isNotEmpty()
    }

    @Exclude
    fun isCompleteProfile(): Boolean {
        return displayName.isNotEmpty() &&
                avatarUrl.isNotEmpty() &&
                bio.isNotEmpty() &&
                interests.isNotEmpty()
    }

    @Exclude
    fun getCompletionPercentage(): Int {
        var completed = 0
        var total = 4

        if (displayName.isNotEmpty()) completed++
        if (avatarUrl.isNotEmpty()) completed++
        if (bio.isNotEmpty()) completed++
        if (interests.isNotEmpty()) completed++

        return (completed * 100) / total
    }

    @Exclude
    fun canSendMessage(): Boolean {
        return privacyFlags["allowMessages"] ?: true
    }

    @Exclude
    fun canBeTagged(): Boolean {
        return privacyFlags["allowTags"] ?: true
    }

    @Exclude
    fun showFollowers(): Boolean {
        return privacyFlags["showFollowers"] ?: true
    }

    @Exclude
    fun showFollowing(): Boolean {
        return privacyFlags["showFollowing"] ?: true
    }

    @Exclude
    fun showPosts(): Boolean {
        return privacyFlags["showPosts"] ?: true
    }

    @Exclude
    fun showOnlineStatus(): Boolean {
        return privacyFlags["showOnlineStatus"] ?: true
    }

    @Exclude
    fun getStatsSummary(): Map<String, Int> {
        return mapOf(
            "posts" to postsCount,
            "followers" to followersCount,
            "following" to followingCount,
            "likes" to likesCount,
            "comments" to commentsCount,
            "shares" to sharesCount
        )
    }

    @Exclude
    fun getFormattedStats(): Map<String, String> {
        fun formatCount(count: Int): String {
            return when {
                count < 1000 -> count.toString()
                count < 1000000 -> String.format("%.1fk", count / 1000.0)
                else -> String.format("%.1fM", count / 1000000.0)
            }
        }

        return mapOf(
            "posts" to formatCount(postsCount),
            "followers" to formatCount(followersCount),
            "following" to formatCount(followingCount)
        )
    }

    // Update methods (return new instances with updated values)
    fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        website: String? = null,
        location: String? = null,
        interests: List<String>? = null,
        avatarUrl: String? = null,
        bannerUrl: String? = null
    ): User {
        return this.copy(
            displayName = displayName ?: this.displayName,
            bio = bio ?: this.bio,
            website = website ?: this.website,
            location = location ?: this.location,
            interests = interests ?: this.interests,
            avatarUrl = avatarUrl ?: this.avatarUrl,
            bannerUrl = bannerUrl ?: this.bannerUrl
        )
    }

    fun updateSettings(
        theme: String? = null,
        language: String? = null,
        notificationsEnabled: Boolean? = null,
        emailNotifications: Boolean? = null,
        pushNotifications: Boolean? = null
    ): User {
        return this.copy(
            theme = theme ?: this.theme,
            language = language ?: this.language,
            notificationsEnabled = notificationsEnabled ?: this.notificationsEnabled,
            emailNotifications = emailNotifications ?: this.emailNotifications,
            pushNotifications = pushNotifications ?: this.pushNotifications
        )
    }

    fun updatePrivacy(
        isPrivate: Boolean? = null,
        privacyFlags: Map<String, Boolean>? = null
    ): User {
        return this.copy(
            isPrivate = isPrivate ?: this.isPrivate,
            privacyFlags = privacyFlags ?: this.privacyFlags
        )
    }

    fun incrementFollowers(): User {
        return this.copy(followersCount = this.followersCount + 1)
    }

    fun decrementFollowers(): User {
        return this.copy(followersCount = maxOf(0, this.followersCount - 1))
    }

    fun incrementFollowing(): User {
        return this.copy(followingCount = this.followingCount + 1)
    }

    fun decrementFollowing(): User {
        return this.copy(followingCount = maxOf(0, this.followingCount - 1))
    }

    fun incrementPosts(): User {
        return this.copy(postsCount = this.postsCount + 1)
    }

    fun decrementPosts(): User {
        return this.copy(postsCount = maxOf(0, this.postsCount - 1))
    }

    fun updateLastSeen(): User {
        return this.copy(lastSeen = Date(), isOnline = true)
    }

    fun setOffline(): User {
        return this.copy(isOnline = false)
    }

    // Check methods
    fun isFollowing(currentUserId: String): Boolean {
        // This would typically check against a separate follows collection
        return false // Placeholder - implement based on your follows system
    }

    fun canViewProfile(viewerId: String): Boolean {
        return if (isPrivate) {
            // For private profiles, only allow viewing if:
            // 1. It's the user themselves
            // 2. The viewer is following the user (you'll need to check follows collection)
            viewerId == id || isFollowing(viewerId)
        } else {
            true
        }
    }
}