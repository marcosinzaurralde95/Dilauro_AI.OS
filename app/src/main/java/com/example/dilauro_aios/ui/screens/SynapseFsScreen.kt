package com.example.dilauro_aios.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dilauro_aios.model.FileCategory
import com.example.dilauro_aios.model.SynapseFile
import com.example.dilauro_aios.ui.components.CyberCard
import com.example.dilauro_aios.ui.components.MetricBadge
import com.example.dilauro_aios.ui.theme.*
import com.example.dilauro_aios.viewmodel.AiosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapseFsScreen(
    viewModel: AiosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<SynapseFile?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SynapseFS Semantic Filesystem", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("Vector Similarity Search & Contextual Metadata", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("synapse_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_file_button")) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "Add Semantic File", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Semantic Search Input
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth().testTag("synapse_search_input"),
                    placeholder = { Text("Search by meaning, e.g. 'burst weights', 'wasi', 'threat'") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Quick Tag Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "kernel", "ml", "wasi", "security").forEach { tag ->
                        val isSelected = (tag == "All" && query.isEmpty()) || query.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onSearchQueryChanged(if (tag == "All") "" else tag) },
                            label = { Text("#$tag", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Results count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Semantic Nodes Indexed (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (query.isNotBlank()) "Vector Ranked by Cosine Similarity" else "All Inode Descriptors",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            // File items
            items(searchResults, key = { it.file.id }) { result ->
                val file = result.file
                CyberCard(
                    modifier = Modifier
                        .clickable { selectedFile = file }
                        .testTag("file_card_${file.id}"),
                    borderColor = if (result.similarityScore > 0.85f && query.isNotBlank()) NeonCyan else BorderSubtle
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
                            Icon(
                                imageVector = when (file.category) {
                                    FileCategory.KERNEL_CORE -> Icons.Default.Memory
                                    FileCategory.NEURAL_WEIGHTS -> Icons.Default.Psychology
                                    FileCategory.WASI_BYTECODE -> Icons.Default.Terminal
                                    FileCategory.SECURITY_POLICY -> Icons.Default.Shield
                                    FileCategory.TELEMETRY_LOGS -> Icons.Default.ListAlt
                                    FileCategory.USER_DATA -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = file.filename,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${file.path} · ${file.sizeKb} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        MetricBadge(
                            text = String.format("%.2f Sim", result.similarityScore),
                            textColor = if (result.similarityScore > 0.8f) NeonCyan else TextSecondary,
                            backgroundColor = CardSurfaceHighlight
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = file.semanticDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            file.tags.take(3).forEach { t ->
                                MetricBadge(
                                    text = "#$t",
                                    textColor = TextCyan,
                                    backgroundColor = DarkSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteSynapseFile(file.id) },
                            modifier = Modifier.size(32.dp).testTag("delete_file_${file.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete File",
                                tint = ThreatCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Inspect Inode Dialog
    if (selectedFile != null) {
        val f = selectedFile!!
        AlertDialog(
            onDismissRequest = { selectedFile = null },
            confirmButton = {
                TextButton(onClick = { selectedFile = null }) {
                    Text("Close", color = NeonCyan)
                }
            },
            title = { Text(f.filename, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("VFS Path: ${f.path}${f.filename}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextSecondary)
                    Text("Category: ${f.category.name}", color = TextSecondary)
                    Text("Context Affinity: ${f.contextAffinity}", color = NeonCyan)
                    Text("Semantic Summary: ${f.semanticDescription}", color = TextSecondary)
                    Text("Access Count: ${f.accessCount} reads", color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Vector Embedding Projection (d=4):", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text(
                        text = f.embeddingPreview.joinToString(", ") { String.format("%.2f", it) },
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            },
            containerColor = CardSurface
        )
    }

    // Create Semantic File Dialog
    if (showAddDialog) {
        var fname by remember { mutableStateOf("") }
        var fpath by remember { mutableStateOf("/synapse/user/") }
        var fcategory by remember { mutableStateOf(FileCategory.USER_DATA) }
        var ftags by remember { mutableStateOf("notes, research") }
        var fdesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Semantic File", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fname,
                        onValueChange = { fname = it },
                        label = { Text("Filename") },
                        placeholder = { Text("kernel_patch.rs") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_filename_input")
                    )
                    OutlinedTextField(
                        value = fpath,
                        onValueChange = { fpath = it },
                        label = { Text("VFS Virtual Path") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ftags,
                        onValueChange = { ftags = it },
                        label = { Text("Comma-separated Tags") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fdesc,
                        onValueChange = { fdesc = it },
                        label = { Text("Semantic Context & Description") },
                        placeholder = { Text("e.g. Memory allocator page compaction RFC") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tagList = ftags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        viewModel.addSynapseFile(fname, fpath, fcategory, tagList, fdesc)
                        showAddDialog = false
                    },
                    modifier = Modifier.testTag("confirm_add_file_btn")
                ) {
                    Text("Save to SynapseFS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = CardSurface
        )
    }
}
