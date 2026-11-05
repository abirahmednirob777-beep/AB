package com.example.ui.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ResearchNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditNoteDialog(
    note: ResearchNote? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, topic: String, tags: List<String>, date: String, isPinned: Boolean) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var topic by remember { mutableStateOf(note?.topic ?: "General") }
    var date by remember { mutableStateOf(note?.date ?: today) }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: false) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(note?.tags ?: emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val topics = listOf("AI & ML", "Biotech", "Physics", "Systems", "Economics", "Mathematics", "General")

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (note == null) "New Research Note" else "Edit Research Note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { isPinned = !isPinned },
                    modifier = Modifier.testTag("dialog_pin_button")
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Note",
                        tint = if (isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Quick Templates
                if (note == null && content.isBlank()) {
                    Text(
                        text = "Research Templates:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                if (title.isBlank()) title = "Paper Review: "
                                content = "## Research Objective\n\n## Methodology & Data\n\n## Key Findings\n\n## Limitations & Critical Review\n\n## Key References & DOIs\n"
                                topic = "AI & ML"
                            },
                            label = { Text("Paper Review") }
                        )
                        AssistChip(
                            onClick = {
                                if (title.isBlank()) title = "Experiment Log: "
                                content = "## Objective & Hypothesis\n\n## Experimental Setup & Parameters\n\n## Step-by-Step Procedure\n\n## Observations & Raw Data\n\n## Conclusions\n"
                                topic = "Biotech"
                            },
                            label = { Text("Experiment Log") }
                        )
                        AssistChip(
                            onClick = {
                                if (title.isBlank()) title = "Daily Research Log: $today"
                                content = "## Daily Focus\n\n## Progress Made Today\n\n## Roadblocks & Inquiries\n\n## Next Steps for Tomorrow\n"
                                topic = "General"
                            },
                            label = { Text("Daily Log") }
                        )
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title *") },
                    placeholder = { Text("e.g. Neural Architecture Search in Low-Power Devices") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    singleLine = true
                )

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Topic selector chips
                Text(
                    text = "Topic:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topics.forEach { item ->
                        FilterChip(
                            selected = topic.equals(item, ignoreCase = true),
                            onClick = { topic = item },
                            label = { Text(item) }
                        )
                    }
                }

                // Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Add Tag") },
                        placeholder = { Text("e.g. transformer, arxiv") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val clean = tagInput.trim().lowercase()
                            if (clean.isNotBlank() && clean !in tags && tags.size < 10) {
                                tags = tags + clean
                                tagInput = ""
                            }
                        },
                        enabled = tagInput.isNotBlank() && tags.size < 10
                    ) {
                        Text("Add")
                    }
                }

                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    IconButton(
                                        onClick = { tags = tags - tag },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove tag",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Research Content & Observations") },
                    placeholder = { Text("Document detailed hypotheses, findings, mathematical formulas, and sources...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("note_content_input"),
                    maxLines = 15
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorText = "Title is required"
                        return@Button
                    }
                    onSave(title, content, topic, tags, date, isPinned)
                },
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
