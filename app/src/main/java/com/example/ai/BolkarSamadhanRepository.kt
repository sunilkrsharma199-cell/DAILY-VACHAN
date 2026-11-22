package com.example.ai

import android.util.Log
import com.example.data.local.DevotionalEntity
import com.example.data.local.OfflineDevotionalCatalog
import com.example.data.model.DevotionalLanguage
import com.example.data.model.TimeSlot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

class BolkarSamadhanRepository {

    companion object {
        private const val TAG = "BolkarSamadhanRepo"
        private const val MODEL_NAME = "gemini-3.8-flash"
    }

    private val devotionalSchema = Schema.obj(
        properties = mapOf(
            "verseReference" to Schema.string(description = "Bible book, chapter and verse reference (e.g. Philippians 4:6-7)"),
            "verseText" to Schema.string(description = "Accurate, comforting Bible verse text in the requested language"),
            "reflection" to Schema.string(description = "Warm, empathetic 3-4 sentence devotional message in simple everyday spoken tone"),
            "shortPrayer" to Schema.string(description = "Short, personal 2-sentence closing prayer in the requested language"),
            "moodTag" to Schema.string(description = "1-2 word emotion or theme label like Peace, Strength, Guidance")
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = devotionalSchema
                temperature = 0.7f
            }
        )
    }

    suspend fun askBibleForSituation(
        situationText: String,
        language: DevotionalLanguage
    ): DevotionalEntity {
        val prompt = """
            You are a gentle, warm spiritual devotional companion for everyday users in India.
            The user shared how they are feeling or what they are going through:
            "$situationText"

            Respond in: ${language.aiLanguageInstruction}
            Rules:
            - Choose a deeply comforting, authentic Bible verse directly relevant to their situation.
            - Keep the reflection warm, natural, conversational, and easy to listen to aloud (no robotic or overly academic wording).
            - Keep the prayer personal and hopeful, ending with Amen / आमीन as appropriate for the language.
        """.trimIndent()

        return invokeStructuredGeneration(
            prompt = prompt,
            id = "samadhan_${UUID.randomUUID()}",
            dateKey = OfflineDevotionalCatalog.dateKeyOffset(0),
            timeSlot = TimeSlot.SAMADHAN.key,
            language = language
        )
    }

    suspend fun generateDailyDevotional(
        dateKey: String,
        slot: TimeSlot,
        language: DevotionalLanguage
    ): DevotionalEntity {
        val timeLabel = if (slot == TimeSlot.MORNING) "Morning Dawn" else "Evening Night Rest"
        val prompt = """
            You are creating a short $timeLabel Bible voice devotional for a single user for $dateKey.
            Respond in: ${language.aiLanguageInstruction}
            Rules:
            - Pick an encouraging, well-loved Bible verse suited for $timeLabel.
            - Write a gentle, relatable 3-sentence reflection meant to be listened to in 60 seconds.
            - Write a brief 2-sentence closing prayer.
        """.trimIndent()

        return invokeStructuredGeneration(
            prompt = prompt,
            id = "${dateKey}_${slot.key}_${language.code}",
            dateKey = dateKey,
            timeSlot = slot.key,
            language = language
        )
    }

    private suspend fun invokeStructuredGeneration(
        prompt: String,
        id: String,
        dateKey: String,
        timeSlot: String,
        language: DevotionalLanguage
    ): DevotionalEntity {
        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI companion returned an empty response.")
            val json = JSONObject(rawJson)
            return DevotionalEntity(
                id = id,
                dateKey = dateKey,
                timeSlot = timeSlot,
                languageCode = language.code,
                verseReference = json.getString("verseReference"),
                verseText = json.getString("verseText"),
                reflection = json.getString("reflection"),
                shortPrayer = json.getString("shortPrayer"),
                moodTag = json.optString("moodTag", "Grace").ifBlank { "Grace" },
                createdAt = System.currentTimeMillis()
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI generation timed out", e)
            throw IllegalStateException("Connection took too long (60s). Please check your network and try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Unable to reach the devotional companion right now.",
                e
            )
        }
    }
}
