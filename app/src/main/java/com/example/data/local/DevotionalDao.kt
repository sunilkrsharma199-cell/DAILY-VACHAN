package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DevotionalDao {

    @Query("SELECT * FROM devotionals WHERE dateKey = :dateKey AND languageCode = :languageCode AND timeSlot != 'SAMADHAN' ORDER BY timeSlot ASC")
    fun observeDayDevotionals(dateKey: String, languageCode: String): Flow<List<DevotionalEntity>>

    @Query("SELECT * FROM devotionals WHERE dateKey = :dateKey AND timeSlot = :timeSlot AND languageCode = :languageCode LIMIT 1")
    suspend fun getByDateSlotAndLanguage(
        dateKey: String,
        timeSlot: String,
        languageCode: String
    ): DevotionalEntity?

    @Query("SELECT * FROM devotionals ORDER BY createdAt DESC")
    fun observeAllHistory(): Flow<List<DevotionalEntity>>

    @Query("SELECT * FROM devotionals WHERE isBookmarked = 1 ORDER BY createdAt DESC")
    fun observeBookmarked(): Flow<List<DevotionalEntity>>

    @Query("SELECT * FROM devotionals WHERE timeSlot = 'SAMADHAN' ORDER BY createdAt DESC")
    fun observeSamadhanHistory(): Flow<List<DevotionalEntity>>

    @Query("SELECT * FROM devotionals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DevotionalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DevotionalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DevotionalEntity>)

    @Query("UPDATE devotionals SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean)

    @Query("UPDATE devotionals SET isPlayed = 1 WHERE id = :id")
    suspend fun markPlayed(id: String)

    @Query("UPDATE devotionals SET audioCached = :cached WHERE id = :id")
    suspend fun markAudioCached(id: String, cached: Boolean)
}
