package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Mode for password dialog
 */
enum class PasswordDialogMode {
    CREATE,   // Creating new backup - requires password confirmation
    RESTORE   // Restoring backup - only requires password entry
}

/**
 * Reusable dialog component for entering/confirming backup password.
 *
 * This dialog supports two modes:
 * - CREATE: Shows password and confirm password fields for creating new backups
 * - RESTORE: Shows only password field for restoring existing backups
 *
 * Features:
 * - Password visibility toggle for both fields
 * - Password validation with error display
 * - Help text showing password requirements
 * - Enabled/disabled state based on input validity
 *
 * @param mode Dialog mode (CREATE or RESTORE)
 * @param password Current password value
 * @param confirmPassword Current confirm password value (used only in CREATE mode)
 * @param passwordError Error message to display, null if no error
 * @param onPasswordChange Callback when password changes
 * @param onConfirmPasswordChange Callback when confirm password changes
 * @param onConfirm Callback when user confirms (Create Backup or Restore)
 * @param onDismiss Callback when user cancels or dismisses dialog
 */
@Composable
fun PasswordDialog(
    mode: PasswordDialogMode,
    password: String,
    confirmPassword: String,
    passwordError: String?,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (mode) {
                    PasswordDialogMode.CREATE -> "Create Backup Password"
                    PasswordDialogMode.RESTORE -> "Enter Backup Password"
                }
            )
        },
        text = {
            Column {
                Text(
                    text = when (mode) {
                        PasswordDialogMode.CREATE -> "Create a strong password to encrypt your backup. You'll need this password to restore your data."
                        PasswordDialogMode.RESTORE -> "Enter the password you used to create this backup."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = passwordError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (mode == PasswordDialogMode.CREATE) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Confirm Password") },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        isError = passwordError != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (passwordError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = passwordError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (mode == PasswordDialogMode.CREATE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Password must be at least 8 characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = password.isNotEmpty() &&
                         (mode == PasswordDialogMode.RESTORE || confirmPassword.isNotEmpty())
            ) {
                Text(when (mode) {
                    PasswordDialogMode.CREATE -> "Create Backup"
                    PasswordDialogMode.RESTORE -> "Restore"
                })
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
