package com.example.splitpay.domain.usecase

import com.example.splitpay.data.model.Participant
import com.example.splitpay.ui.expense.SplitType
import kotlin.math.roundToInt

/**
 * Business logic for calculating expense splits among participants.
 *
 * This use case encapsulates the complex algorithm for determining how much each
 * participant owes in an expense based on different split types. It handles:
 * - Multiple split methods (equal, exact amounts, percentages, shares)
 * - Rounding to cents (2 decimal places)
 * - Rounding error adjustments to ensure totals match exactly
 */
class   CalculateSplitUseCase {

    /**
     * Calculates how much each participant owes based on the selected split type.
     *
     * **Algorithm Overview:**
     * 1. Filter only active (checked) participants
     * 2. Apply the appropriate split calculation method
     * 3. Round all amounts to 2 decimal places (cents)
     * 4. Adjust for any rounding errors by modifying the first participant's amount
     *
     * **Example:**
     * ```
     * // Equal split of $100 among 3 people
     * invoke(100.0, [Alice(checked), Bob(checked), Charlie(checked)], EQUALLY)
     * // Result: Alice: $33.34, Bob: $33.33, Charlie: $33.33 (total = $100.00)
     * ```
     *
     * @param amount The total expense amount to split
     * @param participants All participants (both checked and unchecked)
     * @param splitType The split calculation method to use
     * @return List of participants with calculated 'owesAmount' values
     */
    operator fun invoke(
        amount: Double,
        participants: List<Participant>,
        splitType: SplitType
    ): List<Participant> {
        // Only consider participants who are actively involved (checked)
        val activeParticipants = participants.filter { it.isChecked }

        // Early return for invalid inputs
        if (activeParticipants.isEmpty() || amount <= 0) {
            return participants.map { it.copy(owesAmount = 0.0) }
        }

        // Calculate splits based on the selected type
        val calculatedParticipants = when (splitType) {
            // Equal Split: Divide total equally among all active participants
            SplitType.EQUALLY -> {
                val share = if (activeParticipants.isNotEmpty()) roundToCents(amount / activeParticipants.size) else 0.0
                participants.map { p ->
                    if (p.isChecked) p.copy(owesAmount = share) else p.copy(owesAmount = 0.0)
                }
            }

            // Exact Amounts: User manually specifies exact amount each person owes
            SplitType.UNEQUALLY -> {
                participants.map { p ->
                    val owes = if (p.isChecked) roundToCents(p.splitValue.toDoubleOrNull() ?: 0.0) else 0.0
                    p.copy(owesAmount = owes)
                }
            }

            // Percentage Split: Each participant specifies their percentage (must total 100%)
            SplitType.PERCENTAGES -> {
                val totalPercent = activeParticipants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
                if (totalPercent == 0.0) return participants.map { it.copy(owesAmount = 0.0) }

                participants.map { p ->
                    if (p.isChecked) {
                        val percent = p.splitValue.toDoubleOrNull() ?: 0.0
                        // Convert percentage (0-100) to decimal fraction and multiply by total
                        val owes = roundToCents((percent / 100.0) * amount)
                        p.copy(owesAmount = owes)
                    } else {
                        p.copy(owesAmount = 0.0)
                    }
                }
            }

            // Share-based Split: Divide by ratio of shares
            // Example: A has 2 shares, B has 1 share for $90 → A owes $60, B owes $30
            SplitType.SHARES -> {
                val totalShares = activeParticipants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
                if (totalShares == 0.0) return participants.map { it.copy(owesAmount = 0.0) }

                val costPerShare = amount / totalShares

                participants.map { p ->
                    if (p.isChecked) {
                        val shares = p.splitValue.toDoubleOrNull() ?: 0.0
                        val owes = roundToCents(shares * costPerShare)
                        p.copy(owesAmount = owes)
                    } else {
                        p.copy(owesAmount = 0.0)
                    }
                }
            }
        }

        // Rounding Error Adjustment
        // Due to rounding each amount to 2 decimal places, the sum of all calculated amounts
        // might not exactly equal the total expense (could be off by 1-2 cents).
        // We adjust the first participant's amount by this difference to ensure precision.
        val currentTotalOwed = calculatedParticipants.filter { it.isChecked }.sumOf { it.owesAmount }
        val difference = roundToCents(amount - currentTotalOwed)

        if (difference != 0.0 && activeParticipants.isNotEmpty()) {
            val firstActiveParticipantIndex = calculatedParticipants.indexOfFirst { it.isChecked }
            if (firstActiveParticipantIndex != -1) {
                val adjustedParticipant = calculatedParticipants[firstActiveParticipantIndex]
                val adjustedList = calculatedParticipants.toMutableList()
                // Add or subtract the difference to/from the first participant
                adjustedList[firstActiveParticipantIndex] = adjustedParticipant.copy(
                    owesAmount = roundToCents(adjustedParticipant.owesAmount + difference)
                )
                return adjustedList.toList()
            }
        }
        return calculatedParticipants
    }

    /**
     * Rounds a monetary value to 2 decimal places (cents).
     *
     * Uses integer arithmetic to avoid floating-point precision issues:
     * - Multiplies by 100 to shift cents to whole number position
     * - Rounds to nearest integer
     * - Divides by 100 to shift back to decimal
     *
     * @param value The monetary value to round
     * @return The value rounded to 2 decimal places
     */
    private fun roundToCents(value: Double) = (value * 100.0).roundToInt() / 100.0
}