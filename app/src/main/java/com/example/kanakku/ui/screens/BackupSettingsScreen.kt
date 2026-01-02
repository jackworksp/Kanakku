package com.example.kanakku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.ui.backup.BackupType
import com.example.kanakku.ui.backup.BackupUiState
import com.example.kanakku.ui.backup.BackupViewModel
import com.example.kanakku.ui.backup.OperationType
import com.example.kanakku.ui.components.PasswordDialog
import com.example.kanakku.ui.components.PasswordDialogMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: BackupViewModel,
    uiState: BackupUiState,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDriveSignIn: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordDialogMode by remember { mutableStateOf(PasswordDialogMode.CREATE) }
    var showPrivacyInfo by remember { mutableStateOf(false) }

    // Show snackbar for success/error messages
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Backup Type Selector
            item {
                BackupTypeSelector(
                    selectedType = uiState.backupType,
                    onTypeSelected = { viewModel.selectBackupType(it) },
                    isDriveSignedIn = uiState.isDriveSignedIn,
                    onDriveSignIn = onDriveSignIn,
                    onDriveSignOut = { viewModel.signOutFromDrive() }
                )
            }

            // Progress Card
            if (uiState.isLoading) {
                item {
                    ProgressCard(
                        operationType = uiState.operationType,
                        progress = uiState.progress
                    )
                }
            }

            // Create Backup Section
            item {
                BackupActionCard(
                    title = "Create Backup",
                    description = "Save your data securely with encryption",
                    icon = Icons.Filled.Backup,
                    buttonText = "Create Backup",
                    enabled = !uiState.isLoading &&
                             (uiState.backupType == BackupType.LOCAL || uiState.isDriveSignedIn),
                    onClick = {
                        passwordDialogMode = PasswordDialogMode.CREATE
                        showPasswordDialog = true
                    }
                )
            }

            // Restore Backup Section
            item {
                BackupActionCard(
                    title = "Restore Backup",
                    description = "Restore your data from a backup file",
                    icon = Icons.Filled.CloudDownload,
                    buttonText = "Restore Backup",
                    enabled = !uiState.isLoading &&
                             (uiState.backupType == BackupType.LOCAL || uiState.isDriveSignedIn),
                    onClick = {
                        passwordDialogMode = PasswordDialogMode.RESTORE
                        showPasswordDialog = true
                    }
                )
            }

            // Last Backup Info
            if (uiState.lastBackupMetadata != null) {
                item {
                    LastBackupCard(
                        timestamp = uiState.lastBackupMetadata.timestamp,
                        transactionCount = uiState.lastBackupMetadata.transactionCount,
                        categoryCount = uiState.lastBackupMetadata.categoryCount
                    )
                }
            }

            // Privacy Information
            item {
                PrivacyInfoCard(
                    onShowDetails = { showPrivacyInfo = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Password Dialog
    if (showPasswordDialog) {
        PasswordDialog(
            mode = passwordDialogMode,
            password = uiState.password,
            confirmPassword = uiState.confirmPassword,
            passwordError = uiState.passwordError,
            onPasswordChange = { viewModel.updatePassword(it) },
            onConfirmPasswordChange = { viewModel.updateConfirmPassword(it) },
            onConfirm = {
                showPasswordDialog = false
                when (passwordDialogMode) {
                    PasswordDialogMode.CREATE -> onCreateBackup()
                    PasswordDialogMode.RESTORE -> onRestoreBackup()
                }
            },
            onDismiss = {
                showPasswordDialog = false
                viewModel.resetState()
            }
        )
    }

    // Privacy Info Dialog
    if (showPrivacyInfo) {
        PrivacyInfoDialog(
            onDismiss = { showPrivacyInfo = false }
        )
    }
}

@Composable
private fun BackupTypeSelector(
    selectedType: BackupType,
    onTypeSelected: (BackupType) -> Unit,
    isDriveSignedIn: Boolean,
    onDriveSignIn: () -> Unit,
    onDriveSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Backup Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == BackupType.LOCAL,
                    onClick = { onTypeSelected(BackupType.LOCAL) },
                    label = { Text("Local Storage") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedType == BackupType.GOOGLE_DRIVE,
                    onClick = { onTypeSelected(BackupType.GOOGLE_DRIVE) },
                    label = { Text("Google Drive") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CloudQueue,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Google Drive Sign-In Status
            if (selectedType == BackupType.GOOGLE_DRIVE) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDriveSignedIn) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = if (isDriveSignedIn) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDriveSignedIn) "Signed In" else "Not Signed In",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (isDriveSignedIn) {
                        TextButton(onClick = onDriveSignOut) {
                            Text("Sign Out")
                        }
                    } else {
                        Button(onClick = onDriveSignIn) {
                            Text("Sign In")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun ProgressCard(
    operationType: OperationType,
    progress: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = when (operationType) {
                        OperationType.CREATING_BACKUP -> "Creating Backup"
                        OperationType.RESTORING_BACKUP -> "Restoring Backup"
                        OperationType.VALIDATING_PASSWORD -> "Validating Password"
                        OperationType.LOADING_BACKUPS -> "Loading Backups"
                        OperationType.DELETING_BACKUP -> "Deleting Backup"
                        OperationType.IDLE -> "Processing"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (progress.isNotEmpty()) {
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LastBackupCard(
    timestamp: Long,
    transactionCount: Int,
    categoryCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Last Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatBackupDate(timestamp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$transactionCount transactions • $categoryCount categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyInfoCard(
    onShowDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = Color(0xFF7B1FA2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B1FA2)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your backup is encrypted with AES-256 and secured with your password. Data stays under your control.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onShowDetails,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Learn More")
            }
        }
    }
}

@Composable
private fun PrivacyInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = Color(0xFF7B1FA2)
            )
        },
        title = { Text("Privacy & Security") },
        text = {
            Column {
                Text(
                    text = "What's included in your backup:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• All parsed bank transactions", style = MaterialTheme.typography.bodySmall)
                Text("• Category assignments and overrides", style = MaterialTheme.typography.bodySmall)
                Text("• App preferences and settings", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Security features:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• AES-256-GCM encryption", style = MaterialTheme.typography.bodySmall)
                Text("• Password-based key derivation (PBKDF2)", style = MaterialTheme.typography.bodySmall)
                Text("• 100,000 iterations for key strength", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Privacy guarantees:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Data stays under your control", style = MaterialTheme.typography.bodySmall)
                Text("• No app servers involved", style = MaterialTheme.typography.bodySmall)
                Text("• Local storage or your personal Google Drive", style = MaterialTheme.typography.bodySmall)
                Text("• Google Drive uses app-specific folder", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

private fun formatBackupDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
