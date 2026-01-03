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
import kotlinx.coroutines.launch
import com.example.kanakku.ui.backup.BackupType
import com.example.kanakku.ui.backup.BackupUiState
import com.example.kanakku.ui.backup.BackupViewModel
import com.example.kanakku.ui.backup.ErrorRecoveryAction
import com.example.kanakku.ui.backup.OperationType
import com.example.kanakku.ui.components.BackupProgressCard
import com.example.kanakku.ui.components.PasswordDialog
import com.example.kanakku.ui.components.PasswordDialogMode
import com.example.kanakku.ui.components.PrivacyInfoCard
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

    // Show snackbar for success/error messages with recovery actions
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let { message ->
            val actionLabel = when (uiState.errorRecoveryAction) {
                is ErrorRecoveryAction.SignInToDrive -> "Sign In"
                is ErrorRecoveryAction.RetryOperation -> "Retry"
                is ErrorRecoveryAction.CheckPassword -> "Try Again"
                is ErrorRecoveryAction.CheckNetwork -> "Retry"
                is ErrorRecoveryAction.CheckPermissions -> "Settings"
                is ErrorRecoveryAction.CustomAction -> (uiState.errorRecoveryAction as ErrorRecoveryAction.CustomAction).label
                null -> null
            }

            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )

            // Handle action button clicks
            if (result == SnackbarResult.ActionPerformed) {
                when (uiState.errorRecoveryAction) {
                    is ErrorRecoveryAction.SignInToDrive -> onDriveSignIn()
                    is ErrorRecoveryAction.RetryOperation -> {
                        // User can manually retry
                    }
                    is ErrorRecoveryAction.CheckPassword -> {
                        showPasswordDialog = true
                    }
                    is ErrorRecoveryAction.CheckNetwork -> {
                        // User can manually retry
                    }
                    is ErrorRecoveryAction.CheckPermissions -> {
                        // Could open app settings in future
                    }
                    is ErrorRecoveryAction.CustomAction -> {
                        (uiState.errorRecoveryAction as ErrorRecoveryAction.CustomAction).action()
                    }
                    null -> {}
                }
            }

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
                    BackupProgressCard(
                        operationType = uiState.operationType,
                        progress = uiState.progress
                    )
                }
            }

            // Google Drive Sign-In Required Info
            if (uiState.backupType == BackupType.GOOGLE_DRIVE && !uiState.isDriveSignedIn) {
                item {
                    DriveSignInRequiredCard(onSignIn = onDriveSignIn)
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
                PrivacyInfoCard()
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

private fun formatBackupDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun DriveSignInRequiredCard(onSignIn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4) // Light yellow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color(0xFFF57F17), // Dark yellow
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sign-In Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF57F17)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please sign in to Google Drive to enable cloud backup and restore",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF795548)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF57F17)
                )
            ) {
                Text("Sign In")
            }
        }
    }
}
