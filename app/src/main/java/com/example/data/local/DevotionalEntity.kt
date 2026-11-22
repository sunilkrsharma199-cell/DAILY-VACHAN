package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devotionals")
data class DevotionalEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val timeSlot: String,
    val languageCode: String,
    val verseReference: String,
    val verseText: String,
    val reflection: String,
    val shortPrayer: String,
    val moodTag: String,
    val audioCached: Boolean = false,
    val isBookmarked: Boolean = false,
    val isPlayed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun fullSpokenScript(): String {
        return buildString {
            append(verseReference)
            append(". ")
            append(verseText)
            append(" ... ")
            append(reflection)
            append(" ... ")
            append(shortPrayer)
        }
    }
}
