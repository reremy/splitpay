package com.example.splitpay.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logE
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// UI State for the Activity Screen
data class ActivityUiState(
    val isLoading: Boolean = true,
    val activities: List<Activity> = emptyList(),
    val error: String? = null
)

class ActivityViewModel(
    private val activityRepository: ActivityRepository = ActivityRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val currentUserId: String? = userRepository.getCurrentUser()?.uid

    // This StateFlow will hold the UI state and automatically update
    val uiState: StateFlow<ActivityUiState> =
        activityRepository.getActivityFeedFlow(currentUserId ?: "")
            .map { activities ->
                val sortedActivities = activities.sortedByDescending { it.timestamp }
                // Data has loaded, map it to the success state
                ActivityUiState(
                    isLoading = false,
                    activities = sortedActivities,
                    error = null
                )
            }
            .catch { e ->
                // An error occurred in the flow
                logE("Error collecting activity feed: ${e.message}")
                emit(
                    ActivityUiState(
                        isLoading = false,
                        error = "Failed to load activity feed."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ActivityUiState(isLoading = true) // Initial state is loading
            )
}