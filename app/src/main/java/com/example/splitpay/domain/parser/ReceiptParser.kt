package com.example.splitpay.domain.parser

import com.example.splitpay.data.model.ReceiptData
import com.example.splitpay.data.model.ReceiptLineItem

class ReceiptParser {

    fun parseReceipt(rawText: String): ReceiptData {
        val lines = rawText.split("\n").map { it.trim() }

        var total = 0.0
        var subtotal: Double? = null
        var tax: Double? = null
        var serviceCharge: Double? = null
        val items = mutableListOf<ReceiptLineItem>()

        // Regex patterns for Malaysian receipts
        val totalPattern = """(?i)(total|grand\s*total|amount)\s*:?\s*(?:RM|MYR)?\s*(\d+\.?\d*)""".toRegex()
        val subtotalPattern = """(?i)(sub\s*total|subtotal)\s*:?\s*(?:RM|MYR)?\s*(\d+\.?\d*)""".toRegex()
        val taxPattern = """(?i)(tax|gst|sst|service\s*tax)\s*:?\s*(?:RM|MYR)?\s*(\d+\.?\d*)""".toRegex()
        val servicePattern = """(?i)(service\s*charge|svc\s*chg)\s*:?\s*(?:RM|MYR)?\s*(\d+\.?\d*)""".toRegex()

        // Item pattern: "Description ... 12.50" or "Description 2 x 6.25"
        val itemPattern = """^(.+?)\s+(?:(\d+)\s*x\s*)?(?:RM|MYR)?\s*(\d+\.?\d*)$""".toRegex()

        for (line in lines) {
            when {
                // Extract total
                totalPattern.containsMatchIn(line) -> {
                    totalPattern.find(line)?.let { match ->
                        total = match.groupValues[2].toDoubleOrNull() ?: 0.0
                    }
                }

                // Extract subtotal
                subtotalPattern.containsMatchIn(line) -> {
                    subtotalPattern.find(line)?.let { match ->
                        subtotal = match.groupValues[2].toDoubleOrNull()
                    }
                }

                // Extract tax
                taxPattern.containsMatchIn(line) -> {
                    taxPattern.find(line)?.let { match ->
                        tax = match.groupValues[2].toDoubleOrNull()
                    }
                }

                // Extract service charge
                servicePattern.containsMatchIn(line) -> {
                    servicePattern.find(line)?.let { match ->
                        serviceCharge = match.groupValues[2].toDoubleOrNull()
                    }
                }

                // Extract line items
                itemPattern.containsMatchIn(line) -> {
                    itemPattern.find(line)?.let { match ->
                        val description = match.groupValues[1].trim()
                        val quantity = match.groupValues[2].toIntOrNull() ?: 1
                        val price = match.groupValues[3].toDoubleOrNull() ?: 0.0

                        // Filter out likely non-items (too short, contains keywords)
                        if (description.isNotEmpty() &&
                            description.length > 2 &&
                            !description.contains("total", ignoreCase = true) &&
                            !description.contains("tax", ignoreCase = true) &&
                            price > 0) {
                            items.add(
                                ReceiptLineItem(
                                    description = description,
                                    price = price,
                                    quantity = quantity
                                )
                            )
                        }
                    }
                }
            }
        }

        return ReceiptData(
            lineItems = items,
            subtotal = subtotal,
            tax = tax,
            serviceCharge = serviceCharge,
            total = total,
            rawText = rawText
        )
    }
}