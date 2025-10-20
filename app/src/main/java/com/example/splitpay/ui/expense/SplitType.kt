package com.example.splitpay.ui.expense

// --- Data Models for Internal State ---
enum class SplitType(val label: String) {
    EQUALLY("Equally"),
    UNEQUALLY("Unequally (by amount)"),
    PERCENTAGES("By Percentages"),
    SHARES("By Shares"),
    ADJUSTMENTS("By Adjustments")
}