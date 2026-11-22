package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.local.DevotionalEntity
import com.example.data.model.DevotionalLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

object OfflineAudioCacheManager {

    private const val TAG = "OfflineAudioCache"
    private const val AUDIO_DIR = "devotional_audio"

    private fun audioDir(context: Context): File {
        return File(context.filesDir, AUDIO_DIR).apply { if (!exists()) mkdirs() }
    }

    fun getAudioFile(context: Context, devotionalId: String): File {
        val safeName = devotionalId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(audioDir(context), "$safeName.wav")
    }

    fun hasCachedAudio(context: Context, devotionalId: String): Boolean {
        val file = getAudioFile(context, devotionalId)
        return file.exists() && file.length() > 1024L
    }

    fun getCacheStats(context: Context): Pair<Int, Long> {
        val files = audioDir(context).listFiles()?.filter { it.isFile && it.length() > 0 } ?: emptyList()
        val totalBytes = files.sumOf { it.length() }
        return files.size to totalBytes
    }

    fun clearCache(context: Context) {
        audioDir(context).listFiles()?.forEach { it.delete() }
    }

    suspend fun synthesizeToCache(
        context: Context,
        entity: DevotionalEntity,
        speechRate: Float = 0.95f
    ): Boolean = withContext(Dispatchers.IO) {
        val outFile = getAudioFile(context, entity.id)
        if (outFile.exists() && outFile.length() > 1024L) {
            return@withContext true
        }

        val result = withTimeoutOrNull(35_000L) {
            suspendCancellableCoroutine<Boolean> { cont ->
                var tts: TextToSpeech? = null
                val utteranceId = "cache_${entity.id}"

                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status != TextToSpeech.SUCCESS) {
                        tts?.shutdown()
                        if (cont.isActive) cont.resume(false)
                        return@TextToSpeech
                    }
                    try {
                        val lang = DevotionalLanguage.fromCode(entity.languageCode)
                        val locale = Locale.forLanguageTag(lang.ttsLocaleTag)
                        val availability = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                        if (availability == TextToSpeech.LANG_MISSING_DATA ||
                            availability == TextToSpeech.LANG_NOT_SUPPORTED
                        ) {
                            tts?.setLanguage(Locale("en", "IN"))
                        }
                        tts?.setSpeechRate(speechRate)
                        tts?.setPitch(0.98f)
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(id: String?) {}
                            override fun onDone(id: String?) {
                                tts?.shutdown()
                                if (cont.isActive) cont.resume(outFile.exists() && outFile.length() > 512L)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(id: String?) {
                                tts?.shutdown()
                                if (cont.isActive) cont.resume(false)
                            }

                            override fun onError(id: String?, errorCode: Int) {
                                tts?.shutdown()
                                if (cont.isActive) cont.resume(false)
                            }
                        })
                        val params = Bundle()
                        val res = tts?.synthesizeToFile(
                            entity.fullSpokenScript(),
                            params,
                            outFile,
                            utteranceId
                        )
                        if (res != TextToSpeech.SUCCESS) {
                            tts?.shutdown()
                            if (cont.isActive) cont.resume(false)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Synthesis error", e)
                        tts?.shutdown()
                        if (cont.isActive) cont.resume(false)
                    }
                }

                cont.invokeOnCancellation {
                    try {
                        tts?.stop()
                        tts?.shutdown()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        result ?: false
    }
}
