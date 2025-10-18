package com.example.splitpay.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.repository.GroupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val repository: GroupsRepository = GroupsRepository()
) : ViewModel() {
    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            // Only load if group ID has changed or if it hasn't been loaded yet
            if (_group.value?.id != groupId || _group.value == null) {
                _isLoading.value = true
                // Fetch data using the real repository function
                val fetchedGroup = repository.getGroupById(groupId)
                _group.value = fetchedGroup
                _isLoading.value = false
            }
        }
    }
}