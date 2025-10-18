package com.example.splitpay.ui.home

import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

// In a new file, e.g., HomeBottomBar.kt
@Composable
fun HomeBottomBar(
    uiState: HomeUiState,
    homeNavController: NavHostController,
    viewModel: HomeViewModel
) {
    NavigationBar (
        containerColor = Color(0xFF2D2D2D)
    ){
        uiState.items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = uiState.selectedItemIndex == index,
                onClick = {
                    viewModel.onItemSelected(index)
                    homeNavController.navigate(item.route) {
                        popUpTo(homeNavController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                label = { Text(item.label) },
                icon = { Icon(item.icon, contentDescription = item.label) },

                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF66BB6A), // Example selected green icon color
                selectedTextColor = Color(0xFF66BB6A), // Example selected green text color
                unselectedIconColor = Color(0xFFAAAAAA), // Example unselected icon color
                unselectedTextColor = Color(0xFFAAAAAA), // Example unselected text color
                indicatorColor = Color(0xFF3C3C3C) // Example color for the indicator background
            )
            )
        }
    }
}
