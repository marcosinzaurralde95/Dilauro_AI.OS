package com.example.dilauro_aios.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dilauro_aios.data.AudioRecorderHelper
import com.example.dilauro_aios.data.AudioSynthesizer
import com.example.dilauro_aios.data.GeminiService
import com.example.dilauro_aios.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerativeStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiService(application)
    private val audioRecorderHelper = AudioRecorderHelper(application)
    private val audioSynthesizer = AudioSynthesizer()

    val isKeyConfigured: Boolean
        get() = geminiService.isKeyConfigured

    // ==========================================
    // 1. Image Studio State (gemini-3)
    // ==========================================
    private val _imagePrompt = MutableStateFlow("Futuristic neural kernel hologram with glowing cyan circuit pathways")
    val imagePrompt: StateFlow<String> = _imagePrompt.asStateFlow()

    private val _imageEditPrompt = MutableStateFlow("Add glowing security shield matrix and crimson threat alert rings")
    val imageEditPrompt: StateFlow<String> = _imageEditPrompt.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow(ImageAspectRatio.SQUARE)
    val selectedAspectRatio: StateFlow<ImageAspectRatio> = _selectedAspectRatio.asStateFlow()

    private val _selectedResolution = MutableStateFlow(ImageResolution.RES_1K)
    val selectedResolution: StateFlow<ImageResolution> = _selectedResolution.asStateFlow()

    private val _selectedImageForEdit = MutableStateFlow<GeneratedImageItem?>(null)
    val selectedImageForEdit: StateFlow<GeneratedImageItem?> = _selectedImageForEdit.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _imageHistory = MutableStateFlow<List<GeneratedImageItem>>(emptyList())
    val imageHistory: StateFlow<List<GeneratedImageItem>> = _imageHistory.asStateFlow()

    // ==========================================
    // 2. Audio Transcriber State (gemini-3.5-flash)
    // ==========================================
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcriptions = MutableStateFlow<List<TranscriptionResult>>(initialTranscriptions())
    val transcriptions: StateFlow<List<TranscriptionResult>> = _transcriptions.asStateFlow()

    private val _selectedTranscription = MutableStateFlow<TranscriptionResult?>(null)
    val selectedTranscription: StateFlow<TranscriptionResult?> = _selectedTranscription.asStateFlow()

    val audioSamplePresets: List<AudioSamplePreset> = listOf(
        AudioSamplePreset(
            id = "preset_1",
            title = "D-Kernel ML Telemetry Memo",
            category = "Kernel Dispatch",
            durationText = "0:14",
            sampleText = "Kernel voice memo: Priority rebalance scheduled for Core 3 and 7. DAM memory pressure approaching moderate threshold. Enable zRAM compaction immediately."
        ),
        AudioSamplePreset(
            id = "preset_2",
            title = "Neural Shield Threat Intercept",
            category = "Security Sentry",
            durationText = "0:21",
            sampleText = "Security dispatch alert: Rogue capability token detected attempting IPC access on SynapseFS root inode. Neural Shield eBPF filter triggered zero-trust quarantine."
        ),
        AudioSamplePreset(
            id = "preset_3",
            title = "SynapseFS Semantic Indexing Log",
            category = "Filesystem",
            durationText = "0:18",
            sampleText = "Filesystem audit report: 142 new vector embeddings indexed with 768 dimensions. Cosine similarity score 0.942 on user workspace tree."
        )
    )

    private var recordingTimerJob: Job? = null
    private var currentRecordedFile: File? = null

    // ==========================================
    // 3. Music Generator State (Lyria-3)
    // ==========================================
    private val _musicPrompt = MutableStateFlow(MusicGenrePreset.CYBERPUNK_SYNTHWAVE.promptSuggestion)
    val musicPrompt: StateFlow<String> = _musicPrompt.asStateFlow()

    private val _selectedGenre = MutableStateFlow(MusicGenrePreset.CYBERPUNK_SYNTHWAVE)
    val selectedGenre: StateFlow<MusicGenrePreset> = _selectedGenre.asStateFlow()

    private val _musicDurationConfig = MutableStateFlow(LyriaMusicDuration.SHORT_CLIP)
    val musicDurationConfig: StateFlow<LyriaMusicDuration> = _musicDurationConfig.asStateFlow()

    private val _musicBpm = MutableStateFlow(128)
    val musicBpm: StateFlow<Int> = _musicBpm.asStateFlow()

    private val _isGeneratingMusic = MutableStateFlow(false)
    val isGeneratingMusic: StateFlow<Boolean> = _isGeneratingMusic.asStateFlow()

    private val _musicTracks = MutableStateFlow<List<GeneratedMusicTrack>>(initialMusicTracks())
    val musicTracks: StateFlow<List<GeneratedMusicTrack>> = _musicTracks.asStateFlow()

    private val _playingTrackId = MutableStateFlow<String?>(null)
    val playingTrackId: StateFlow<String?> = _playingTrackId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    init {
        // Generate an initial starter image so the gallery is rich immediately
        generateStarterImage()
    }

    private fun generateStarterImage() {
        viewModelScope.launch {
            val starter = geminiService.generateImage(
                prompt = "Dilauro AI-OS D-Kernel quantum core with cyan telemetry rings",
                aspectRatio = ImageAspectRatio.SQUARE,
                resolution = ImageResolution.RES_1K
            )
            _imageHistory.update { listOf(starter) }
        }
    }

    // --- Image Studio Actions ---
    fun onImagePromptChanged(newPrompt: String) { _imagePrompt.value = newPrompt }
    fun onImageEditPromptChanged(newEdit: String) { _imageEditPrompt.value = newEdit }
    fun onAspectRatioSelected(ratio: ImageAspectRatio) { _selectedAspectRatio.value = ratio }
    fun onResolutionSelected(res: ImageResolution) { _selectedResolution.value = res }
    fun selectImageForEdit(image: GeneratedImageItem?) { _selectedImageForEdit.value = image }

    fun generateImage() {
        val prompt = _imagePrompt.value.trim()
        if (prompt.isEmpty() || _isGeneratingImage.value) return

        _isGeneratingImage.value = true
        viewModelScope.launch {
            try {
                val result = geminiService.generateImage(
                    prompt = prompt,
                    aspectRatio = _selectedAspectRatio.value,
                    resolution = _selectedResolution.value
                )
                _imageHistory.update { listOf(result) + it }
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }

    fun editSelectedImage() {
        val target = _selectedImageForEdit.value ?: return
        val editInstruction = _imageEditPrompt.value.trim()
        if (editInstruction.isEmpty() || _isGeneratingImage.value) return

        _isGeneratingImage.value = true
        viewModelScope.launch {
            try {
                val result = geminiService.editImage(
                    sourceImage = target,
                    editPrompt = editInstruction,
                    aspectRatio = _selectedAspectRatio.value
                )
                _imageHistory.update { listOf(result) + it }
                _selectedImageForEdit.value = result
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }

    // --- Audio Transcriber Actions ---
    fun startMicrophoneRecording() {
        if (_isRecording.value) return
        currentRecordedFile = audioRecorderHelper.startRecording()
        _isRecording.value = true
        _recordingDurationSeconds.value = 0

        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingDurationSeconds.update { it + 1 }
            }
        }
    }

    fun stopMicrophoneRecordingAndTranscribe() {
        if (!_isRecording.value) return
        recordingTimerJob?.cancel()
        _isRecording.value = false

        val recordedFile = audioRecorderHelper.stopRecording() ?: currentRecordedFile
        if (recordedFile != null && recordedFile.exists()) {
            transcribeAudioFile(recordedFile)
        }
    }

    fun transcribeSamplePreset(preset: AudioSamplePreset) {
        if (_isTranscribing.value) return
        _isTranscribing.value = true

        viewModelScope.launch {
            try {
                delay(900) // Simulated transmission latency
                val result = TranscriptionResult(
                    title = preset.title,
                    audioDurationSeconds = 18,
                    rawTranscription = preset.sampleText,
                    executiveSummary = "Decoded voice dispatch from [${preset.category}]. Systems checked and actions queued for D-Kernel execution.",
                    actionItems = listOf(
                        "Execute ${preset.category} directive in current scheduler epoch.",
                        "Archive telemetry audio transcript to SynapseFS vector inode."
                    ),
                    sentiment = "OPTIMAL",
                    urgencyLevel = if (preset.id == "preset_2") "HIGH" else "NORMAL",
                    modelUsed = "gemini-3.5-flash"
                )
                _transcriptions.update { listOf(result) + it }
                _selectedTranscription.value = result
            } finally {
                _isTranscribing.value = false
            }
        }
    }

    private fun transcribeAudioFile(file: File) {
        _isTranscribing.value = true
        viewModelScope.launch {
            try {
                val result = geminiService.transcribeAudio(file)
                _transcriptions.update { listOf(result) + it }
                _selectedTranscription.value = result
            } finally {
                _isTranscribing.value = false
            }
        }
    }

    fun selectTranscription(item: TranscriptionResult?) {
        _selectedTranscription.value = item
    }

    // --- Music Generator (Lyria-3) Actions ---
    fun onMusicPromptChanged(newPrompt: String) { _musicPrompt.value = newPrompt }
    fun onGenreSelected(genre: MusicGenrePreset) {
        _selectedGenre.value = genre
        _musicPrompt.value = genre.promptSuggestion
        _musicBpm.value = genre.bpm
    }
    fun onMusicDurationConfigSelected(duration: LyriaMusicDuration) {
        _musicDurationConfig.value = duration
    }
    fun onMusicBpmChanged(newBpm: Int) { _musicBpm.value = newBpm }

    fun generateMusic() {
        val prompt = _musicPrompt.value.trim()
        if (prompt.isEmpty() || _isGeneratingMusic.value) return

        _isGeneratingMusic.value = true
        viewModelScope.launch {
            try {
                val track = geminiService.generateMusic(
                    prompt = prompt,
                    genre = _selectedGenre.value.genreName,
                    durationConfig = _musicDurationConfig.value,
                    bpm = _musicBpm.value
                )
                _musicTracks.update { listOf(track) + it }
                playTrack(track)
            } finally {
                _isGeneratingMusic.value = false
            }
        }
    }

    fun playTrack(track: GeneratedMusicTrack) {
        if (_playingTrackId.value == track.id) {
            stopPlayback()
            return
        }

        stopPlayback()
        _playingTrackId.value = track.id
        _playbackProgress.value = 0f

        val filePath = track.audioFilePath
        if (filePath != null && File(filePath).exists()) {
            audioSynthesizer.playFile(filePath) {
                _playingTrackId.value = null
                _playbackProgress.value = 0f
            }
        } else {
            audioSynthesizer.playProceduralMusic(
                bpm = track.bpm,
                genre = track.genre,
                seed = track.waveformSeed,
                onProgress = { progress -> _playbackProgress.value = progress },
                onFinished = {
                    _playingTrackId.value = null
                    _playbackProgress.value = 0f
                }
            )
        }
    }

    fun stopPlayback() {
        audioSynthesizer.stop()
        _playingTrackId.value = null
        _playbackProgress.value = 0f
    }

    // Initial mock data
    private fun initialTranscriptions(): List<TranscriptionResult> {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return listOf(
            TranscriptionResult(
                title = "Initial D-Kernel Boot Audio Log",
                audioDurationSeconds = 14,
                rawTranscription = "System voice telemetry boot sequence initialized. Rust microkernel verified no_std compatibility. DAM memory controller loaded with 2048 MB dynamic page allocation.",
                executiveSummary = "Initial bootstrap voice confirmation. Memory controllers and microkernel capabilities online.",
                actionItems = listOf("Engage eBPF syscall sentinels", "Mount SynapseFS root namespace"),
                sentiment = "OPTIMAL",
                urgencyLevel = "NORMAL",
                modelUsed = "gemini-3.5-flash",
                timestamp = System.currentTimeMillis() - 3600000
            )
        )
    }

    private fun initialMusicTracks(): List<GeneratedMusicTrack> {
        return listOf(
            GeneratedMusicTrack(
                title = "Cyberpunk Synthwave // Neon Horizon",
                prompt = "128 BPM retro-futuristic dark electronic arpeggiators with punchy 80s analog bass",
                genre = "Cyberpunk Synthwave",
                durationConfig = LyriaMusicDuration.SHORT_CLIP,
                durationSeconds = 28,
                bpm = 128,
                modelUsed = "lyria-3-clip-preview"
            ),
            GeneratedMusicTrack(
                title = "Neural Ambient // Sub-Zero Void",
                prompt = "Deep meditative sub-bass pads with generative granular textures and spatial reverb",
                genre = "Neural Ambient Drone",
                durationConfig = LyriaMusicDuration.FULL_TRACK,
                durationSeconds = 64,
                bpm = 72,
                modelUsed = "lyria-3-pro-preview"
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioSynthesizer.stop()
        audioRecorderHelper.cancelRecording()
        videoPlaybackJob?.cancel()
    }

    // ==========================================
    // 4. Gemini Chatbot State & Actions
    // ==========================================
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(initialChatMessages())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedPersona = MutableStateFlow(ChatbotPersona.COMPLEX_ENGINEER)
    val selectedPersona: StateFlow<ChatbotPersona> = _selectedPersona.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun onChatInputChanged(text: String) { _chatInput.value = text }
    fun selectPersona(persona: ChatbotPersona) { _selectedPersona.value = persona }

    fun sendChatMessage() {
        val text = _chatInput.value.trim()
        if (text.isEmpty() || _isChatLoading.value) return
        val userMsg = ChatMessage(role = ChatRole.USER, content = text)
        _chatMessages.update { it + userMsg }
        _chatInput.value = ""
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val response = geminiService.sendChatMessage(
                    history = _chatMessages.value,
                    currentPrompt = text,
                    persona = _selectedPersona.value
                )
                _chatMessages.update { it + response }
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatMessages.value = initialChatMessages()
    }

    // ==========================================
    // 5. Veo 3 Video Generation State & Actions
    // ==========================================
    private val _videoPrompt = MutableStateFlow("Cinematic flythrough of quantum neural core with pulsing neon cyan circuit matrices")
    val videoPrompt: StateFlow<String> = _videoPrompt.asStateFlow()

    private val _selectedVideoAspectRatio = MutableStateFlow(VideoAspectRatio.LANDSCAPE_16_9)
    val selectedVideoAspectRatio: StateFlow<VideoAspectRatio> = _selectedVideoAspectRatio.asStateFlow()

    private val _selectedPhotoForAnimation = MutableStateFlow<android.graphics.Bitmap?>(null)
    val selectedPhotoForAnimation: StateFlow<android.graphics.Bitmap?> = _selectedPhotoForAnimation.asStateFlow()

    private val _isGeneratingVideo = MutableStateFlow(false)
    val isGeneratingVideo: StateFlow<Boolean> = _isGeneratingVideo.asStateFlow()

    private val _generatedVideos = MutableStateFlow<List<GeneratedVideoItem>>(initialGeneratedVideos())
    val generatedVideos: StateFlow<List<GeneratedVideoItem>> = _generatedVideos.asStateFlow()

    private val _playingVideoId = MutableStateFlow<String?>(null)
    val playingVideoId: StateFlow<String?> = _playingVideoId.asStateFlow()

    private val _currentVideoFrameIdx = MutableStateFlow(0)
    val currentVideoFrameIdx: StateFlow<Int> = _currentVideoFrameIdx.asStateFlow()

    private var videoPlaybackJob: Job? = null

    fun onVideoPromptChanged(text: String) { _videoPrompt.value = text }
    fun selectVideoAspectRatio(ratio: VideoAspectRatio) { _selectedVideoAspectRatio.value = ratio }
    fun selectPhotoForAnimation(bitmap: android.graphics.Bitmap?) { _selectedPhotoForAnimation.value = bitmap }

    fun generateVideoFromText() {
        val prompt = _videoPrompt.value.trim()
        if (prompt.isEmpty() || _isGeneratingVideo.value) return
        _isGeneratingVideo.value = true
        viewModelScope.launch {
            try {
                val video = geminiService.generateVideo(
                    prompt = prompt,
                    aspectRatio = _selectedVideoAspectRatio.value,
                    sourceBitmap = null,
                    isImageAnimated = false
                )
                _generatedVideos.update { listOf(video) + it }
                toggleVideoPlayback(video)
            } finally {
                _isGeneratingVideo.value = false
            }
        }
    }

    fun animatePhotoToVideo() {
        val prompt = _videoPrompt.value.trim().ifBlank { "Volumetric light sweep and 3D camera pan around neural object" }
        if (_isGeneratingVideo.value) return
        _isGeneratingVideo.value = true
        viewModelScope.launch {
            try {
                val video = geminiService.generateVideo(
                    prompt = prompt,
                    aspectRatio = _selectedVideoAspectRatio.value,
                    sourceBitmap = _selectedPhotoForAnimation.value,
                    isImageAnimated = true
                )
                _generatedVideos.update { listOf(video) + it }
                toggleVideoPlayback(video)
            } finally {
                _isGeneratingVideo.value = false
            }
        }
    }

    fun toggleVideoPlayback(video: GeneratedVideoItem) {
        if (_playingVideoId.value == video.id) {
            stopVideoPlayback()
        } else {
            _playingVideoId.value = video.id
            _currentVideoFrameIdx.value = 0
            videoPlaybackJob?.cancel()
            val frameCount = video.videoPreviewFrames?.size ?: 1
            if (frameCount > 1) {
                videoPlaybackJob = viewModelScope.launch {
                    while (_playingVideoId.value == video.id) {
                        delay(220)
                        _currentVideoFrameIdx.update { (it + 1) % frameCount }
                    }
                }
            }
        }
    }

    fun stopVideoPlayback() {
        _playingVideoId.value = null
        videoPlaybackJob?.cancel()
        videoPlaybackJob = null
    }

    private fun initialChatMessages(): List<ChatMessage> = listOf(
        ChatMessage(
            role = ChatRole.MODEL,
            content = "Greetings Operator. Dilauro AI-OS Neural Intelligence Copilot initialized. I am equipped to analyze microkernel telemetry, review capability manifests, and orchestrate zero-trust system workflows. How can I assist your operations today?",
            modelUsed = "gemini-3.1-pro-preview",
            timestamp = System.currentTimeMillis() - 120000L
        )
    )

    private fun initialGeneratedVideos(): List<GeneratedVideoItem> = listOf(
        GeneratedVideoItem(
            id = "vid_quantum_core",
            prompt = "Hyper-speed cinematic camera orbit around D-Kernel quantum core with glowing cyan laser traces",
            aspectRatio = VideoAspectRatio.LANDSCAPE_16_9,
            modelUsed = "veo-3.1-fast-generate-preview",
            durationSeconds = 6,
            resolution = "1080p",
            isImageAnimated = false
        )
    )
}
