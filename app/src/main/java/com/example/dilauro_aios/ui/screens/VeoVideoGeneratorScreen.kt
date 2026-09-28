package com.example.dilauro_aios.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.GeneratedVideoItem
import com.example.dilauro_aios.model.VideoAspectRatio
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.GenerativeStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeoVideoGeneratorScreen(
    viewModel: GenerativeStudioViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Text to Video, 1: Animate Image
    val videoPrompt by viewModel.videoPrompt.collectAsState()
    val selectedRatio by viewModel.selectedVideoAspectRatio.collectAsState()
    val selectedPhoto by viewModel.selectedPhotoForAnimation.collectAsState()
    val isGenerating by viewModel.isGeneratingVideo.collectAsState()
    val videos by viewModel.generatedVideos.collectAsState()
    val playingVideoId by viewModel.playingVideoId.collectAsState()
    val currentFrameIdx by viewModel.currentVideoFrameIdx.collectAsState()

    var toastNotice by remember { mutableStateOf<String?>(null) }

    // Google Play Policy compliant zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    viewModel.selectPhotoForAnimation(bitmap)
                    toastNotice = "Photo loaded for Veo animation!"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val videoPromptSuggestions = listOf(
        "Cinematic hyper-drive flight through neon cybernetic kernel pipeline",
        "Fluid 3D camera pan around floating holographic quantum processor",
        "Slow-motion volumetric light beams scanning Neural Shield honeycombs"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Veo 3 Video Generator", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            MetricBadge(
                                text = "VEO-3.1",
                                textColor = NeonCyan,
                                backgroundColor = DeepBackground
                            )
                        }
                        Text(
                            "Cinematic Video Synthesis & Photo Animation",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("veo_video_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBackground)
            )
        },
        containerColor = DeepBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Tabs: Text to Video vs Animate Image
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DeepBackground,
                    contentColor = NeonCyan,
                    modifier = Modifier.testTag("veo_generation_tabs")
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.MovieFilter, contentDescription = null) },
                        text = { Text("Generate from Text", fontSize = 11.sp) },
                        selectedContentColor = NeonCyan,
                        unselectedContentColor = TextSecondary,
                        modifier = Modifier.testTag("tab_text_to_video")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Animation, contentDescription = null) },
                        text = { Text("Animate Image", fontSize = 11.sp) },
                        selectedContentColor = NeonPurple,
                        unselectedContentColor = TextSecondary,
                        modifier = Modifier.testTag("tab_animate_image")
                    )
                }
            }

            // Toast Notice
            item {
                AnimatedVisibility(visible = toastNotice != null) {
                    toastNotice?.let { notice ->
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = EmeraldSecurity
                        ) {
                            Text(notice, color = EmeraldSecurity, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Generator Deck
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (selectedTab == 0) BorderCyan else NeonPurple
                ) {
                    Text(
                        text = if (selectedTab == 0) "TEXT-TO-VIDEO GENERATOR // VEO 3" else "PHOTO ANIMATION STUDIO // VEO 3",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedTab == 0) NeonCyan else NeonPurple,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // If Animate Image mode, show photo picker and preview
                    if (selectedTab == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedPhoto != null) {
                                    Image(
                                        bitmap = selectedPhoto!!.asImageBitmap(),
                                        contentDescription = "Selected Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonPurple)
                                        Text("Pick Photo", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedPhoto != null) "Photo Ready for Veo Animation" else "Tap box to upload a photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedPhoto != null) EmeraldSecurity else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Veo 3 will synthesize realistic camera pans, lighting sweeps, and 3D fluid motion.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Aspect Ratio Selector: 16:9 or 9:16 (Required by spec!)
                    Text("Aspect Ratio (Veo 3):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoAspectRatio.entries.forEach { ratio ->
                            val isSelected = selectedRatio == ratio
                            OutlinedButton(
                                onClick = { viewModel.selectVideoAspectRatio(ratio) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DeepBackground
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else BorderSubtle
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = ratio.label,
                                    fontSize = 11.sp,
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Video Prompt Input
                    OutlinedTextField(
                        value = videoPrompt,
                        onValueChange = { viewModel.onVideoPromptChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("veo_video_prompt_input"),
                        label = { Text("Video Motion Prompt", color = NeonCyan) },
                        placeholder = { Text("Describe cinematic scenes, camera movements, lighting...") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Suggested Prompts
                    if (selectedTab == 0) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(videoPromptSuggestions) { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DeepBackground,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.clickable { viewModel.onVideoPromptChanged(suggestion) }
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextCyan,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                viewModel.generateVideoFromText()
                            } else {
                                viewModel.animatePhotoToVideo()
                            }
                        },
                        enabled = !isGenerating && videoPrompt.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_veo_video_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) NeonCyan else NeonPurple
                        )
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepBackground, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synthesizing Video via Veo 3...", color = DeepBackground)
                        } else {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = DeepBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedTab == 0) "Generate Veo Video (${selectedRatio.apiValue})" else "Animate Image with Veo 3",
                                color = DeepBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Video Library
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Synthesized Veo Videos (${videos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }

            items(videos, key = { it.id }) { video ->
                val isPlaying = playingVideoId == video.id
                VeoVideoCard(
                    video = video,
                    isPlaying = isPlaying,
                    currentFrameIdx = if (isPlaying) currentFrameIdx else 0,
                    onPlayToggle = { viewModel.toggleVideoPlayback(video) },
                    onSaveToSynapseFs = {
                        toastNotice = "Archived video to SynapseFS: /vfs/videos/${video.id.take(8)}.mp4"
                    }
                )
            }
        }
    }
}

@Composable
fun VeoVideoCard(
    video: GeneratedVideoItem,
    isPlaying: Boolean,
    currentFrameIdx: Int,
    onPlayToggle: () -> Unit,
    onSaveToSynapseFs: () -> Unit
) {
    val frames = video.videoPreviewFrames
    val currentBitmap = if (!frames.isNullOrEmpty()) {
        frames[currentFrameIdx % frames.size]
    } else null

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isPlaying) NeonCyan else BorderSubtle
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricBadge(
                        text = video.modelUsed,
                        textColor = NeonCyan,
                        backgroundColor = DeepBackground
                    )
                    MetricBadge(
                        text = video.aspectRatio.label.take(4),
                        textColor = NeonPurple,
                        backgroundColor = DeepBackground
                    )
                    if (video.isImageAnimated) {
                        MetricBadge(text = "ANIMATED", textColor = EmeraldSecurity, backgroundColor = DeepBackground)
                    }
                }
                Text("${video.durationSeconds}s · ${video.resolution}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Video Player Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(video.aspectRatio.widthRatio / video.aspectRatio.heightRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(1.dp, if (isPlaying) NeonCyan else BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onPlayToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Veo Video Player",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Play / Pause Overlay Icon
                Surface(
                    shape = CircleShape,
                    color = DeepBackground.copy(alpha = if (isPlaying) 0.35f else 0.75f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Progress Bar at bottom of video
                if (isPlaying && !frames.isNullOrEmpty()) {
                    val progress = (currentFrameIdx + 1).toFloat() / frames.size.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = NeonCyan,
                        trackColor = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = video.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlayToggle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) ThreatCrimson else NeonCyan
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DeepBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPlaying) "Pause Loop" else "Play Video Loop", color = DeepBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSaveToSynapseFs,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSecurity)
                ) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to SynapseFS", color = EmeraldSecurity, fontSize = 12.sp)
                }
            }
        }
    }
}
