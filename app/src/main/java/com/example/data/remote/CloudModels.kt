package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val selectedLanguage: String = "en",
    val morningEnabled: Boolean = true,
    val eveningEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudSavedVerse(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val timeSlot: String = "MORNING",
    val languageCode: String = "en",
    val verseReference: String = "",
    val verseText: String = "",
    val reflection: String = "",
    val shortPrayer: String = "",
    val moodTag: String = "Peace",
    val bookmarked: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
