package com.example.splitpay.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close // Import Close icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester // Import FocusRequester
import androidx.compose.ui.focus.focusRequester // Import modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.TextPlaceholder
import com.example.splitpay.ui.theme.TextWhite
import com.example.splitpay.ui.theme.PrimaryBlue // Assuming you might need this

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior?,
    // --- NEW Search Parameters ---
    isSearchActive: Boolean = false, // Is search mode active?
    searchQuery: String = "",        // Current search text
    onSearchQueryChange: (String) -> Unit = {}, // Callback for text changes
    onSearchClose: () -> Unit = {}, // Callback to close search mode
    focusRequester: FocusRequester = FocusRequester(), // To manage focus
    // --- END NEW ---
    actions: @Composable RowScope.() -> Unit = {} // Original actions lambda
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
        // --- Conditionally display Title/Actions OR Search Field ---
        title = {
            if (isSearchActive) {
                // Search TextField takes the title slot
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 0.dp) // Use padding on Row instead if needed
                        //.height(48.dp) // Control height
                        .focusRequester(focusRequester), // Apply focus requester
                    placeholder = { Text("Search friends...", color = TextPlaceholder) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = TextWhite, fontSize = MaterialTheme.typography.bodyLarge.fontSize), // Match title style
                    colors = TextFieldDefaults.colors( // Minimal styling
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = TextWhite, // White cursor
                        focusedIndicatorColor = Color.Transparent, // No underline
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        disabledTextColor = Color.Gray,
                        focusedPlaceholderColor = TextPlaceholder,
                        unfocusedPlaceholderColor = TextPlaceholder
                    )
                )
            } else {
                // Default: Show the title passed in
                Text(text = title, color = TextWhite)
            }
        },
        // Navigation icon could be added here if needed (e.g., back arrow when search active)

        // --- Conditionally display Actions OR Close Button ---
        actions = {
            if (isSearchActive) {
                // Show Close button instead of standard actions
                IconButton(onClick = onSearchClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Search", tint = TextWhite)
                }
            } else {
                // Default: Show the actions passed in via lambda
                actions()
            }
        },
        scrollBehavior = scrollBehavior
    )
}