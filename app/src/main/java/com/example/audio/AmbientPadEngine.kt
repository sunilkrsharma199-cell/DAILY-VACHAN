package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

class AmbientPadEngine {

    private var audioTrack: AudioTrack? = null
    private var isRunning = false
    private var currentVolume = 0.25f

    fun start(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        if (isRunning) {
            setVolume(currentVolume)
            return
        }
        try {
            val sampleRate = 22050
            // 4-second seamless loop where every harmonic completes an integer number of cycles
            val durationSeconds = 4
            val numSamples = sampleRate * durationSeconds
            val pcm = ShortArray(numSamples)

            // Warm meditative drone chord: C3 (130.75 Hz -> 131 Hz), G3 (196 Hz), D4 (293.75 Hz), E4 (329.5 Hz)
            // Quantized to exact multiples of 0.25 Hz for click-free seamless looping
            val freqs = doubleArrayOf(130.75, 196.0, 261.5, 329.5)
            val weights = doubleArrayOf(0.40, 0.28, 0.20, 0.12)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                var sample = 0.0
                for (fIdx in freqs.indices) {
                    val phase = 2.0 * PI * freqs[fIdx] * t
                    // Very gentle slow breathing LFO (0.25 Hz = 1 full breath per 4 seconds)
                    val lfo = 0.85 + 0.15 * sin(2.0 * PI * 0.25 * t + fIdx)
                    sample += sin(phase) * weights[fIdx] * lfo
                }
                val scaled = (sample * Short.MAX_VALUE * 0.22).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[i] = scaled.toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.setLoopPoints(0, numSamples, -1)
            track.setVolume(currentVolume)
            track.play()

            audioTrack = track
            isRunning = true
        } catch (e: Exception) {
            Log.e("AmbientPadEngine", "Failed to start ambient pad", e)
            stop()
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(currentVolume)
        } catch (e: Exception) {
            Log.w("AmbientPadEngine", "Failed to adjust ambient volume", e)
        }
    }

    fun stop() {
        isRunning = false
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w("AmbientPadEngine", "Error releasing AudioTrack", e)
        } finally {
            audioTrack = null
        }
    }
}
