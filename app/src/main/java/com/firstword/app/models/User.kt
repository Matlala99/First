package com.firstword.app.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId
    val id: String = "",
    val handle: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val authMethods: List<String> = emptyList(), // "google", "phone", "email"
    val region: String = "",
    val interests: List<String> = emptyList(),
    val isGuest: Boolean = false,
    val privacyFlags: Map<String, Boolean> = emptyMap(),
    val referralId: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)
