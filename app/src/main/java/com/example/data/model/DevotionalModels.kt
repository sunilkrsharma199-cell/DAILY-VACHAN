package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

enum class DevotionalLanguage(
    val code: String,
    val displayName: String,
    val nativeLabel: String,
    val ttsLocaleTag: String,
    val aiLanguageInstruction: String
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeLabel = "English",
        ttsLocaleTag = "en-IN",
        aiLanguageInstruction = "Simple, warm everyday Indian English that is comforting and easy to understand."
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeLabel = "हिन्दी",
        ttsLocaleTag = "hi-IN",
        aiLanguageInstruction = "Simple, natural, conversational Hindi in Devanagari script (सरल और आत्मीय हिन्दी) without overly complex Sanskritized words."
    ),
    HINGLISH(
        code = "hinglish",
        displayName = "Hinglish",
        nativeLabel = "Hinglish",
        ttsLocaleTag = "hi-IN",
        aiLanguageInstruction = "Natural, warm conversational Hinglish (everyday Hindi written in Roman/Latin script mixed with simple English words, like a caring elder or friend speaking)."
    ),
    TAMIL(
        code = "ta",
        displayName = "Tamil",
        nativeLabel = "தமிழ்",
        ttsLocaleTag = "ta-IN",
        aiLanguageInstruction = "Simple, warm, everyday Tamil in Tamil script that is comforting and easy to follow."
    ),
    TELUGU(
        code = "te",
        displayName = "Telugu",
        nativeLabel = "తెలుగు",
        ttsLocaleTag = "te-IN",
        aiLanguageInstruction = "Simple, warm, everyday Telugu in Telugu script that is gentle and reassuring."
    ),
    MARATHI(
        code = "mr",
        displayName = "Marathi",
        nativeLabel = "मराठी",
        ttsLocaleTag = "mr-IN",
        aiLanguageInstruction = "Simple, warm, everyday Marathi in Devanagari script that feels personal and peaceful."
    );

    companion object {
        fun fromCode(code: String?): DevotionalLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

enum class TimeSlot(val key: String, val label: String) {
    MORNING("MORNING", "Morning"),
    EVENING("EVENING", "Evening"),
    SAMADHAN("SAMADHAN", "Samadhan");

    companion object {
        fun fromKey(key: String?): TimeSlot {
            return entries.firstOrNull { it.key == key } ?: MORNING
        }
    }
}

enum class MoodPreset(
    val id: String,
    val englishTitle: String,
    val hindiSubtitle: String,
    val promptContext: String
) {
    PEACE("peace", "Peace", "शांति", "I want to feel God's calm and quiet peace in my heart today."),
    ANXIETY("anxiety", "Anxiety", "चिंता", "I am feeling anxious and overthinking about tomorrow and responsibilities."),
    STRENGTH("strength", "Strength", "ताकत", "I feel tired and weak and need inner strength to keep going."),
    FEAR("fear", "Fear", "डर", "I am feeling afraid and uncertain about what lies ahead."),
    GRATITUDE("gratitude", "Gratitude", "धन्यवाद", "My heart is thankful and I want to thank God for His blessings."),
    GUIDANCE("guidance", "Guidance", "राह", "I need clarity and wisdom for an important decision in my life or work.")
}

enum class StatusCardTheme(
    val id: String,
    val title: String,
    @DrawableRes val drawableRes: Int
) {
    DAWN_SUNRISE("dawn", "Golden Dawn", R.drawable.img_dawn_sunrise),
    EVENING_LANTERN("lantern", "Evening Lantern", R.drawable.img_evening_lantern),
    QUIET_WATERS("waters", "Quiet Waters", R.drawable.img_quiet_waters)
}

data class UserPreferences(
    val selectedLanguage: DevotionalLanguage = DevotionalLanguage.ENGLISH,
    val morningEnabled: Boolean = true,
    val morningHour: Int = 6,
    val morningMinute: Int = 30,
    val eveningEnabled: Boolean = true,
    val eveningHour: Int = 20,
    val eveningMinute: Int = 30,
    val oneTapNotificationAudio: Boolean = true,
    val ambientMusicEnabled: Boolean = true,
    val ambientMusicVolume: Float = 0.28f,
    val autoDownloadOffline: Boolean = true,
    val speechRate: Float = 0.95f,
    val phoneNumber: String = "",
    val welcomeNotificationSent: Boolean = false
)
