package com.example.splitpay.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun OverallBalanceHeader(
    totalBalance: Double,
    //topPadding: Dp // Receive top padding
) {
    val balanceText = when {
        totalBalance > 0.01 -> String.format(Locale.getDefault(), "Overall, you are owed MYR%.2f", totalBalance)
        totalBalance < -0.01 -> String.format(Locale.getDefault(), "Overall, you owe MYR%.2f", totalBalance.absoluteValue)
        else -> "You are all settled up!"
    }
    val balanceColor = when {
        totalBalance > 0.01 -> PositiveGreen
        totalBalance < -0.01 -> NegativeRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column( // Use Column to apply padding and then divider
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
//        Text(
//            text = "Overall, you are owed",
//            color = Color.Gray,
//            fontSize = 18.sp,
//        )
        Text(
            text = balanceText,
            color = balanceColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
    }
}