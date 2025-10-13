package com.example.splitpay.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

fun slideInFromRight(): EnterTransition =
    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500))

fun slideOutToLeft(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(500))

fun slideInFromLeft(): EnterTransition =
    slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(500))

fun slideOutToRight(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(500))