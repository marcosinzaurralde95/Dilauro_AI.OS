package com.example.dilauro_aios.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File
import java.io.FileInputStream

/**
 * Manages audio recording from the device microphone for speech transcription.
 */
class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    var isRecording: Boolean = false
        private set

    fun startRecording(): File? {
        try {
            val outputDir = context.cacheDir
            currentOutputFile = File.createTempFile("voice_intel_", ".m4a", outputDir)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(22050)
                setOutputFile(currentOutputFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            return currentOutputFile
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            currentOutputFile = null
            return null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return currentOutputFile
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        return currentOutputFile
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
            mediaRecorder?.release()
            currentOutputFile?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
            currentOutputFile = null
        }
    }

    companion object {
        fun fileToBase64(file: File): String {
            val bytes = ByteArray(file.length().toInt())
            FileInputStream(file).use { it.read(bytes) }
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }
}
