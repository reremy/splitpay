package com.example.splitpay.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.ActivityType
import androidx.navigation.NavHostController
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.DialogBackground
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.absoluteValue

/**
 * The main composable for the Activity screen.
 * This is what will be called by the NavHost in HomeScreen.
 */
@Composable
fun ActivityScreen(
    innerPadding: PaddingValues,
    navController: NavHostController,
    viewModel: ActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    when {
        uiState.isLoading -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }
        uiState.error != null -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${uiState.error}", color = NegativeRed)
            }
        }
        uiState.filteredActivities.isEmpty() -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.searchQuery.isNotBlank() ||
                               uiState.activityFilter != ActivityFilterType.ALL ||
                               uiState.timePeriodFilter != TimePeriodFilter.ALL_TIME) {
                        "No activities match your filters"
                    } else {
                        "No recent activity found."
                    },
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
        else -> {
            // Group activities by date
            val groupedActivities = remember(uiState.filteredActivities) {
                groupActivitiesByDate(uiState.filteredActivities)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                )
            ) {
                groupedActivities.forEach { (dateHeader, activities) ->
                    // Date header
                    item(key = "header_$dateHeader") {
                        DateHeader(dateHeader = dateHeader)
                    }

                    // Activities for this date
                    items(activities, key = { it.id }) { activity ->
                        ActivityCard(
                            activity = activity,
                            currentUserId = currentUserId ?: "",
                            onClick = {
                                val activityType = try {
                                    ActivityType.valueOf(activity.activityType)
                                } catch (e: Exception) {
                                    null
                                }

                                when (activityType) {
                                    ActivityType.EXPENSE_ADDED -> {
                                        if (activity.entityId != null) {
                                            navController.navigate("${Screen.ExpenseDetail}/${activity.entityId}")
                                        }
                                    }
                                    ActivityType.EXPENSE_UPDATED,
                                    ActivityType.EXPENSE_DELETED -> {
                                        // Do nothing
                                    }
                                    ActivityType.PAYMENT_MADE,
                                    ActivityType.PAYMENT_UPDATED -> {
                                        if (activity.entityId != null) {
                                            navController.navigate("${Screen.PaymentDetail}/${activity.entityId}")
                                        }
                                    }
                                    ActivityType.PAYMENT_DELETED -> {
                                        // Do nothing
                                    }
                                    else -> {
                                        navController.navigate("${Screen.ActivityDetail}?activityId=${activity.id}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Groups activities by date (Today, Yesterday, or formatted date)
 */
private fun groupActivitiesByDate(activities: List<Activity>): List<Pair<String, List<Activity>>> {
    val groups = mutableMapOf<String, MutableList<Activity>>()

    activities.forEach { activity ->
        val dateHeader = getDateHeader(activity.timestamp)
        groups.getOrPut(dateHeader) { mutableListOf() }.add(activity)
    }

    // Return in chronological order (most recent first)
    return groups.entries
        .sortedByDescending { it.value.firstOrNull()?.timestamp ?: 0L }
        .map { it.key to it.value }
}

/**
 * Returns date header string (Today, Yesterday, or formatted date)
 */
private fun getDateHeader(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance()
    time.timeInMillis = timestamp

    val dateSdf = SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault())

    // Check if it's today
    if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
        return "Today"
    }

    // Check if it was yesterday
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday"
    }

    // Older than yesterday
    return dateSdf.format(Date(timestamp))
}

/**
 * Date header composable
 */
@Composable
fun DateHeader(dateHeader: String) {
    Text(
        text = dateHeader,
        color = Color.Gray,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/**
 * A single card representing one activity item in the feed.
 */
@Composable
fun ActivityCard(
    activity: Activity,
    currentUserId: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DialogBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 1. Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getActivityIcon(activity.activityType),
                    contentDescription = "Activity Icon",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))

            // 2. Text Content
            Column(modifier = Modifier.weight(1f)) {
                // Main Text (e.g., "You added 'twst'")
                Text(
                    text = formatDisplayText(activity, currentUserId),
                    color = TextWhite,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(4.dp))

                // Financial Summary (e.g., "You owe MYR333.33")
                val financialSummary = formatFinancialSummary(activity, currentUserId)
                if (financialSummary.text.isNotEmpty()) {
                    Text(
                        text = financialSummary,
                        color = getFinancialSummaryColor(activity, currentUserId),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Timestamp (e.g., "12:20")
                Text(
                    text = formatTimestampShort(activity.timestamp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// --- Helper Functions for Formatting ---

/**
 * Returns a placeholder icon based on the activity type.
 */
private fun getActivityIcon(activityTypeStr: String): ImageVector {
    return try {
        when (ActivityType.valueOf(activityTypeStr)) {
            ActivityType.GROUP_CREATED,
            ActivityType.GROUP_DELETED -> Icons.Default.Group
            ActivityType.MEMBER_ADDED -> Icons.Default.PersonAdd // <-- Keep this
            ActivityType.MEMBER_REMOVED, // <-- ADD THIS
            ActivityType.MEMBER_LEFT -> Icons.Default.PersonRemove // <-- Use PersonRemove icon
            ActivityType.EXPENSE_ADDED,
            ActivityType.EXPENSE_UPDATED,
            ActivityType.EXPENSE_DELETED -> Icons.Default.Description
            ActivityType.PAYMENT_MADE,
            ActivityType.PAYMENT_UPDATED,
            ActivityType.PAYMENT_DELETED -> Icons.Default.Payment
        }
    } catch (e: IllegalArgumentException) {
        Icons.Default.Description // Fallback icon
    }
}

/**
 * Creates the main descriptive text for the activity.
 * e.g., "You added 'twst'"
 * e.g., "You added Nur A. in 'KOKOLTRIP'"
 */
private fun formatDisplayText(activity: Activity, currentUserId: String): AnnotatedString {
    val actor = if (activity.actorUid == currentUserId) "You" else activity.actorName
    val activityType = try {
        ActivityType.valueOf(activity.activityType)
    } catch (e: IllegalArgumentException) {
        null // Handle unknown activity types
    }

    return buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(actor)
        }

        when (activityType) {
            ActivityType.GROUP_CREATED -> {
                append(" created the group ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("'${activity.displayText}'")
                }
            }
            ActivityType.GROUP_DELETED -> {
                append(" deleted the group ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("'${activity.displayText}'")
                }
            }
            ActivityType.EXPENSE_ADDED -> {
                append(" added ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("'${activity.displayText}'") // Expense description
                }
                if (activity.groupName != null) {
                    append(" in ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("'${activity.groupName}'")
                    }
                }
            }
            ActivityType.EXPENSE_UPDATED -> {
                append(" edited ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("'${activity.displayText}'") // Expense description
                }
                if (activity.groupName != null) {
                    append(" in ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("'${activity.groupName}'")
                    }
                }
            }
            ActivityType.EXPENSE_DELETED -> {
                append(" deleted ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("'${activity.displayText}'") // Expense description
                }
                if (activity.groupName != null) {
                    append(" in ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("'${activity.groupName}'")
                    }
                }
            }
            ActivityType.PAYMENT_MADE -> {
                // For PAYMENT_MADE, displayText is just the receiver's name
                // actor is the payer
                append(" paid ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${activity.displayText}")
                }
                if (activity.groupName != null) {
                    append(" in ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("'${activity.groupName}'")
                    }
                }
            }
            ActivityType.PAYMENT_UPDATED -> {
                // For PAYMENT_UPDATED, displayText is "payerName|receiverName"
                val names = activity.displayText?.split("|") ?: listOf("Someone", "Someone")
                val payerName = names.getOrNull(0) ?: "Someone"
                val receiverName = names.getOrNull(1) ?: "Someone"

                append(" updated a payment from ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(payerName)
                }
                append(" to ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(receiverName)
                }
                if (activity.groupName != null) {
                    append(" in ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("'${activity.groupName}'")
                    }
                }
            }
            ActivityType.PAYMENT_DELETED -> {
                // For PAYMENT_DELETED, displayText is "payerName|receiverName"
                val names = activity.displayText?.split("|") ?: listOf("Someone", "Someone")
                val payerName = names.getOrNull(0) ?: "Someone"
                val receiverName = names.getOrNull(1) ?: "Someone"

                append(" deleted a payment from ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(payerName)
                }
                append(" to ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(receiverName)
                }
                if (activity.groupName != null) {
                    append(" in ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("'${activity.groupName}'")
                    }
                }
            }
            ActivityType.MEMBER_ADDED -> {
                append(" added ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${activity.displayText}") // Added member's name
                }
                append(" to ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("'${activity.groupName}'")
                }
            }
            ActivityType.MEMBER_REMOVED -> {
                append(" removed ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${activity.displayText}") // Removed member's name
                }
                append(" from ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("'${activity.groupName}'")
                }
            }
            // Add other types as we implement them
            else -> {
                append(" did an action: ${activity.displayText ?: ""}")
            }
        }
    }
}

/**
 * Creates the personalized financial summary.
 * e.g., "You owe MYR333.33"
 * e.g., "You get back $25.00"
 * Returns AnnotatedString to support strikethrough for deleted/updated payments
 */
private fun formatFinancialSummary(activity: Activity, currentUserId: String): AnnotatedString {
    // Find the financial impact for the *current* user
    val impact = activity.financialImpacts?.get(currentUserId) ?: 0.0

    val type = try { ActivityType.valueOf(activity.activityType) } catch (e: Exception) { null }

    // Handle PAYMENT_UPDATED and PAYMENT_DELETED with strikethrough
    if (type == ActivityType.PAYMENT_UPDATED || type == ActivityType.PAYMENT_DELETED) {
        val amount = activity.totalAmount ?: 0.0
        return buildAnnotatedString {
            withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append("MYR%.2f".format(amount))
            }
        }
    }

    if (type == ActivityType.PAYMENT_MADE) {
        val amount = impact.absoluteValue
        val text = if (activity.actorUid == currentUserId) {
            // I was the payer (actor)
            "You paid MYR%.2f".format(amount)
        } else {
            // I was the receiver
            "You were paid MYR%.2f".format(amount)
        }
        return buildAnnotatedString { append(text) }
    }

    val textContent = when {
        impact < -0.01 -> "You owe MYR%.2f".format(impact.unaryMinus()) // Show positive value
        impact > 0.01 -> "You get back MYR%.2f".format(impact)
        else -> {
            // Don't show "you do not owe" for non-financial events
            val type = try { ActivityType.valueOf(activity.activityType) } catch (e: Exception) { null }
            if (type == ActivityType.GROUP_CREATED ||
                type == ActivityType.GROUP_DELETED ||
                type == ActivityType.MEMBER_ADDED ||
                type == ActivityType.MEMBER_REMOVED
            ) {
                "" // Return empty string for these types
            }
            // For expenses where user is not involved financially
            else "You do not owe anything"
        }
    }
    return buildAnnotatedString { append(textContent) }
}

/**
 * Returns the correct color for the financial summary.
 */
private fun getFinancialSummaryColor(activity: Activity, currentUserId: String): Color {
    val impact = activity.financialImpacts?.get(currentUserId) ?: 0.0
    val type = try { ActivityType.valueOf(activity.activityType) } catch (e: Exception) { null }
    if (type == ActivityType.PAYMENT_MADE) {
        return if (activity.actorUid == currentUserId) {
            // I was the payer, show as "negative" or neutral
            NegativeRed // Or Color.Gray, depending on preference
        } else {
            // I was the receiver, show as "positive"
            PositiveGreen
        }
    }
    return when {
        impact < -0.01 -> NegativeRed
        impact > 0.01 -> PositiveGreen
        else -> Color.Gray
    }
}

/**
 * Formats the timestamp into a user-friendly "time ago" string.
 * This is a basic implementation.
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance()
    time.timeInMillis = timestamp

    val todaySdf = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val otherSdf = SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault())

    // Check if it's today
    if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
        return "Today, ${todaySdf.format(Date(timestamp))}"
    }

    // Check if it was yesterday
    now.add(Calendar.DAY_OF_YEAR, -1)
    if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday, ${todaySdf.format(Date(timestamp))}"
    }

    // Older than yesterday
    return otherSdf.format(Date(timestamp))
}

/**
 * Formats timestamp to show just the time (used with date headers)
 */
private fun formatTimestampShort(timestamp: Long): String {
    val timeSdf = SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return timeSdf.format(Date(timestamp))
}