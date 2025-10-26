package com.example.splitpay.domain.usecase

import com.example.splitpay.data.model.Participant
import com.example.splitpay.ui.expense.SplitType
import kotlin.math.roundToInt

/**
 * A UseCase dedicated to calculating the 'owesAmount' for a list of participants
 * based on a total amount and a split type.
 */
class CalculateSplitUseCase {

    /**
     * Executes the split calculation.
     * @param amount The total amount of the expense.
     * @param participants The list of all participants (both checked and unchecked).
     * @param splitType The method of splitting (e.g., EQUALLY, PERCENTAGES).
     * @return A new list of participants with updated 'owesAmount'.
     */
    operator fun invoke(
        amount: Double,
        participants: List<Participant>,
        splitType: SplitType
    ): List<Participant> {
        val activeParticipants = participants.filter { it.isChecked }
        if (activeParticipants.isEmpty() || amount <= 0) {
            return participants.map { it.copy(owesAmount = 0.0) }
        }

        val calculatedParticipants = when (splitType) {
            SplitType.EQUALLY -> {
                val share = if (activeParticipants.isNotEmpty()) roundToCents(amount / activeParticipants.size) else 0.0
                participants.map { p ->
                    if (p.isChecked) p.copy(owesAmount = share) else p.copy(owesAmount = 0.0)
                }
            }
            SplitType.UNEQUALLY, SplitType.ADJUSTMENTS -> {
                participants.map { p ->
                    val owes = if (p.isChecked) roundToCents(p.splitValue.toDoubleOrNull() ?: 0.0) else 0.0
                    p.copy(owesAmount = owes)
                }
            }
            SplitType.PERCENTAGES -> {
                val totalPercent = activeParticipants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
                if (totalPercent == 0.0) return participants.map { it.copy(owesAmount = 0.0) }

                participants.map { p ->
                    if (p.isChecked) {
                        val percent = p.splitValue.toDoubleOrNull() ?: 0.0
                        val owes = roundToCents((percent / 100.0) * amount)
                        p.copy(owesAmount = owes)
                    } else {
                        p.copy(owesAmount = 0.0)
                    }
                }
            }
            SplitType.SHARES -> {
                val totalShares = activeParticipants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
                if (totalShares == 0.0) return participants.map { it.copy(owesAmount = 0.0) }

                val costPerShare = amount / totalShares // Keep precision

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

        // --- Adjustment for rounding errors ---
        val currentTotalOwed = calculatedParticipants.filter { it.isChecked }.sumOf { it.owesAmount }
        val difference = roundToCents(amount - currentTotalOwed)

        if (difference != 0.0 && activeParticipants.isNotEmpty()) {
            val firstActiveParticipantIndex = calculatedParticipants.indexOfFirst { it.isChecked }
            if (firstActiveParticipantIndex != -1) {
                val adjustedParticipant = calculatedParticipants[firstActiveParticipantIndex]
                val adjustedList = calculatedParticipants.toMutableList()
                adjustedList[firstActiveParticipantIndex] = adjustedParticipant.copy(
                    owesAmount = roundToCents(adjustedParticipant.owesAmount + difference)
                )
                return adjustedList.toList()
            }
        }
        return calculatedParticipants
    }

    // This helper function is now private to the UseCase
    private fun roundToCents(value: Double) = (value * 100.0).roundToInt() / 100.0
}