package com.example.data.local

import com.example.data.model.DevotionalLanguage
import com.example.data.model.TimeSlot
import kotlinx.coroutines.flow.Flow

class DevotionalRepository(
    private val dao: DevotionalDao
) {
    fun observeDayDevotionals(dateKey: String, language: DevotionalLanguage): Flow<List<DevotionalEntity>> {
        return dao.observeDayDevotionals(dateKey, language.code)
    }

    fun observeAllHistory(): Flow<List<DevotionalEntity>> = dao.observeAllHistory()

    fun observeBookmarked(): Flow<List<DevotionalEntity>> = dao.observeBookmarked()

    fun observeSamadhanHistory(): Flow<List<DevotionalEntity>> = dao.observeSamadhanHistory()

    suspend fun getById(id: String): DevotionalEntity? = dao.getById(id)

    suspend fun ensureSeededForLanguage(language: DevotionalLanguage) {
        val toInsert = mutableListOf<DevotionalEntity>()
        // Seed yesterday, today, and tomorrow so Today, History, and Offline Pre-Cache are ready
        for (offset in listOf(-1, 0, 1)) {
            val dateKey = OfflineDevotionalCatalog.dateKeyOffset(offset)
            val seedIndex = OfflineDevotionalCatalog.dayIndexForDateKey(dateKey)
            for (slot in listOf(TimeSlot.MORNING, TimeSlot.EVENING)) {
                val existing = dao.getByDateSlotAndLanguage(dateKey, slot.key, language.code)
                if (existing == null) {
                    toInsert.add(
                        OfflineDevotionalCatalog.buildDevotionalForDate(
                            dateKey = dateKey,
                            slot = slot,
                            language = language,
                            daySeedIndex = seedIndex
                        )
                    )
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            dao.upsertAll(toInsert)
        }
    }

    suspend fun getOrCreateTodayDevotional(
        slot: TimeSlot,
        language: DevotionalLanguage,
        daysOffset: Int = 0
    ): DevotionalEntity {
        val dateKey = OfflineDevotionalCatalog.dateKeyOffset(daysOffset)
        val existing = dao.getByDateSlotAndLanguage(dateKey, slot.key, language.code)
        if (existing != null) return existing
        val created = OfflineDevotionalCatalog.buildDevotionalForDate(
            dateKey = dateKey,
            slot = slot,
            language = language,
            daySeedIndex = OfflineDevotionalCatalog.dayIndexForDateKey(dateKey)
        )
        dao.upsert(created)
        return created
    }

    suspend fun upsert(entity: DevotionalEntity) = dao.upsert(entity)

    suspend fun toggleBookmark(entity: DevotionalEntity) {
        dao.updateBookmark(entity.id, !entity.isBookmarked)
    }

    suspend fun setBookmarked(id: String, bookmarked: Boolean) {
        dao.updateBookmark(id, bookmarked)
    }

    suspend fun markPlayed(id: String) = dao.markPlayed(id)

    suspend fun markAudioCached(id: String, cached: Boolean) = dao.markAudioCached(id, cached)
}
