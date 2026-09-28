package com.example.dilauro_aios.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.*
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Dedicated SynapseFS Component that visualizes semantic file indexing
 * and retrieval patterns using a high-density, interactive Treemap visualization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapseFsTreeMapDashboard(
    viewModel: AiosViewModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onViewFullFs: (() -> Unit)? = null
) {
    val files by viewModel.synapseFiles.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val telemetry by viewModel.synapseFsTelemetry.collectAsState()

    var sizingMode by remember { mutableStateOf(TreeMapSizing.STORAGE_SIZE) }
    var coloringMode by remember { mutableStateOf(TreeMapColoring.BY_CLUSTER) }
    var selectedFile by remember { mutableStateOf<SynapseFile?>(null) }
    var filterCategory by remember { mutableStateOf<FileCategory?>(null) }
    var isSimulatingPulse by remember { mutableStateOf(false) }

    // Pulsing animation for matching search results or re-indexing
    val infiniteTransition = rememberInfiniteTransition(label = "treemap_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Match map from file.id to similarity score
    val similarityMap = remember(searchResults) {
        searchResults.associate { it.file.id to it.similarityScore }
    }

    // Filter files if a category chip is selected
    val displayFiles = remember(files, filterCategory) {
        if (filterCategory == null) files else files.filter { it.category == filterCategory }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. HUD HEADER: TELEMETRY & CONTROLS ---
        CyberCard(
            borderColor = if (telemetry.isReindexing) NeonCyan else BorderSubtle,
            backgroundColor = DarkSurface,
            modifier = Modifier.testTag("synapse_treemap_header_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (telemetry.isReindexing) NeonCyan else EmeraldSecurity,
                                CircleShape
                            )
                    )
                    Text(
                        text = "SYNAPSE-FS SEMANTIC TREEMAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricBadge(
                        text = "${telemetry.vectorEmbeddingDimension}-D HNSW",
                        textColor = NeonCyan,
                        backgroundColor = Color(0xFF0C4A6E)
                    )
                    if (onViewFullFs != null && isCompact) {
                        IconButton(
                            onClick = onViewFullFs,
                            modifier = Modifier.size(28.dp).testTag("view_full_synapse_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open Full SynapseFS",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Re-indexing progress bar if active
            if (telemetry.isReindexing) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Optimizing HNSW Graph & PQ-8 Quantization...",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                        Text(
                            text = "${(telemetry.reindexingProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { telemetry.reindexingProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = NeonCyan,
                        trackColor = BorderSubtle
                    )
                }
            }

            // Quick Telemetry Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TreeMapKpiBadge(
                    label = "INDEXED INODES",
                    value = "${files.size} Nodes",
                    subtext = "${files.sumOf { it.sizeKb }} KB",
                    modifier = Modifier.weight(1f)
                )
                TreeMapKpiBadge(
                    label = "CACHE HIT RATIO",
                    value = String.format("%.1f%%", telemetry.cacheHitRatio),
                    subtext = "Direct Vector Hit",
                    color = EmeraldSecurity,
                    modifier = Modifier.weight(1f)
                )
                TreeMapKpiBadge(
                    label = "RETRIEVAL LATENCY",
                    value = String.format("%.1f ms", telemetry.avgRetrievalLatencyMs),
                    subtext = "Cosine kNN",
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                TreeMapKpiBadge(
                    label = "PREFETCH ACCURACY",
                    value = String.format("%.1f%%", telemetry.prefetchAccuracyPercent),
                    subtext = "Predictive Preload",
                    color = ElectricViolet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 2. RETRIEVAL PATTERN SIMULATOR & QUERY INPUT ---
        CyberCard(
            borderColor = if (searchQuery.isNotEmpty()) NeonCyan.copy(alpha = 0.6f) else BorderSubtle,
            backgroundColor = CardSurface,
            modifier = Modifier.testTag("synapse_query_simulator_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEMANTIC RETRIEVAL TRAJECTORY SIMULATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Button(
                        onClick = { viewModel.triggerSynapseReindex() },
                        enabled = !telemetry.isReindexing,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp).testTag("trigger_reindex_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceHighlight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reindex",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-Index HNSW", fontSize = 11.sp, color = NeonCyan)
                    }
                }

                // Query search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Query semantic meaning (e.g. 'burst scheduler weights', 'wasm sandbox')") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("synapse_treemap_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Fast Pattern Chips
                Text(
                    text = "Quick Pattern Triggers:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "D-Kernel Weights" to "scheduler ml burst weights",
                        "Zero-Trust eBPF" to "ebpf syscall security anomaly",
                        "WASI Sandbox" to "wasi liquid ui bytecode",
                        "User Intent" to "dense faiss user context vector"
                    )
                    presets.forEach { (label, q) ->
                        val isCurrent = searchQuery.equals(q, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) NeonCyan else BorderSubtle
                            ),
                            modifier = Modifier
                                .clickable {
                                    isSimulatingPulse = true
                                    viewModel.onSearchQueryChanged(if (isCurrent) "" else q)
                                }
                                .testTag("preset_query_${label.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = label,
                                color = if (isCurrent) NeonCyan else TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. VIEW CONTROLS: SIZING & COLORING MODES ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sizing Mode Dropdown / Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Size:", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 11.sp)
                TreeMapSizing.values().forEach { mode ->
                    val isSel = sizingMode == mode
                    FilterChip(
                        selected = isSel,
                        onClick = { sizingMode = mode },
                        label = { Text(mode.label, fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp).testTag("sizing_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Coloring Mode Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Color:", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 11.sp)
                TreeMapColoring.values().forEach { mode ->
                    val isSel = coloringMode == mode
                    FilterChip(
                        selected = isSel,
                        onClick = { coloringMode = mode },
                        label = { Text(mode.label, fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp).testTag("coloring_mode_${mode.name.lowercase()}")
                    )
                }
            }
        }

        // Category Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterCategory == null,
                onClick = { filterCategory = null },
                label = { Text("All Clusters (${files.size})", fontSize = 10.sp) },
                modifier = Modifier.height(26.dp).testTag("filter_cluster_all")
            )
            FileCategory.values().take(4).forEach { cat ->
                val isSel = filterCategory == cat
                FilterChip(
                    selected = isSel,
                    onClick = { filterCategory = if (isSel) null else cat },
                    label = { Text(cat.name.replace("_", " "), fontSize = 10.sp) },
                    modifier = Modifier.height(26.dp).testTag("filter_cluster_${cat.name.lowercase()}")
                )
            }
        }

        // --- 4. THE TREEMAP CANVAS ---
        CyberCard(
            borderColor = BorderSubtle,
            backgroundColor = DeepBackground,
            modifier = Modifier.testTag("synapse_treemap_canvas_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEMANTIC INODE DENSITY MAP (${displayFiles.size} Nodes)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Tap node to inspect vector inode",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 280.dp else 360.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                ) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()

                    // Compute Treemap Partition Rectangles
                    val computedRects = remember(displayFiles, sizingMode, similarityMap, canvasWidth, canvasHeight) {
                        calculateTreemapLayout(
                            files = displayFiles,
                            sizingMode = sizingMode,
                            similarityMap = similarityMap,
                            width = canvasWidth,
                            height = canvasHeight
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(computedRects) {
                                detectTapGestures { tapOffset ->
                                    val hit = computedRects.firstOrNull { rect ->
                                        tapOffset.x >= rect.x && tapOffset.x <= (rect.x + rect.width) &&
                                                tapOffset.y >= rect.y && tapOffset.y <= (rect.y + rect.height)
                                    }
                                    if (hit != null) {
                                        selectedFile = hit.file
                                        viewModel.simulateSynapseRetrieval(hit.file.id)
                                    }
                                }
                            }
                            .testTag("synapse_treemap_canvas")
                    ) {
                        val textPaint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            isAntiAlias = true
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                            textSize = 30f
                        }
                        val subTextPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#94A3B8")
                            isAntiAlias = true
                            typeface = Typeface.MONOSPACE
                            textSize = 22f
                        }
                        val tagPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#00F2FE")
                            isAntiAlias = true
                            typeface = Typeface.DEFAULT_BOLD
                            textSize = 20f
                        }

                        // Draw each Treemap cell
                        computedRects.forEach { treemapRect ->
                            val file = treemapRect.file
                            val sim = treemapRect.similarityScore
                            val isSelected = selectedFile?.id == file.id
                            val isMatchHighlight = searchQuery.isNotBlank() && sim > 0.70f

                            // Cell inner bounds with 2.5dp margin
                            val pad = 3f
                            val cellX = treemapRect.x + pad
                            val cellY = treemapRect.y + pad
                            val cellW = max(4f, treemapRect.width - (pad * 2))
                            val cellH = max(4f, treemapRect.height - (pad * 2))

                            val (cellColorStart, cellColorEnd) = getTreemapCellColors(
                                file = file,
                                coloringMode = coloringMode,
                                similarity = sim
                            )

                            // Background gradient
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(cellColorStart, cellColorEnd),
                                    startY = cellY,
                                    endY = cellY + cellH
                                ),
                                topLeft = Offset(cellX, cellY),
                                size = Size(cellW, cellH),
                                cornerRadius = CornerRadius(8f, 8f)
                            )

                            // Cell Border: highlight glowing border for matching search results or selected node
                            val borderColor = if (isSelected) {
                                NeonCyan
                            } else if (isMatchHighlight) {
                                NeonCyan.copy(alpha = pulseAlpha)
                            } else {
                                BorderSubtle.copy(alpha = 0.7f)
                            }
                            val borderWidth = if (isSelected || isMatchHighlight) 2.5f else 1f

                            drawRoundRect(
                                color = borderColor,
                                topLeft = Offset(cellX, cellY),
                                size = Size(cellW, cellH),
                                cornerRadius = CornerRadius(8f, 8f),
                                style = Stroke(width = borderWidth)
                            )

                            // Draw text labels if space permits
                            if (cellW > 70f && cellH > 40f) {
                                val canvasNative = drawContext.canvas.nativeCanvas

                                // Truncate filename if needed
                                val maxChars = max(4, (cellW / 18f).toInt())
                                val displayName = if (file.filename.length > maxChars) {
                                    file.filename.take(maxChars - 2) + ".."
                                } else file.filename

                                canvasNative.drawText(
                                    displayName,
                                    cellX + 12f,
                                    cellY + 34f,
                                    textPaint
                                )

                                if (cellH > 70f) {
                                    val infoStr = when (sizingMode) {
                                        TreeMapSizing.STORAGE_SIZE -> "${file.sizeKb} KB · ${file.cluster.take(14)}"
                                        TreeMapSizing.RETRIEVAL_HEAT -> "${file.accessCount} reads · ${(file.recentRetrievalFrequency * 100).toInt()}% heat"
                                        TreeMapSizing.SEMANTIC_SIMILARITY -> String.format("%.2f sim · %s", sim, file.cluster.take(10))
                                    }
                                    canvasNative.drawText(
                                        infoStr,
                                        cellX + 12f,
                                        cellY + 60f,
                                        subTextPaint
                                    )
                                }

                                if (cellH > 100f && cellW > 110f) {
                                    val tagText = "#${file.category.name.take(10)}"
                                    canvasNative.drawText(
                                        tagText,
                                        cellX + 12f,
                                        cellY + 86f,
                                        tagPaint
                                    )
                                }

                                // Hot retrieval pulse dot in top-right corner
                                if (file.recentRetrievalFrequency > 0.75f) {
                                    drawCircle(
                                        color = ThreatCrimson,
                                        radius = 5f,
                                        center = Offset(cellX + cellW - 14f, cellY + 14f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Legend row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (coloringMode) {
                            TreeMapColoring.BY_CLUSTER -> {
                                LegendItem(color = Color(0xFF0070F3), label = "Kernel")
                                LegendItem(color = Color(0xFF8A2BE2), label = "Weights")
                                LegendItem(color = Color(0xFF06B6D4), label = "WASI")
                                LegendItem(color = Color(0xFF10B981), label = "Security")
                                LegendItem(color = Color(0xFFF59E0B), label = "Logs")
                            }
                            TreeMapColoring.BY_RETRIEVAL_HEAT -> {
                                LegendItem(color = Color(0xFF1E3A8A), label = "Cold (0%)")
                                LegendItem(color = Color(0xFF06B6D4), label = "Normal")
                                LegendItem(color = Color(0xFFF59E0B), label = "Warm")
                                LegendItem(color = ThreatCrimson, label = "Hot (100%)")
                            }
                            TreeMapColoring.BY_SIMILARITY -> {
                                LegendItem(color = Color(0xFF1E293B), label = "<0.4 Low")
                                LegendItem(color = Color(0xFF8A2BE2), label = "0.7 Med")
                                LegendItem(color = NeonCyan, label = ">=0.85 High")
                            }
                        }
                    }

                    Text(
                        text = "Total: ${displayFiles.sumOf { it.sizeKb }} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- 5. HOTTEST INODES & RETRIEVAL PATTERNS STREAM ---
        CyberCard(
            borderColor = BorderSubtle,
            backgroundColor = CardSurface,
            modifier = Modifier.testTag("synapse_retrieval_stream_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE RETRIEVAL PATTERNS & PREFETCH PIPELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${telemetry.totalRetrievalsHandled} Queries Handled",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Top 3 Hot Inodes
                val hotFiles = remember(files) {
                    files.sortedByDescending { it.recentRetrievalFrequency }.take(3)
                }

                hotFiles.forEach { hFile ->
                    val sim = similarityMap[hFile.id] ?: 1.0f
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFile = hFile }
                            .testTag("hot_inode_row_${hFile.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = hFile.filename,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    MetricBadge(
                                        text = "${(hFile.recentRetrievalFrequency * 100).toInt()}% Heat",
                                        textColor = if (hFile.recentRetrievalFrequency > 0.85f) ThreatCrimson else WarningAmber,
                                        backgroundColor = CardSurfaceHighlight
                                    )
                                }
                                Text(
                                    text = "Affinity: ${hFile.prefetchAffinity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${hFile.accessCount} hits",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${hFile.retrievalLatencyMs} ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.simulateSynapseRetrieval(hFile.id) },
                                    modifier = Modifier.size(28.dp).testTag("simulate_read_${hFile.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Simulate Read",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 6. INODE SEMANTIC INSPECTOR MODAL ---
    if (selectedFile != null) {
        val f = selectedFile!!
        val sim = similarityMap[f.id] ?: 1.0f

        AlertDialog(
            onDismissRequest = { selectedFile = null },
            confirmButton = {
                TextButton(
                    onClick = { selectedFile = null },
                    modifier = Modifier.testTag("close_inode_inspector_btn")
                ) {
                    Text("Close", color = NeonCyan)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.simulateSynapseRetrieval(f.id)
                    },
                    modifier = Modifier.testTag("simulate_retrieval_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceHighlight)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trigger Retrieval Read", fontSize = 12.sp, color = NeonCyan)
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = f.filename,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    MetricBadge(
                        text = "${f.sizeKb} KB",
                        textColor = NeonCyan,
                        backgroundColor = DarkSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Path & Cluster
                    Text(
                        text = "VFS Mount: ${f.path}${f.filename}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Semantic Cluster: ${f.cluster} (${f.category.name})",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )

                    HorizontalDivider(color = BorderSubtle)

                    // Description
                    Text(
                        text = f.semanticDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    // Vector Embedding Preview
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Dense Vector Embedding (${f.vectorDimension}-D)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Text(
                                text = String.format("Cosine Sim: %.3f", sim),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sim > 0.8f) NeonCyan else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Bar visualization of embedding preview coordinates
                        Row(
                            modifier = Modifier.fillMaxWidth().height(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            f.embeddingPreview.forEachIndexed { idx, coord ->
                                val normHeight = ((coord + 1f) / 2f).coerceIn(0.1f, 1f)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(normHeight)
                                        .background(
                                            if (coord >= 0) NeonCyan.copy(alpha = 0.8f) else ElectricViolet.copy(alpha = 0.8f),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                        Text(
                            text = "Coords: [${f.embeddingPreview.joinToString(", ") { String.format("%.2f", it) }}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // Retrieval & Telemetry specs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Hits", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("${f.accessCount} reads", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Latency", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("${f.retrievalLatencyMs} ms", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Cache Hit", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("${f.cacheHitRate}%", color = EmeraldSecurity, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Tokens", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            Text("${f.tokenCount}", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Prefetch & Affinity
                    Text(
                        text = "Prefetch Rule: ${f.prefetchAffinity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextCyan,
                        fontSize = 11.sp
                    )
                }
            },
            containerColor = CardSurface
        )
    }
}

/**
 * KPI Badge inside Treemap Header
 */
@Composable
private fun TreeMapKpiBadge(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = subtext, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 9.sp)
        }
    }
}

/**
 * Legend indicator dot and label
 */
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, color = TextTertiary, fontSize = 10.sp)
    }
}

/**
 * Calculates a 2D recursive space-partitioning Treemap layout.
 * Ensures 100% space utilization without overlapping or exceeding boundary.
 */
private fun calculateTreemapLayout(
    files: List<SynapseFile>,
    sizingMode: TreeMapSizing,
    similarityMap: Map<String, Float>,
    width: Float,
    height: Float
): List<TreemapRect> {
    if (files.isEmpty() || width <= 0f || height <= 0f) return emptyList()

    // 1. Prepare items with positive weights
    data class TreemapItem(val file: SynapseFile, val weight: Float, val similarity: Float)

    val items = files.map { file ->
        val sim = similarityMap[file.id] ?: 1.0f
        val w = when (sizingMode) {
            TreeMapSizing.STORAGE_SIZE -> file.sizeKb.toFloat()
            TreeMapSizing.RETRIEVAL_HEAT -> (file.accessCount * 8f + file.recentRetrievalFrequency * 600f)
            TreeMapSizing.SEMANTIC_SIMILARITY -> (sim * 100f).coerceAtLeast(8f)
        }
        TreemapItem(file, max(5f, w), sim)
    }.sortedByDescending { it.weight }

    val results = mutableListOf<TreemapRect>()

    // 2. Recursive BSP function
    fun partition(
        subItems: List<TreemapItem>,
        x: Float,
        y: Float,
        w: Float,
        h: Float
    ) {
        if (subItems.isEmpty() || w <= 0f || h <= 0f) return

        if (subItems.size == 1) {
            val itm = subItems[0]
            results.add(
                TreemapRect(
                    id = itm.file.id,
                    file = itm.file,
                    x = x,
                    y = y,
                    width = w,
                    height = h,
                    similarityScore = itm.similarity
                )
            )
            return
        }

        val totalWeight = subItems.sumOf { it.weight.toDouble() }.toFloat()
        if (totalWeight <= 0f) return

        // Find split index closest to 50% weight
        var halfSum = 0f
        var splitIndex = 0
        val targetHalf = totalWeight / 2f
        for (i in subItems.indices) {
            halfSum += subItems[i].weight
            if (halfSum >= targetHalf) {
                splitIndex = max(1, i + 1)
                break
            }
        }
        if (splitIndex >= subItems.size) splitIndex = subItems.size - 1

        val leftGroup = subItems.take(splitIndex)
        val rightGroup = subItems.drop(splitIndex)
        val leftWeight = leftGroup.sumOf { it.weight.toDouble() }.toFloat()
        val ratio = (leftWeight / totalWeight).coerceIn(0.1f, 0.9f)

        // Split along the longer dimension
        if (w >= h) {
            val leftW = w * ratio
            val rightW = w - leftW
            partition(leftGroup, x, y, leftW, h)
            partition(rightGroup, x + leftW, y, rightW, h)
        } else {
            val topH = h * ratio
            val bottomH = h - topH
            partition(leftGroup, x, y, w, topH)
            partition(rightGroup, x, y + topH, w, bottomH)
        }
    }

    partition(items, 0f, 0f, width, height)
    return results
}

/**
 * Returns gradient start and end colors for a treemap block based on mode.
 */
private fun getTreemapCellColors(
    file: SynapseFile,
    coloringMode: TreeMapColoring,
    similarity: Float
): Pair<Color, Color> {
    return when (coloringMode) {
        TreeMapColoring.BY_CLUSTER -> {
            when (file.category) {
                FileCategory.KERNEL_CORE -> Color(0xFF003B73) to Color(0xFF0066CC)
                FileCategory.NEURAL_WEIGHTS -> Color(0xFF4A0E4E) to Color(0xFF8A2BE2)
                FileCategory.WASI_BYTECODE -> Color(0xFF083344) to Color(0xFF06B6D4)
                FileCategory.SECURITY_POLICY -> Color(0xFF064E3B) to Color(0xFF10B981)
                FileCategory.TELEMETRY_LOGS -> Color(0xFF78350F) to Color(0xFFD97706)
                FileCategory.USER_DATA -> Color(0xFF4C0519) to Color(0xFFBE123C)
            }
        }
        TreeMapColoring.BY_RETRIEVAL_HEAT -> {
            val heat = file.recentRetrievalFrequency
            when {
                heat < 0.35f -> Color(0xFF0F172A) to Color(0xFF1E3A8A) // Cold Blue
                heat < 0.65f -> Color(0xFF0C4A6E) to Color(0xFF0284C7) // Normal Cyan
                heat < 0.85f -> Color(0xFF78350F) to Color(0xFFF59E0B) // Warm Amber
                else -> Color(0xFF7F1D1D) to Color(0xFFEF4444)         // Hot Red
            }
        }
        TreeMapColoring.BY_SIMILARITY -> {
            when {
                similarity >= 0.85f -> Color(0xFF0284C7) to NeonCyan
                similarity >= 0.65f -> Color(0xFF4A0E4E) to Color(0xFF8A2BE2)
                else -> Color(0xFF0F172A) to Color(0xFF1E293B)
            }
        }
    }
}
