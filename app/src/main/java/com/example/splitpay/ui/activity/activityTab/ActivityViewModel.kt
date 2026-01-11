package com.example.splitpay.ui.activity.activityTab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Filter types
enum class ActivityFilterType {
    ALL,
    EXPENSES,
    PAYMENTS,
    GROUP_ACTIVITIES
}

enum class TimePeriodFilter {
    ALL_TIME,
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

// UI State for the Activity Screen
data class ActivityUiState(
    val isLoading: Boolean = true,
    val allActivities: List<Activity> = emptyList(),
    val filteredActivities: List<Activity> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val activityFilter: ActivityFilterType = ActivityFilterType.ALL,
    val timePeriodFilter: TimePeriodFilter = TimePeriodFilter.ALL_TIME,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val isFilterMenuExpanded: Boolean = false
)

class ActivityViewModel(
    private val activityRepository: ActivityRepository = ActivityRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val currentUserId: String? = userRepository.getCurrentUser()?.uid

    // Internal state for filters
    private val _searchQuery = MutableStateFlow("")
    private val _activityFilter = MutableStateFlow(ActivityFilterType.ALL)
    private val _timePeriodFilter = MutableStateFlow(TimePeriodFilter.ALL_TIME)
    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    private val _isSearchActive = MutableStateFlow(false)
    private val _isFilterMenuExpanded = MutableStateFlow(false)

    // This StateFlow will hold the UI state and automatically update
    val uiState: StateFlow<ActivityUiState> =
        combine(
            activityRepository.getActivityFeedFlow(currentUserId ?: ""),
            _searchQuery,
            _activityFilter,
            _timePeriodFilter,
            _sortOrder,
            _isSearchActive,
            _isFilterMenuExpanded
        ) { flows: Array<Any> ->
            val activities = flows[0] as List<Activity>
            val searchQuery = flows[1] as String
            val activityFilter = flows[2] as ActivityFilterType
            val timePeriodFilter = flows[3] as TimePeriodFilter
            val sortOrder = flows[4] as SortOrder
            val isSearchActive = flows[5] as Boolean
            val isFilterMenuExpanded = flows[6] as Boolean

            // Apply filters and search
            val filtered = activities
                .let { applyActivityTypeFilter(it, activityFilter) }
                .let { applyTimePeriodFilter(it, timePeriodFilter) }
                .let { applySearchFilter(it, searchQuery) }
                .let { applySorting(it, sortOrder) }

            ActivityUiState(
                isLoading = false,
                allActivities = activities,
                filteredActivities = filtered,
                error = null,
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                activityFilter = activityFilter,
                timePeriodFilter = timePeriodFilter,
                sortOrder = sortOrder,
                isFilterMenuExpanded = isFilterMenuExpanded
            )
        }
        .catch { e ->
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
            initialValue = ActivityUiState(isLoading = true)
        )

    // Search functions
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSearchIconClick() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            _searchQuery.value = ""
        }
    }

    fun onSearchDismiss() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    // Filter functions
    fun onFilterIconClick() {
        _isFilterMenuExpanded.value = !_isFilterMenuExpanded.value
    }

    fun onDismissFilterMenu() {
        _isFilterMenuExpanded.value = false
    }

    fun onActivityFilterChange(filter: ActivityFilterType) {
        _activityFilter.value = filter
    }

    fun onTimePeriodFilterChange(filter: TimePeriodFilter) {
        _timePeriodFilter.value = filter
    }

    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
    }

    // Helper functions for filtering
    private fun applyActivityTypeFilter(activities: List<Activity>, filter: ActivityFilterType): List<Activity> {
        return when (filter) {
            ActivityFilterType.ALL -> activities
            ActivityFilterType.EXPENSES -> activities.filter {
                val type = try { ActivityType.valueOf(it.activityType) } catch (e: Exception) { null }
                type == ActivityType.EXPENSE_ADDED || type == ActivityType.EXPENSE_UPDATED || type == ActivityType.EXPENSE_DELETED
            }
            ActivityFilterType.PAYMENTS -> activities.filter {
                val type = try { ActivityType.valueOf(it.activityType) } catch (e: Exception) { null }
                type == ActivityType.PAYMENT_MADE || type == ActivityType.PAYMENT_UPDATED || type == ActivityType.PAYMENT_DELETED
            }
            ActivityFilterType.GROUP_ACTIVITIES -> activities.filter {
                val type = try { ActivityType.valueOf(it.activityType) } catch (e: Exception) { null }
                type == ActivityType.GROUP_CREATED || type == ActivityType.GROUP_DELETED ||
                type == ActivityType.MEMBER_ADDED || type == ActivityType.MEMBER_REMOVED
            }
        }
    }

    private fun applyTimePeriodFilter(activities: List<Activity>, filter: TimePeriodFilter): List<Activity> {
        val now = System.currentTimeMillis()
        return when (filter) {
            TimePeriodFilter.ALL_TIME -> activities
            TimePeriodFilter.TODAY -> {
                val startOfDay = now - (now % 86400000) // Milliseconds in a day
                activities.filter { it.timestamp >= startOfDay }
            }
            TimePeriodFilter.THIS_WEEK -> {
                val weekAgo = now - (7 * 86400000)
                activities.filter { it.timestamp >= weekAgo }
            }
            TimePeriodFilter.THIS_MONTH -> {
                val monthAgo = now - (30L * 86400000)
                activities.filter { it.timestamp >= monthAgo }
            }
        }
    }

    private fun applySearchFilter(activities: List<Activity>, query: String): List<Activity> {
        if (query.isBlank()) return activities
        val lowerQuery = query.lowercase()
        return activities.filter { activity ->
            activity.displayText?.lowercase()?.contains(lowerQuery) == true ||
            activity.actorName.lowercase().contains(lowerQuery) ||
            activity.groupName?.lowercase()?.contains(lowerQuery) == true
        }
    }

    private fun applySorting(activities: List<Activity>, order: SortOrder): List<Activity> {
        return when (order) {
            SortOrder.NEWEST_FIRST -> activities.sortedByDescending { it.timestamp }
            SortOrder.OLDEST_FIRST -> activities.sortedBy { it.timestamp }
        }
    }
}