package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * A composable for entering and displaying keyword chips.
 *
 * Allows users to add keywords via a text field and displays them as chips.
 * Each chip can be removed individually. Includes validation to prevent
 * duplicate keywords and ensures keywords are trimmed.
 *
 * @param keywords Current list of keywords to display
 * @param onKeywordsChange Callback invoked when keywords list changes
 * @param modifier Optional modifier for the component
 * @param label Label text for the input field
 * @param placeholder Placeholder text for the input field
 * @param maxKeywords Maximum number of keywords allowed, null for no limit
 * @param enabled Whether the input is enabled for user interaction
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeywordChipInput(
    keywords: List<String>,
    onKeywordsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Keywords",
    placeholder: String = "Enter keyword and press +",
    maxKeywords: Int? = null,
    enabled: Boolean = true
) {
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Input field for adding keywords
        OutlinedTextField(
            value = currentInput,
            onValueChange = {
                currentInput = it
                // Clear error when user starts typing
                if (errorMessage != null) {
                    errorMessage = null
                }
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            enabled = enabled && (maxKeywords == null || keywords.size < maxKeywords),
            isError = errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (maxKeywords != null) {
                    Text(
                        text = "${keywords.size}/$maxKeywords keywords",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        val result = validateAndAddKeyword(
                            input = currentInput,
                            existingKeywords = keywords,
                            maxKeywords = maxKeywords
                        )
                        when (result) {
                            is KeywordValidationResult.Success -> {
                                onKeywordsChange(keywords + result.keyword)
                                currentInput = ""
                                errorMessage = null
                            }
                            is KeywordValidationResult.Error -> {
                                errorMessage = result.message
                            }
                        }
                    },
                    enabled = enabled && currentInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add keyword",
                        tint = if (currentInput.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val result = validateAndAddKeyword(
                        input = currentInput,
                        existingKeywords = keywords,
                        maxKeywords = maxKeywords
                    )
                    when (result) {
                        is KeywordValidationResult.Success -> {
                            onKeywordsChange(keywords + result.keyword)
                            currentInput = ""
                            errorMessage = null
                        }
                        is KeywordValidationResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Display existing keywords as chips
        if (keywords.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keywords.forEach { keyword ->
                    KeywordChip(
                        keyword = keyword,
                        onRemove = {
                            onKeywordsChange(keywords - keyword)
                            errorMessage = null
                        },
                        enabled = enabled
                    )
                }
            }
        }
    }
}

/**
 * A single keyword chip with a remove button.
 *
 * @param keyword The keyword text to display
 * @param onRemove Callback invoked when the remove button is clicked
 * @param enabled Whether the chip is enabled for interaction
 */
@Composable
private fun KeywordChip(
    keyword: String,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    InputChip(
        selected = false,
        onClick = { /* Chips are not clickable, only removable */ },
        label = {
            Text(
                text = keyword,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove keyword",
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        enabled = enabled,
        colors = InputChipDefaults.inputChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/**
 * Validates a keyword input and returns the result.
 *
 * @param input The raw input string to validate
 * @param existingKeywords List of existing keywords to check for duplicates
 * @param maxKeywords Maximum number of keywords allowed, null for no limit
 * @return ValidationResult indicating success with trimmed keyword or error with message
 */
private fun validateAndAddKeyword(
    input: String,
    existingKeywords: List<String>,
    maxKeywords: Int?
): KeywordValidationResult {
    val trimmed = input.trim()

    return when {
        trimmed.isEmpty() -> {
            KeywordValidationResult.Error("Keyword cannot be empty")
        }
        trimmed.length < 2 -> {
            KeywordValidationResult.Error("Keyword must be at least 2 characters")
        }
        trimmed.length > 30 -> {
            KeywordValidationResult.Error("Keyword must be at most 30 characters")
        }
        existingKeywords.any { it.equals(trimmed, ignoreCase = true) } -> {
            KeywordValidationResult.Error("Keyword already exists")
        }
        maxKeywords != null && existingKeywords.size >= maxKeywords -> {
            KeywordValidationResult.Error("Maximum $maxKeywords keywords allowed")
        }
        else -> {
            KeywordValidationResult.Success(trimmed)
        }
    }
}

/**
 * Sealed class representing the result of keyword validation.
 */
private sealed class KeywordValidationResult {
    /**
     * Validation succeeded with the trimmed keyword.
     *
     * @param keyword The validated and trimmed keyword
     */
    data class Success(val keyword: String) : KeywordValidationResult()

    /**
     * Validation failed with an error message.
     *
     * @param message User-friendly error message
     */
    data class Error(val message: String) : KeywordValidationResult()
}
