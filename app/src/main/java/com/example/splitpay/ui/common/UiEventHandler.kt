package com.example.splitpay.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun <T> UiEventHandler(
    uiEvent: SharedFlow<T>,
    onEvent: (T) -> Unit
) {
    if (!LocalInspectionMode.current) {
        LaunchedEffect(Unit) {
            uiEvent.collect { event ->
                onEvent(event)
            }
        }
    }
}