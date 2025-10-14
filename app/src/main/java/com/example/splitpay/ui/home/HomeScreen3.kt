package com.example.splitpay.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.splitpay.ui.profile.UserProfileScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen3(
    mainNavController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val homeNavController = rememberNavController()

    Scaffold(

        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    val currentItem = uiState.items[uiState.selectedItemIndex]

                    Text(
                        text = currentItem.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { /* open menu or settings */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },

        bottomBar = {
            NavigationBar {
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
                        icon = { Icon(item.icon, contentDescription = item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = homeNavController,
            startDestination = "groups_screen",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("groups_screen") { GroupsContent(innerPadding) }
            composable("friends_screen") { FriendsContent(innerPadding) }
            composable("activity_screen") { ActivityContent(innerPadding) }
            composable("profile_screen") {
                UserProfileScreen(mainNavController = mainNavController)
            }
        }
    }
}


@Composable
fun GroupsContent(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Groups Screen Content", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun FriendsContent(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Friends Screen Content", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ActivityContent(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Activity Screen Content", style = MaterialTheme.typography.titleLarge)
    }
}

//@Composable
//fun ProfileContent(innerPadding: PaddingValues) {UserProfileScreen()}

