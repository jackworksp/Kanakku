package com.example.kanakku.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

/**
 * A Material3 Text composable that highlights matching portions of text with a background color.
 *
 * This component provides:
 * - Case-insensitive search matching
 * - Background color highlighting for matched text
 * - Support for partial matches anywhere in the text
 * - Multiple match highlighting (all occurrences are highlighted)
 *
 * Use cases:
 * - Search result highlighting in transaction lists
 * - Emphasizing matched portions in merchant names or descriptions
 * - Visual feedback for search queries
 *
 * @param text The full text to display
 * @param highlight The search query to highlight (case-insensitive). If null or empty, displays plain text.
 * @param highlightColor The background color for highlighted text. Defaults to primary color with transparency.
 * @param modifier Optional modifier for customizing the component's appearance and layout
 * @param style The text style to apply. Defaults to the current LocalTextStyle.
 * @param overflow How to handle text overflow. Defaults to Clip.
 * @param maxLines Maximum number of lines to display. Defaults to Int.MAX_VALUE (no limit).
 */
@Composable
fun HighlightedText(
    text: String,
    highlight: String?,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    // If highlight is null or empty, display plain text
    if (highlight.isNullOrBlank()) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            overflow = overflow,
            maxLines = maxLines
        )
        return
    }

    // Build annotated string with highlighted matches
    val annotatedString = buildHighlightedString(
        text = text,
        highlight = highlight,
        highlightColor = highlightColor
    )

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        overflow = overflow,
        maxLines = maxLines
    )
}

/**
 * Builds an AnnotatedString with highlighted matching portions.
 *
 * This function performs case-insensitive matching and highlights all occurrences
 * of the search query within the text.
 *
 * @param text The full text to process
 * @param highlight The search query to find and highlight
 * @param highlightColor The background color for highlighted portions
 * @return An AnnotatedString with highlighted matches
 */
private fun buildHighlightedString(
    text: String,
    highlight: String,
    highlightColor: Color
): AnnotatedString = buildAnnotatedString {
    if (text.isEmpty() || highlight.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }

    // Convert to lowercase for case-insensitive matching
    val lowerText = text.lowercase()
    val lowerHighlight = highlight.lowercase()

    var currentIndex = 0
    var matchIndex = lowerText.indexOf(lowerHighlight, currentIndex)

    // Find and highlight all matches
    while (matchIndex != -1) {
        // Append text before the match (no highlighting)
        if (matchIndex > currentIndex) {
            append(text.substring(currentIndex, matchIndex))
        }

        // Append the matched portion with highlighting
        withStyle(
            style = SpanStyle(
                background = highlightColor
            )
        ) {
            append(text.substring(matchIndex, matchIndex + highlight.length))
        }

        // Move to the next potential match position
        currentIndex = matchIndex + highlight.length
        matchIndex = lowerText.indexOf(lowerHighlight, currentIndex)
    }

    // Append any remaining text after the last match
    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}
