package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine {
    @Volatile
    var enabled: Boolean = true

    private val executor = Executors.newSingleThreadExecutor()

    private val shootBuffer: ShortArray by lazy { synthesizeSweep(280.0, 560.0, 70, 0.25) }
    private val bounceBuffer: ShortArray by lazy { synthesizeSweep(720.0, 860.0, 32, 0.18) }
    private val explosionBuffer: ShortArray by lazy { synthesizeExplosion(190) }
    private val victoryBuffer: ShortArray by lazy {
        synthesizeSequence(doubleArrayOf(523.25, 659.25, 783.99, 1046.50), 80, 0.30)
    }
    private val gameOverBuffer: ShortArray by lazy {
        synthesizeSequence(doubleArrayOf(349.23, 293.66, 220.0), 110, 0.25)
    }
    private val popScaleBuffers: List<ShortArray> by lazy {
        val freqs = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1174.66)
        freqs.map { f -> synthesizeSweep(f * 0.92, f * 1.15, 85, 0.28) }
    }

    fun playShoot() = playPcm(shootBuffer)

    fun playBounce() = playPcm(bounceBuffer)

    fun playPop(combo: Int) {
        val index = (combo - 1).coerceIn(0, popScaleBuffers.lastIndex)
        playPcm(popScaleBuffers[index])
    }

    fun playExplosion() = playPcm(explosionBuffer)

    fun playVictory() = playPcm(victoryBuffer)

    fun playGameOver() = playPcm(gameOverBuffer)

    fun release() {
        executor.shutdownNow()
    }

    private fun playPcm(samples: ShortArray) {
        if (!enabled) return
        executor.execute {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                val durationMs = (samples.size * 1000L) / SAMPLE_RATE + 30L
                Thread.sleep(durationMs)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore audio device errors on unsupported environments
            }
        }
    }

    private fun synthesizeSweep(
        startHz: Double,
        endHz: Double,
        durationMs: Int,
        volume: Double
    ): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        val data = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val env = sin(PI * t) * exp(-2.2 * t)
            data[i] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return data
    }

    private fun synthesizeSequence(
        notesHz: DoubleArray,
        noteDurationMs: Int,
        volume: Double
    ): ShortArray {
        val perNote = (SAMPLE_RATE * noteDurationMs) / 1000
        val data = ShortArray(perNote * notesHz.size)
        for ((idx, freq) in notesHz.withIndex()) {
            val offset = idx * perNote
            for (i in 0 until perNote) {
                val t = i.toDouble() / perNote
                val env = (1.0 - t) * sin(PI * minOf(t * 6.0, 1.0) / 2.0)
                val sample = sin(2.0 * PI * freq * i / SAMPLE_RATE) * env * volume
                data[offset + i] = (sample * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return data
    }

    private fun synthesizeExplosion(durationMs: Int): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        val data = ShortArray(count)
        val rng = Random(7)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val bassFreq = 150.0 - 105.0 * t
            phase += 2.0 * PI * bassFreq / SAMPLE_RATE
            val noise = (rng.nextDouble() * 2.0 - 1.0) * 0.45
            val wave = sin(phase) * 0.65 + noise
            val env = exp(-4.0 * t)
            data[i] = (wave * env * 0.40 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return data
    }

    companion object {
        private const val SAMPLE_RATE = 22050
    }
}
