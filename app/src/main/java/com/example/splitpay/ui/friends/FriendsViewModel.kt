package com.example.splitpay.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.BalanceDetail
import com.example.splitpay.data.model.FriendWithBalance
import com.example.splitpay.data.repository.UserRepository // Assuming friends logic is here for now
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Enum for filtering state
enum class FriendFilterType {
    ALL, OUTSTANDING, OWES_YOU, YOU_OWE
}

// UI State for the Friends Screen
data class FriendsUiState(
    val isLoading: Boolean = true,
    val friends: List<FriendWithBalance> = emptyList(),
    val filteredFriends: List<FriendWithBalance> = emptyList(),
    val currentFilter: FriendFilterType = FriendFilterType.ALL,
    val totalNetBalance: Double = 0.0, // Overall balance across all friends
    val error: String? = null,
    val isFilterMenuExpanded: Boolean = false
)

class FriendsViewModel(
    // Inject UserRepository later, use default instance for now
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState

    // --- Action to open the filter menu ---
    fun onFilterIconClick() {
        _uiState.update { it.copy(isFilterMenuExpanded = true) }
    }

    // --- Action to dismiss the filter menu ---
    fun onDismissFilterMenu() {
        _uiState.update { it.copy(isFilterMenuExpanded = false) }
    }

    // --- Apply filter AND dismiss menu ---
    fun applyFilter(filterType: FriendFilterType) {
        val originalList = _uiState.value.friends
        val filtered = when (filterType) {
            FriendFilterType.ALL -> originalList
            FriendFilterType.OUTSTANDING -> originalList.filter { it.netBalance != 0.0 }
            FriendFilterType.OWES_YOU -> originalList.filter { it.netBalance > 0.0 }
            FriendFilterType.YOU_OWE -> originalList.filter { it.netBalance < 0.0 }
        }
        _uiState.update {
            it.copy(
                filteredFriends = filtered,
                currentFilter = filterType,
                isFilterMenuExpanded = false // <-- Dismiss menu on selection
            )
        }
    }

    init {
        loadMockFriends()
    }

    private fun loadMockFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Simulate network delay
            kotlinx.coroutines.delay(500)

            // --- MOCK DATA ---
            val mockFriendsList = listOf(
                FriendWithBalance(
                    uid = "friend_1",
                    username = "Nur Arysa",
                    netBalance = 32.67, // Positive: Owes you
                    balanceBreakdown = listOf(
                        BalanceDetail("KOKOLTRIP", 27.67),
                        BalanceDetail("Geng KualiMimie", 5.00)
                    )
                ),
                FriendWithBalance(
                    uid = "friend_2",
                    username = "Stevie",
                    netBalance = -11.00 // Negative: You owe them
                ),
                FriendWithBalance(
                    uid = "friend_3",
                    username = "John Doe",
                    netBalance = 0.00 // Settled up
                )
            )
            // --- END MOCK DATA ---

            val totalBalance = mockFriendsList.sumOf { it.netBalance }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    friends = mockFriendsList,
                    filteredFriends = mockFriendsList, // Initially show all
                    currentFilter = FriendFilterType.ALL,
                    totalNetBalance = totalBalance
                )
            }
        }
    }

    // Add functions later for search, add friend navigation, etc.
}