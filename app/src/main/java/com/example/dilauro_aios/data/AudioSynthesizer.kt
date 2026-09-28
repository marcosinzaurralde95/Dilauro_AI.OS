package com.example.dilauro_aios.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

/**
 * Handles real audio playback and procedural audio synthesizer engine.
 * Supports playing actual audio files as well as generating real-time musical synthesizer audio
 * using Android's AudioTrack for rich auditory feedback.
 */
class AudioSynthesizer {

    private var mediaPlayer: MediaPlayer? = null
    private var synthJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var isPlaying: Boolean = false
        private set

    /**
     * Play an audio file (e.g., recorded voice memo or downloaded track).
     */
    fun playFile(filePath: String, onCompletion: () -> Unit = {}) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    this@AudioSynthesizer.isPlaying = false
                    onCompletion()
                }
            }
            isPlaying = true
        } catch (e: Exception) {
            isPlaying = false
        }
    }

    /**
     * Synthesizes and plays a procedural music track in real time using AudioTrack.
     * Generates chord progressions, basslines, and synth melodies tailored to BPM and genre.
     */
    fun playProceduralMusic(
        bpm: Int,
        genre: String,
        seed: Long,
        onProgress: (Float) -> Unit,
        onFinished: () -> Unit
    ) {
        stop()
        isPlaying = true

        synthJob = scope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            val secondsPerBeat = 60.0 / bpm
            val totalSeconds = 24.0 // 24 seconds preview loop
            val totalSamples = (totalSeconds * sampleRate).toInt()
            val shortBuffer = ShortArray(1024)

            // Scale notes based on genre
            val scale = when {
                genre.contains("Cyberpunk", ignoreCase = true) -> intArrayOf(110, 130, 146, 164, 196, 220, 261, 293)
                genre.contains("Techno", ignoreCase = true) -> intArrayOf(98, 116, 131, 147, 165, 196)
                genre.contains("Ambient", ignoreCase = true) -> intArrayOf(130, 164, 196, 246, 261, 329)
                genre.contains("Lo-Fi", ignoreCase = true) -> intArrayOf(146, 174, 220, 261, 329, 392)
                else -> intArrayOf(110, 146, 164, 220, 293, 329)
            }

            var currentSample = 0
            while (currentSample < totalSamples && isPlaying) {
                for (i in shortBuffer.indices) {
                    val globalIdx = currentSample + i
                    val t = globalIdx.toDouble() / sampleRate
                    val beatIndex = (t / secondsPerBeat).toInt()
                    val subBeat = (t / (secondsPerBeat / 4.0)).toInt()

                    // Bass note
                    val bassFreq = scale[beatIndex % scale.size].toDouble()
                    val bassWave = sin(2.0 * Math.PI * bassFreq * t)

                    // Arpeggio melody note
                    val arpIndex = (subBeat * 2) % scale.size
                    val leadFreq = (scale[arpIndex] * 2).toDouble()
                    val leadWave = sin(2.0 * Math.PI * leadFreq * t) * 0.4

                    // Kick pulse on each beat
                    val timeInBeat = (t % secondsPerBeat)
                    val kickEnvelope = (1.0 - (timeInBeat / (secondsPerBeat * 0.3)).coerceIn(0.0, 1.0))
                    val kickWave = sin(2.0 * Math.PI * (60.0 * (1.0 + kickEnvelope * 2)) * timeInBeat) * kickEnvelope * 0.6

                    // Sub-pad texture
                    val padWave = sin(2.0 * Math.PI * (bassFreq * 1.5) * t) * 0.25

                    val mixed = (bassWave * 0.35 + leadWave + kickWave + padWave).coerceIn(-1.0, 1.0)
                    shortBuffer[i] = (mixed * 24000).toInt().toShort()
                }

                audioTrack?.write(shortBuffer, 0, shortBuffer.size)
                currentSample += shortBuffer.size
                withContext(Dispatchers.Main) {
                    onProgress(currentSample.toFloat() / totalSamples)
                }
                delay(15)
            }

            withContext(Dispatchers.Main) {
                stop()
                onFinished()
            }
        }
    }

    /**
     * Stop all current playback.
     */
    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    /**
     * Write raw synthesized music bytes to a cache WAV file for SynapseFS persistence.
     */
    fun exportToWavFile(destinationFile: File, bpm: Int, genre: String) {
        val sampleRate = 22050
        val durationSeconds = 15
        val totalSamples = sampleRate * durationSeconds
        val scale = intArrayOf(110, 130, 146, 164, 196, 220)

        FileOutputStream(destinationFile).use { fos ->
            // WAV Header
            val totalDataLen = totalSamples * 2 + 36
            val byteRate = sampleRate * 2
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // Subchunk1Size (16 for PCM)
            header[20] = 1; header[21] = 0 // AudioFormat 1 = PCM
            header[22] = 1; header[23] = 0 // NumChannels = 1 (Mono)
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2; header[33] = 0 // BlockAlign
            header[34] = 16; header[35] = 0 // BitsPerSample
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = ((totalSamples * 2) and 0xff).toByte()
            header[41] = (((totalSamples * 2) shr 8) and 0xff).toByte()
            header[42] = (((totalSamples * 2) shr 16) and 0xff).toByte()
            header[43] = (((totalSamples * 2) shr 24) and 0xff).toByte()
            fos.write(header)

            val buffer = ByteArray(2)
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val freq = scale[((t * 2).toInt()) % scale.size].toDouble()
                val wave = sin(2.0 * Math.PI * freq * t) * 0.6
                val sVal = (wave * 28000).toInt().toShort()
                buffer[0] = (sVal.toInt() and 0xFF).toByte()
                buffer[1] = ((sVal.toInt() shr 8) and 0xFF).toByte()
                fos.write(buffer)
            }
        }
    }
}
