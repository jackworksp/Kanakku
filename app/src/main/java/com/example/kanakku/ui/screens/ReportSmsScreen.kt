package com.example.kanakku.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for users to report undetected bank SMS messages.
 * Allows users to submit SMS details that were not automatically detected
 * as transactions, enabling pattern improvement and debugging.
 *
 * @param smsId Optional pre-filled SMS ID
 * @param senderAddress Optional pre-filled sender address
 * @param smsBody Optional pre-filled SMS body
 * @param smsDate Optional pre-filled SMS date
 * @param onBack Callback when user navigates back
 * @param onSubmit Callback when user submits the report with all SMS details and user notes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSmsScreen(
    smsId: Long? = null,
    senderAddress: String? = null,
    smsBody: String? = null,
    smsDate: Long? = null,
    onBack: () -> Unit,
    onSubmit: (Long?, String, String, Long, String?) -> Unit
) {
    var sender by remember { mutableStateOf(senderAddress ?: "") }
    var body by remember { mutableStateOf(smsBody ?: "") }
    var dateInput by remember { mutableStateOf(smsDate ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    var senderError by remember { mutableStateOf<String?>(null) }
    var bodyError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Undetected SMS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "ℹ️",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Help Us Improve",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Report SMS messages from your bank that weren't automatically detected. This helps us improve transaction detection for all users.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sender Address Field
            OutlinedTextField(
                value = sender,
                onValueChange = {
                    sender = it
                    senderError = null
                },
                label = { Text("Sender Address") },
                placeholder = { Text("e.g., VM-HDFCBK, AD-SBIBNK") },
                modifier = Modifier.fillMaxWidth(),
                isError = senderError != null,
                supportingText = {
                    senderError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    } ?: Text("The SMS sender ID (usually starts with VM-, AD-, or bank name)")
                },
                singleLine = true,
                enabled = !isSubmitting
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SMS Body Field
            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    bodyError = null
                },
                label = { Text("SMS Message") },
                placeholder = { Text("Enter the complete SMS text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                isError = bodyError != null,
                supportingText = {
                    bodyError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    } ?: Text("The full text of the bank SMS message")
                },
                minLines = 4,
                maxLines = 8,
                enabled = !isSubmitting
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SMS Date Field
            OutlinedTextField(
                value = formatDateTime(dateInput),
                onValueChange = { },
                label = { Text("SMS Date & Time") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = !isSubmitting,
                supportingText = { Text("When the SMS was received") },
                trailingIcon = {
                    TextButton(
                        onClick = { showDatePicker = true },
                        enabled = !isSubmitting
                    ) {
                        Text("Change")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes (Optional)") },
                placeholder = { Text("Any additional context or information") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                supportingText = { Text("Help us understand why this should be detected") },
                minLines = 3,
                maxLines = 5,
                enabled = !isSubmitting
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    // Validate inputs
                    var hasError = false

                    if (sender.isBlank()) {
                        senderError = "Sender address is required"
                        hasError = true
                    }

                    if (body.isBlank()) {
                        bodyError = "SMS message is required"
                        hasError = true
                    }

                    if (!hasError) {
                        isSubmitting = true
                        onSubmit(
                            smsId,
                            sender.trim(),
                            body.trim(),
                            dateInput,
                            notes.trim().takeIf { it.isNotBlank() }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submitting...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Note
            Text(
                text = "Note: The SMS content will be stored locally on your device to help improve transaction detection patterns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date Picker Dialog (placeholder - will be enhanced in future)
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Change Date") },
            text = {
                Text("Date picker functionality will be enhanced in the next update. Current date: ${formatDateTime(dateInput)}")
            },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Formats a timestamp into a human-readable date and time string.
 *
 * @param timestamp Time in milliseconds since epoch
 * @return Formatted date string (e.g., "15 Jan 2024, 02:30 PM")
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
