package com.example.kanakku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A dialog for selecting category icons from a grid of emoji icons grouped by category.
 *
 * Icons are organized into tabs by category (Food, Transport, Shopping, etc.) for easy browsing.
 * Users can select an emoji which will be used as the category icon.
 *
 * @param currentIcon The currently selected icon (emoji), or null if none selected
 * @param onIconSelected Callback invoked when an icon is selected, receives the selected emoji
 * @param onDismiss Callback invoked when the dialog is dismissed
 */
@Composable
fun IconPickerDialog(
    currentIcon: String?,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val iconGroups = remember { getIconGroups() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Icon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tab row for icon categories
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 0.dp
                ) {
                    iconGroups.forEachIndexed { index, group ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Icon grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(iconGroups[selectedTab].icons) { icon ->
                        IconPickerItem(
                            icon = icon,
                            isSelected = icon == currentIcon,
                            onClick = { onIconSelected(icon) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

/**
 * A single icon item in the picker grid.
 *
 * @param icon The emoji character to display
 * @param isSelected Whether this icon is currently selected
 * @param onClick Callback invoked when this icon is clicked
 */
@Composable
private fun IconPickerItem(
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )

        // Show check mark on selected icon
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            )
        }
    }
}

/**
 * Data class representing a group of related icons.
 *
 * @property name Display name for this icon group (e.g., "Food", "Transport")
 * @property icons List of emoji characters in this group
 */
private data class IconGroup(
    val name: String,
    val icons: List<String>
)

/**
 * Returns all icon groups organized by category.
 *
 * @return List of IconGroup objects containing categorized emoji icons
 */
private fun getIconGroups(): List<IconGroup> {
    return listOf(
        IconGroup(
            name = "Food",
            icons = listOf(
                "🍔", "🍕", "🍝", "🍜", "🍱", "🍛",
                "🍣", "🍤", "🥗", "🥙", "🌮", "🌯",
                "🍿", "🧆", "🥘", "🍲", "🍳", "🥞",
                "🧇", "🥓", "🍖", "🍗", "🥩", "🍞",
                "🥐", "🥖", "🥨", "🧀", "🥚", "🍰",
                "🎂", "🧁", "🥧", "🍦", "🍧", "🍨",
                "🍩", "🍪", "☕", "🍵", "🥤", "🧃",
                "🥛", "🍺", "🍻", "🍷", "🥂", "🍾"
            )
        ),
        IconGroup(
            name = "Transport",
            icons = listOf(
                "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️",
                "🚓", "🚑", "🚒", "🚐", "🛻", "🚚",
                "🚛", "🚜", "🏍️", "🛵", "🚲", "🛴",
                "🚁", "✈️", "🛫", "🛬", "🚀", "🛸",
                "🚂", "🚃", "🚄", "🚅", "🚆", "🚇",
                "🚈", "🚉", "🚊", "🚝", "🚞", "🚋",
                "🚌", "🚍", "🚎", "⛽", "🛢️", "🚏",
                "🚥", "🚦", "🛑", "🚧", "⚓", "⛵"
            )
        ),
        IconGroup(
            name = "Shopping",
            icons = listOf(
                "🛍️", "🛒", "💳", "💰", "💵", "💴",
                "💶", "💷", "💸", "🏪", "🏬", "🏢",
                "🏛️", "🏦", "🏨", "🏩", "👕", "👔",
                "👗", "👘", "👚", "👖", "👙", "🧥",
                "🧤", "🧣", "🧦", "👠", "👡", "👢",
                "👞", "👟", "🥾", "👜", "👝", "👛",
                "🎒", "💼", "👓", "🕶️", "🥽", "💍",
                "💎", "📦", "📫", "📪", "📬", "📭"
            )
        ),
        IconGroup(
            name = "Health",
            icons = listOf(
                "💊", "💉", "🩺", "🩹", "🩼", "🩻",
                "🏥", "⚕️", "🔬", "🧬", "🧪", "🧫",
                "🌡️", "🩸", "😷", "🤒", "🤕", "🤢",
                "🤮", "🤧", "🧘", "🏃", "🚴", "🏋️",
                "🤸", "🧗", "🤺", "🏌️", "🏇", "⛷️",
                "🏂", "🏄", "🚣", "🏊", "⛹️", "🤾",
                "🧑‍⚕️", "👨‍⚕️", "👩‍⚕️", "🫀", "🫁", "🦴",
                "🦷", "👁️", "👂", "🧠", "🩺", "💪"
            )
        ),
        IconGroup(
            name = "Entertainment",
            icons = listOf(
                "🎬", "🎭", "🎪", "🎨", "🎤", "🎧",
                "🎼", "🎹", "🥁", "🎷", "🎺", "🎸",
                "🪕", "🎻", "🎲", "♟️", "🎯", "🎳",
                "🎮", "🎰", "🧩", "🃏", "🀄", "🎴",
                "📺", "📻", "📱", "💻", "🖥️", "⌨️",
                "🖱️", "🕹️", "🎥", "📷", "📸", "📹",
                "📼", "🔍", "🔎", "🕯️", "💡", "🔦",
                "🏮", "🎆", "🎇", "✨", "🎈", "🎉"
            )
        ),
        IconGroup(
            name = "Bills",
            icons = listOf(
                "📄", "📃", "📑", "📊", "📈", "📉",
                "🗒️", "🗓️", "📅", "📆", "🗂️", "📋",
                "📌", "📍", "📎", "🖇️", "📏", "📐",
                "✂️", "🗃️", "🗄️", "🗑️", "💡", "🔌",
                "🔋", "🪫", "💧", "🚰", "🚿", "🛁",
                "🚽", "🪠", "🧻", "🧼", "🧽", "🧹",
                "🧺", "🔥", "🕯️", "💨", "🌬️", "☁️",
                "⚡", "🌩️", "🌪️", "🌫️", "🌀", "🌊"
            )
        ),
        IconGroup(
            name = "Education",
            icons = listOf(
                "📚", "📖", "📕", "📗", "📘", "📙",
                "📓", "📔", "📒", "📝", "✏️", "✒️",
                "🖊️", "🖋️", "🖍️", "🖌️", "🔖", "📑",
                "🎓", "🎒", "🏫", "🏛️", "🏢", "🔬",
                "🔭", "🧮", "📐", "📏", "📊", "📈",
                "🗺️", "🌍", "🌎", "🌏", "🗾", "🧭",
                "⚗️", "🧪", "🧫", "🧬", "🔍", "🔎",
                "💡", "🔦", "📡", "🛰️", "🚀", "🛸"
            )
        ),
        IconGroup(
            name = "Home",
            icons = listOf(
                "🏠", "🏡", "🏘️", "🏚️", "🏗️", "🏭",
                "🏢", "🏬", "🏣", "🏤", "🏥", "🏦",
                "🏨", "🏪", "🏫", "🏩", "💒", "🏛️",
                "⛪", "🕌", "🕍", "🛕", "🕋", "⛩️",
                "🗼", "🗽", "⛲", "⛺", "🌁", "🌃",
                "🏙️", "🌄", "🌅", "🌆", "🌇", "🌉",
                "🛋️", "🪑", "🚪", "🪟", "🛏️", "🛌",
                "🖼️", "🪞", "🧸", "🎁", "🎀", "🎊"
            )
        ),
        IconGroup(
            name = "Misc",
            icons = listOf(
                "📦", "📧", "📨", "📩", "📤", "📥",
                "📮", "📪", "📫", "📬", "📭", "📯",
                "📜", "📃", "📊", "📋", "📌", "📍",
                "🔑", "🗝️", "🔨", "⚒️", "🛠️", "⛏️",
                "🔧", "🔩", "⚙️", "🗜️", "⚖️", "🦯",
                "🔗", "⛓️", "🧰", "🧲", "🪜", "⚗️",
                "🔬", "🔭", "📡", "💉", "💊", "🩹",
                "🩺", "🩻", "🩼", "🧬", "🧪", "🧫"
            )
        )
    )
}
