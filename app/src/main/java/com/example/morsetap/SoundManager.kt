package com.example.morsetap

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

class SoundManager {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    private var audioThread: Thread? = null

    fun startTone(frequency: Double = 700.0) {
        if (isPlaying) return
        isPlaying = true
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufferSize.coerceAtLeast(2048)

            @Suppress("DEPRECATION")
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM
            )
            audioTrack?.play()

            audioThread = Thread {
                val samples = ShortArray(bufferSize)
                var angle = 0.0
                val angleIncrement = (2.0 * Math.PI * frequency) / sampleRate
                while (isPlaying) {
                    for (i in samples.indices) {
                        samples[i] = (sin(angle) * Short.MAX_VALUE * 0.4).toInt().toShort()
                        angle += angleIncrement
                        if (angle > 2.0 * Math.PI) angle -= 2.0 * Math.PI
                    }
                    audioTrack?.write(samples, 0, samples.size)
                }
            }.apply { name = "MorseSoundThread"; priority = Thread.MAX_PRIORITY; start() }
        } catch (e: Exception) {
            e.printStackTrace(); isPlaying = false
        }
    }

    fun stopTone() {
        isPlaying = false
        try { audioThread?.join(100) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
        audioThread = null
        try { audioTrack?.flush(); audioTrack?.stop(); audioTrack?.release(); audioTrack = null }
        catch (e: Exception) { e.printStackTrace() }
    }
}
