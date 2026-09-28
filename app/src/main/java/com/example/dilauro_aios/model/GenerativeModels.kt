package com.example.dilauro_aios.model

import android.graphics.Bitmap
import java.util.UUID

// ==========================================
// 1. Create & Edit Images Models (Gemini-3)
// ==========================================

enum class ImageAspectRatio(val label: String, val apiValue: String, val widthRatio: Float, val heightRatio: Float) {
    SQUARE("1:1 Square", "1:1", 1f, 1f),
    LANDSCAPE("16:9 Cinema HUD", "16:9", 16f, 9f),
    PORTRAIT("9:16 Cyber Mobile", "9:16", 9f, 16f),
    STANDARD("4:3 Terminal View", "4:3", 4f, 3f)
}

enum class ImageResolution(val label: String, val apiValue: String) {
    RES_512("512px Fast Preview", "512px"),
    RES_1K("1K High Definition", "1K"),
    RES_2K("2K Ultra HD", "2K")
}

data class GeneratedImageItem(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val editInstruction: String? = null,
    val modelUsed: String = "gemini-3.1-flash-image-preview",
    val aspectRatio: ImageAspectRatio = ImageAspectRatio.SQUARE,
    val resolution: ImageResolution = ImageResolution.RES_1K,
    val timestamp: Long = System.currentTimeMillis(),
    val bitmap: Bitmap? = null,
    val base64Data: String? = null,
    val isEdited: Boolean = false,
    val parentImageId: String? = null,
    val synapseFsInodePath: String? = null
)

// ==========================================
// 2. Transcribe Audio Models (Gemini)
// ==========================================

data class TranscriptionResult(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val audioDurationSeconds: Int,
    val rawTranscription: String,
    val executiveSummary: String,
    val actionItems: List<String>,
    val sentiment: String, // e.g. "OPTIMAL", "CRITICAL ALERT", "INFORMATIONAL"
    val urgencyLevel: String, // "NORMAL", "HIGH", "KERNEL CRITICAL"
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = "gemini-3.5-flash",
    val synapseFsInodePath: String? = null
)

data class AudioSamplePreset(
    val id: String,
    val title: String,
    val category: String,
    val durationText: String,
    val sampleText: String
)

// ==========================================
// 3. Generate Music Models (Lyria-3)
// ==========================================

enum class LyriaMusicDuration(val label: String, val model: String, val maxSeconds: Int) {
    SHORT_CLIP("Short Clip (up to 30s)", "lyria-3-clip-preview", 30),
    FULL_TRACK("Full-Length Track", "lyria-3-pro-preview", 120)
}

enum class MusicGenrePreset(val genreName: String, val bpm: Int, val promptSuggestion: String) {
    CYBERPUNK_SYNTHWAVE("Cyberpunk Synthwave", 128, "Retro-futuristic synthwave with punchy analog bass, glowing neon arpeggios, and driving 80s drum machines."),
    NEURAL_AMBIENT("Neural Ambient Drone", 72, "Deep meditative ambient soundscape with warm analog sub-bass pads, generative granular textures, and spatial reverb."),
    DKERNEL_TECHNO("D-Kernel Industrial Techno", 138, "High-velocity industrial techno with relentless kick drums, modular acid synth lines, and metallic percussion."),
    LOFI_TERMINAL("Lo-Fi Terminal Chill", 84, "Relaxed lo-fi hip hop beat with dusty vinyl crackle, mellow Rhodes electric piano chords, and soothing bass."),
    SCIFI_SYMPHONY("Sci-Fi Orchestral Odyssey", 110, "Epic futuristic cinematic orchestral track with dramatic brass swells, hybrid percussion, and ethereal digital choir.")
}

data class GeneratedMusicTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val prompt: String,
    val genre: String,
    val durationConfig: LyriaMusicDuration,
    val durationSeconds: Int,
    val bpm: Int,
    val modelUsed: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String? = null,
    val synapseFsInodePath: String? = null,
    val waveformSeed: Long = Random().nextLong()
) {
    companion object {
        private fun Random() = java.util.Random()
    }
}

// ==========================================
// 4. Gemini Chatbot Models
// ==========================================

enum class ChatRole {
    USER,
    MODEL,
    SYSTEM
}

enum class ChatbotPersona(
    val title: String,
    val model: String,
    val roleBadge: String,
    val systemInstruction: String,
    val description: String
) {
    COMPLEX_ENGINEER(
        title = "D-Kernel Systems Architect",
        model = "gemini-3.1-pro-preview",
        roleBadge = "COMPLEX REASONING",
        systemInstruction = "You are the Lead Systems Architect of Dilauro AI-OS. You specialize in low-level microkernel design (no_std Rust), ML scheduling, capability-based security, and zero-ambient authority. Provide deep, technical, reasoned answers with architectural precision.",
        description = "Advanced reasoning & deep technical problem solving via gemini-3.1-pro-preview."
    ),
    GENERAL_OPERATOR(
        title = "AI-OS Command Copilot",
        model = "gemini-3.5-flash",
        roleBadge = "GENERAL TASKS",
        systemInstruction = "You are the Dilauro AI-OS Command Copilot. You assist users with general operating system queries, SynapseFS filesystem navigation, DAM memory optimization, and day-to-day productivity.",
        description = "Balanced intelligence for general workflows via gemini-3.5-flash."
    ),
    FAST_TELEMETRY(
        title = "Fast Telemetry Sentinel",
        model = "gemini-3.1-flash-lite-preview",
        roleBadge = "FAST EXECUTION",
        systemInstruction = "You are the Ultra-Fast Real-Time Telemetry Sentinel. Provide ultra-concise, rapid diagnosis of system processes, latency anomalies, and quick status summaries. Respond in under 3 concise sentences.",
        description = "Ultra-low latency responses for high-velocity queries via gemini-3.1-flash-lite."
    )
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val tokensUsed: Int? = null
)

// ==========================================
// 5. Veo 3 Video Generation Models
// ==========================================

enum class VideoAspectRatio(val label: String, val apiValue: String, val widthRatio: Float, val heightRatio: Float) {
    LANDSCAPE_16_9("16:9 Landscape (Cinema HUD)", "16:9", 16f, 9f),
    PORTRAIT_9_16("9:16 Portrait (Mobile Terminal)", "9:16", 9f, 16f)
}

data class GeneratedVideoItem(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.LANDSCAPE_16_9,
    val modelUsed: String = "veo-3.1-fast-generate-preview",
    val sourceImage: Bitmap? = null,
    val isImageAnimated: Boolean = false,
    val durationSeconds: Int = 6,
    val resolution: String = "1080p",
    val timestamp: Long = System.currentTimeMillis(),
    val videoPreviewFrames: List<Bitmap>? = null,
    val synapseFsInodePath: String? = null
)
