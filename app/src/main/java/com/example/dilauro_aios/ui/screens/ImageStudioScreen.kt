package com.example.dilauro_aios.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.GeneratedImageItem
import com.example.dilauro_aios.model.ImageAspectRatio
import com.example.dilauro_aios.model.ImageResolution
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.GenerativeStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioScreen(
    viewModel: GenerativeStudioViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prompt by viewModel.imagePrompt.collectAsState()
    val editPrompt by viewModel.imageEditPrompt.collectAsState()
    val selectedRatio by viewModel.selectedAspectRatio.collectAsState()
    val selectedRes by viewModel.selectedResolution.collectAsState()
    val selectedImageForEdit by viewModel.selectedImageForEdit.collectAsState()
    val isGenerating by viewModel.isGeneratingImage.collectAsState()
    val history by viewModel.imageHistory.collectAsState()
    val isKeySet = viewModel.isKeyConfigured

    var exportNotice by remember { mutableStateOf<String?>(null) }

    val promptSuggestions = listOf(
        "D-Kernel quantum processor core with neon cyan laser traces",
        "Neural Shield holographic honeycomb barrier deflecting anomalies",
        "SynapseFS multidimensional vector data crystal in deep cyber void",
        "WASI micro-container capsule matrix suspended in plasma liquid"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Neural Vision Studio", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            MetricBadge(
                                text = "GEMINI-3",
                                textColor = NeonCyan,
                                backgroundColor = DeepBackground
                            )
                        }
                        Text(
                            "Create & Edit Images via Gemini 3 Multimodal Vision",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("image_studio_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Banner / API Status Notice
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isKeySet) BorderCyan.copy(alpha = 0.5f) else BorderSubtle
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isKeySet) EmeraldSecurity else WarningAmber, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKeySet) "GEMINI-3 VISION ENGINE ONLINE" else "GEMINI API KEY READY (HYBRID MODE)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isKeySet) EmeraldSecurity else WarningAmber
                            )
                            Text(
                                text = if (isKeySet)
                                    "Connected to gemini-3.1-flash-image-preview. Generates & edits high-definition neural visuals."
                                else
                                    "Configure GEMINI_API_KEY in AI Studio Secrets to unlock cloud generation. Procedural synthesis active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Notice Toast
            item {
                AnimatedVisibility(visible = exportNotice != null) {
                    exportNotice?.let { notice ->
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = EmeraldSecurity
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSecurity)
                                Text(notice, color = EmeraldSecurity, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Image-To-Image Edit Target (If an image is selected for editing)
            if (selectedImageForEdit != null) {
                item {
                    val currentEdit = selectedImageForEdit!!
                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = NeonPurple
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = NeonPurple)
                                Text(
                                    "IMAGE-TO-IMAGE EDIT MODE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { viewModel.selectImageForEdit(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Edit Mode", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            currentEdit.bitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Source Image",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, NeonPurple, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Base Prompt: ${currentEdit.prompt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2
                                )
                                Text(
                                    text = "Ratio: ${currentEdit.aspectRatio.label} · ${currentEdit.resolution.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editPrompt,
                            onValueChange = { viewModel.onImageEditPromptChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_edit_prompt_input"),
                            label = { Text("Edit & Refine Instruction", color = NeonCyan) },
                            placeholder = { Text("e.g. Add glowing crimson shields, increase matrix rain density...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.editSelectedImage() },
                            enabled = !isGenerating && editPrompt.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_image_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepBackground, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refining Visual with Gemini 3...", color = DeepBackground)
                            } else {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = DeepBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply Gemini 3 Image Edit", color = DeepBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Text-To-Image Generation Form
                item {
                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderCyan
                    ) {
                        Text(
                            text = "CREATE NEW NEURAL IMAGE",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { viewModel.onImagePromptChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_prompt_input"),
                            label = { Text("Image Generation Prompt", color = NeonCyan) },
                            placeholder = { Text("Describe the neural visual you want to create...") },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick prompt inspiration chips
                        Text("Suggested System Prompts:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(promptSuggestions) { item ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = DeepBackground,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.clickable { viewModel.onImagePromptChanged(item) }
                                ) {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Aspect Ratio Selector
                        Text("Aspect Ratio:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImageAspectRatio.entries.forEach { ratio ->
                                val isSelected = selectedRatio == ratio
                                OutlinedButton(
                                    onClick = { viewModel.onAspectRatioSelected(ratio) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DeepBackground
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonCyan else BorderSubtle
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = ratio.apiValue,
                                        fontSize = 11.sp,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Resolution Selector
                        Text("Resolution Tier:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImageResolution.entries.forEach { res ->
                                val isSelected = selectedRes == res
                                OutlinedButton(
                                    onClick = { viewModel.onResolutionSelected(res) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DeepBackground
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NeonCyan else BorderSubtle
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = res.apiValue,
                                        fontSize = 11.sp,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.generateImage() },
                            enabled = !isGenerating && prompt.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_image_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepBackground, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synthesizing Visual via Gemini 3...", color = DeepBackground)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Image with Gemini 3", color = DeepBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Gallery / Visuals History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Neural Visual Gallery (${history.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap image to Edit or Export",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            items(history, key = { it.id }) { item ->
                ImageGalleryCard(
                    item = item,
                    onEditClick = { viewModel.selectImageForEdit(item) },
                    onExportToSynapse = {
                        exportNotice = "Saved visual to SynapseFS: /vfs/neural_assets/img_${item.id.take(6)}.png"
                    }
                )
            }
        }
    }
}

@Composable
fun ImageGalleryCard(
    item: GeneratedImageItem,
    onEditClick: () -> Unit,
    onExportToSynapse: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (item.isEdited) NeonPurple.copy(alpha = 0.5f) else BorderCyan.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricBadge(
                    text = item.modelUsed,
                    textColor = if (item.isEdited) NeonPurple else NeonCyan,
                    backgroundColor = DeepBackground
                )
                Text(
                    text = sdf.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Image
            item.bitmap?.let { bmp ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(item.aspectRatio.widthRatio / item.aspectRatio.heightRatio)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = item.prompt,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Badge if edited
                    if (item.isEdited) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                            color = DeepBackground.copy(alpha = 0.85f),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = "REFINED EDIT",
                                color = NeonPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )

            if (item.editInstruction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Edit Applied: ${item.editInstruction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonPurple
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row: Edit Image, Export to SynapseFS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit / Refine", color = NeonPurple, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onExportToSynapse,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSecurity)
                ) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = EmeraldSecurity, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to SynapseFS", color = EmeraldSecurity, fontSize = 12.sp)
                }
            }
        }
    }
}
