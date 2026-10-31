package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager {

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Short, crisp two-note ascending tone (C5 -> G5) matching the length and volume of playWrong()
     */
    fun playCorrect() {
        scope.launch {
            try {
                val sampleRate = 44100
                val totalDurationMs = 240
                val numSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
                val mixBuffer = DoubleArray(numSamples)

                val notes = listOf(
                    NoteEvent(0, 120, 523.25, 0.52),    // C5
                    NoteEvent(80, 160, 783.99, 0.58)    // G5
                )

                for (note in notes) {
                    addWarmWoodNote(mixBuffer, sampleRate, note)
                }

                val pcmBuffer = toShortPcm(mixBuffer)
                playBuffer(pcmBuffer, sampleRate, totalDurationMs + 30L)
            } catch (_: Exception) {}
        }
    }

    /**
     * Gentle, warm wooden marimba two-note descending tone (G4 -> Eb4) instead of a harsh buzz
     */
    fun playWrong() {
        scope.launch {
            try {
                val sampleRate = 44100
                val totalDurationMs = 360
                val numSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
                val mixBuffer = DoubleArray(numSamples)

                val notes = listOf(
                    NoteEvent(0, 170, 392.00, 0.50),    // G4
                    NoteEvent(115, 245, 311.13, 0.55)   // Eb4
                )

                for (note in notes) {
                    addWarmWoodNote(mixBuffer, sampleRate, note)
                }

                val pcmBuffer = toShortPcm(mixBuffer)
                playBuffer(pcmBuffer, sampleRate, totalDurationMs + 40L)
            } catch (_: Exception) {}
        }
    }

    /**
     * Subtle, organic soft tap sound with smooth attack (no digital click/pop)
     */
    fun playClick() {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 28
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val mixBuffer = DoubleArray(numSamples)

                addWarmWoodNote(
                    mixBuffer,
                    sampleRate,
                    NoteEvent(0, durationMs, 680.0, 0.35)
                )

                val pcmBuffer = toShortPcm(mixBuffer)
                playBuffer(pcmBuffer, sampleRate, 60L)
            } catch (_: Exception) {}
        }
    }

    private data class NoteEvent(
        val startMs: Int,
        val durationMs: Int,
        val freqHz: Double,
        val amplitude: Double
    )

    private fun addBellNote(
        buffer: DoubleArray,
        sampleRate: Int,
        note: NoteEvent
    ) {
        val startSample = (sampleRate * (note.startMs / 1000.0)).toInt()
        val noteSamples = (sampleRate * (note.durationMs / 1000.0)).toInt()
        val attackSamples = (sampleRate * 0.006).toInt().coerceAtLeast(1) // 6ms soft attack

        for (i in 0 until noteSamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break

            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / noteSamples

            val attackEnv = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val decayEnv = exp(-4.8 * progress)
            val env = attackEnv * decayEnv

            // Warm acoustic bell timbre: fundamental + soft 2nd and 3rd harmonics that decay faster
            val f = note.freqHz
            val fundamental = sin(2.0 * PI * f * t)
            val harmonic2 = 0.22 * sin(2.0 * PI * (f * 2.0) * t) * exp(-7.0 * progress)
            val harmonic3 = 0.08 * sin(2.0 * PI * (f * 3.0) * t) * exp(-10.0 * progress)

            buffer[idx] += (fundamental + harmonic2 + harmonic3) * env * note.amplitude
        }
    }

    private fun addWarmWoodNote(
        buffer: DoubleArray,
        sampleRate: Int,
        note: NoteEvent
    ) {
        val startSample = (sampleRate * (note.startMs / 1000.0)).toInt()
        val noteSamples = (sampleRate * (note.durationMs / 1000.0)).toInt()
        val attackSamples = (sampleRate * 0.005).toInt().coerceAtLeast(1) // 5ms soft attack

        for (i in 0 until noteSamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break

            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / noteSamples

            val attackEnv = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val decayEnv = exp(-5.5 * progress)
            val env = attackEnv * decayEnv

            // Marimba-like wood timbre: fundamental + brief 4th harmonic overtone
            val f = note.freqHz
            val fundamental = sin(2.0 * PI * f * t)
            val woodOvertone = 0.15 * sin(2.0 * PI * (f * 2.0) * t) * exp(-9.0 * progress)

            buffer[idx] += (fundamental + woodOvertone) * env * note.amplitude
        }
    }

    private fun toShortPcm(mixBuffer: DoubleArray): ShortArray {
        val out = ShortArray(mixBuffer.size)
        for (i in mixBuffer.indices) {
            // Soft saturation to prevent harsh clipping when chord notes overlap
            val clamped = mixBuffer[i].coerceIn(-0.98, 0.98)
            out[i] = (clamped * 28000.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun playBuffer(buffer: ShortArray, sampleRate: Int, sleepMs: Long) {
        var track: AudioTrack? = null
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, buffer.size * 2)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(sleepMs)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
        }
    }
}
