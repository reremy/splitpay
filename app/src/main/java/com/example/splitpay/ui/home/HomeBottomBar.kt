package com.example.splitpay.ui.home

import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.SurfaceDark

// In a new file, e.g., HomeBottomBar.kt
@Composable
fun HomeBottomBar(
    uiState: HomeUiState,
    homeNavController: NavHostController,
    viewModel: HomeViewModel
) {
    NavigationBar (
        containerColor = MaterialTheme.colorScheme.surface
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

                colors = NavigationBarItemDefaults.colors(selectedIconColor = PositiveGreen, // Example selected green icon color
                selectedTextColor = PositiveGreen, // Example selected green text color
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // Example unselected icon color
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, // Example unselected text color
                indicatorColor = SurfaceDark // Example color for the indicator background
            )
            )
        }
    }
}
