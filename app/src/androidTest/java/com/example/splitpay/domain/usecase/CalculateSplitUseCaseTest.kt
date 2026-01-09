package com.example.splitpay.domain.usecase

import com.example.splitpay.data.model.Participant
import com.example.splitpay.ui.expense.SplitType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Unit Tests for CalculateSplitUseCase
 *
 * Test Class Purpose:
 * This class contains comprehensive unit tests for the bill-splitting calculation
 * algorithm. It verifies that expenses are correctly divided among participants
 * using different split methods while maintaining precision to the cent.
 *
 * Functionality Under Test:
 * - Equal split calculation
 * - Unequal (exact amount) split calculation
 * - Percentage-based split calculation
 * - Share-based split calculation
 * - Rounding to 2 decimal places
 * - Rounding error adjustment algorithm
 *
 * Test Coverage:
 * 1. Happy Path Tests: Normal usage scenarios for each split type
 * 2. Edge Cases: Empty lists, zero amounts, single participants
 * 3. Precision Tests: Rounding behavior and total accuracy
 * 4. Invalid Input Tests: Null values, invalid strings, negative numbers
 * 5. Mixed Participant Tests: Checked and unchecked participants
 *
 * Key Testing Principles Applied:
 * - AAA Pattern (Arrange-Act-Assert)
 * - Each test focuses on one specific behavior
 * - Tests are independent and can run in any order
 * - Descriptive test names follow naming convention
 *
 * Critical Assertion:
 * Every test that involves splitting money verifies that the sum of all
 * owesAmount values equals the original total amount.
 *
 * Total Tests: 26
 * Author: [Your Name]
 * Course: [Course Code]
 * Date: [Date]
 */
class CalculateSplitUseCaseTest {

    // ========================================================================
    // EQUAL SPLIT TESTS (10 tests)
    // ========================================================================

    /**
     * Test: Equal split calculation with divisible amount for 2 checked participants
     *
     * Purpose: Verify that each participant receives an equal share with divisible amount.
     * This is the most basic and common scenario - splitting a bill evenly between two friends.
     *
     * Test Data:
     * - Input: Amount = 100.0, Participants = [Alice(checked), Bob(checked)], SplitType = EQUALLY
     * - Expected Output: Each person owes 50.0
     *
     * Verification:
     * - Each participant's owesAmount is 50.0
     * - Total owesAmount equals original amount (100.0)
     *
     * Edge Case/Scenario: Happy path - simplest equal split case
     */
    @Test
    fun testEqualSplit_TwoParticipants_DivisibleAmount() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(
                uid = "1",
                name = "Alice",
                isChecked = true,
                splitValue = "",
                owesAmount = 0.0
            ),
            Participant(
                uid = "2",
                name = "Bob",
                isChecked = true,
                splitValue = "",
                owesAmount = 0.0
            )
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(50.0, result[0].owesAmount)
        assertEquals(50.0, result[1].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Equal split calculation with indivisible amount for 3 checked participants
     *
     * Purpose: Verify that each participant receives an equal share except the first participant
     * will receive the rounding adjustment with indivisible amount. This tests the core
     * rounding correction algorithm.
     *
     * Test Data:
     * - Input: Amount = 100.0, Participants = [Alice(checked), Bob(checked), Charlie(checked)], SplitType = EQUALLY
     * - Expected Output: Alice: 33.34, Bob: 33.33, Charlie: 33.33
     *
     * Verification:
     * - Alice's owesAmount: 33.34 (receives +0.01 adjustment), Bob: 33.33, Charlie: 33.33
     * - Total owesAmount equals original amount (100.0)
     *
     * Edge Case/Scenario: Rounding adjustment - ensures total precision
     */
    @Test
    fun testEqualSplit_ThreeParticipants_IndivisibleAmount() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(
                uid = "1",
                name = "Alice",
                isChecked = true,
                splitValue = "",
                owesAmount = 0.0
            ),
            Participant(
                uid = "2",
                name = "Bob",
                isChecked = true,
                splitValue = "",
                owesAmount = 0.0
            ),
            Participant(
                uid = "3",
                name = "Charlie",
                isChecked = true,
                splitValue = "",
                owesAmount = 0.0
            )
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(33.34, result[0].owesAmount)
        assertEquals(33.33, result[1].owesAmount)
        assertEquals(33.33, result[2].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Equal split calculation with divisible amount for 5 checked participants
     *
     * Purpose: Verify that the algorithm works correctly with more participants.
     * Tests scalability of the equal split algorithm.
     *
     * Test Data:
     * - Input: Amount = 100.0, Participants = 5 people (all checked), SplitType = EQUALLY
     * - Expected Output: Each person owes 20.0
     *
     * Verification:
     * - Each participant's owesAmount is 20.0
     * - Total owesAmount equals original amount (100.0)
     *
     * Edge Case/Scenario: Multiple participants - tests algorithm scalability
     */
    @Test
    fun testEqualSplit_FiveParticipants_DivisibleAmount() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "4", name = "Danny", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "5", name = "Eric", isChecked = true, splitValue = "", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(20.0, result[0].owesAmount)
        assertEquals(20.0, result[1].owesAmount)
        assertEquals(20.0, result[2].owesAmount)
        assertEquals(20.0, result[3].owesAmount)
        assertEquals(20.0, result[4].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Equal split calculation with one checked participant
     *
     * Purpose: Verify that when only one participant is checked, they receive the full amount.
     * Real-world scenario: User is splitting a bill but only including themselves.
     *
     * Test Data:
     * - Input: Amount = 100.0, Participants = [Alice(checked)], SplitType = EQUALLY
     * - Expected Output: Alice owes 100.0
     *
     * Verification:
     * - Alice's owesAmount is 100.0
     * - Total owesAmount equals original amount (100.0)
     *
     * Edge Case/Scenario: Single participant - boundary case
     */
    @Test
    fun testEqualSplit_OneCheckedParticipant() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(
                uid = "1",
                name = "Alice",
                isChecked = true,
                splitValue = "",
                owesAmount = 0.0
            )
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(100.0, result[0].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Equal split calculation with one unchecked participant
     *
     * Purpose: Verify that an unchecked participant does not receive any amount.
     * Real-world scenario: User has a participant in the list but excludes them from this expense.
     *
     * Test Data:
     * - Input: Amount = 100.0, Participants = [Alice(unchecked)], SplitType = EQUALLY
     * - Expected Output: Alice owes 0.0
     *
     * Verification:
     * - Alice's owesAmount is 0.0
     * - Total owesAmount is 0.0 (not equal to original amount)
     *
     * Edge Case/Scenario: Unchecked participant - exclusion logic
     */
    @Test
    fun testEqualSplit_OneUncheckedParticipant() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(
                uid = "1",
                name = "Alice",
                isChecked = false,
                splitValue = "",
                owesAmount = 0.0
            )
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(0.0, result[0].owesAmount)

        // Verify total matches expected (0.0 since no one is checked)
        val total = result.sumOf { it.owesAmount }
        assertEquals(0.0, total)
    }

    /**
     * Test: Equal split with mixed checked and unchecked participants
     *
     * Purpose: Verify that only checked participants receive amounts and unchecked
     * participants receive 0.0. Tests the filtering logic.
     *
     * Test Data:
     * - Input: Amount = 100.0, Participants = [Alice(checked), Bob(unchecked)], SplitType = EQUALLY
     * - Expected Output: Alice owes 100.0, Bob owes 0.0
     *
     * Verification:
     * - Alice's owesAmount is 100.0, Bob's owesAmount is 0.0
     * - Total owesAmount equals original amount (100.0)
     *
     * Edge Case/Scenario: Mixed participation - partial exclusion
     */
    @Test
    fun testEqualSplit_OneCheckedOneUncheckedParticipants() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(
                uid = "1",
                name = "Alice",
                isChecked = true,
                splitValue = "",
                owesAmount = 0.0
            ),
            Participant(
                uid = "2",
                name = "Bob",
                isChecked = false,
                splitValue = "",
                owesAmount = 0.0
            )
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(100.0, result[0].owesAmount)
        assertEquals(0.0, result[1].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Equal split with zero amount
     *
     * Purpose: Verify the function handles zero amount gracefully without calculation errors.
     * Real-world scenario: User hasn't entered an amount yet or clears the amount field.
     *
     * Test Data:
     * - Input: Amount = 0.0, Participants = 3 people (all checked), SplitType = EQUALLY
     * - Expected Output: All participants owe 0.0
     *
     * Verification:
     * - All participants' owesAmount is 0.0
     * - Total owesAmount equals 0.0
     *
     * Edge Case/Scenario: Zero amount - defensive programming
     */
    @Test
    fun testEqualSplit_ZeroAmount() {
        // Arrange
        val amount = 0.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(0.0, result[0].owesAmount)
        assertEquals(0.0, result[1].owesAmount)
        assertEquals(0.0, result[2].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(0.0, total)
    }

    /**
     * Test: Equal split with different indivisible amount
     *
     * Purpose: Test another rounding scenario to verify rounding adjustment works consistently.
     * This uses a different amount that also requires rounding.
     *
     * Test Data:
     * - Input: Amount = 10.0, Participants = 3 people (all checked), SplitType = EQUALLY
     * - Expected Output: Alice: 3.34, Bob: 3.33, Charlie: 3.33
     *
     * Verification:
     * - First participant gets rounding adjustment
     * - Total equals original amount exactly
     *
     * Edge Case/Scenario: Alternative rounding test
     */
    @Test
    fun testEqualSplit_ThreeParticipants_SmallIndivisibleAmount() {
        // Arrange
        val amount = 10.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(3.34, result[0].owesAmount)
        assertEquals(3.33, result[1].owesAmount)
        assertEquals(3.33, result[2].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(10.0, total)
    }

    /**
     * Test: Equal split with very small amount
     *
     * Purpose: Test precision with very small amounts (cents).
     * Real-world scenario: Splitting small change or tips.
     *
     * Test Data:
     * - Input: Amount = 0.01 (1 cent), Participants = 2 people, SplitType = EQUALLY
     * - Expected Output: One gets 0.01, other gets 0.0 (can't split a cent)
     *
     * Verification:
     * - Rounding adjustment applies to maintain total
     * - Total equals 0.01
     *
     * Edge Case/Scenario: Minimum amount - precision boundary
     */
    @Test
    fun testEqualSplit_VerySmallAmount() {
        // Arrange
        val amount = 0.01
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        // 0.01 / 2 = 0.005, which rounds to 0.01 and 0.00 with adjustment
        assertEquals(0.01, result[0].owesAmount)
        assertEquals(0.0, result[1].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(0.01, total)
    }

    /**
     * Test: Equal split with four participants and indivisible amount
     *
     * Purpose: Test rounding with different participant count.
     * Ensures rounding algorithm works with various group sizes.
     *
     * Test Data:
     * - Input: Amount = 10.0, Participants = 4 people, SplitType = EQUALLY
     * - Expected Output: First person: 2.50, others: 2.50 each
     *
     * Verification:
     * - Total equals 10.0
     *
     * Edge Case/Scenario: Different group size for rounding
     */
    @Test
    fun testEqualSplit_FourParticipants_IndivisibleAmount() {
        // Arrange
        val amount = 10.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "4", name = "Danny", isChecked = true, splitValue = "", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        // 10 / 4 = 2.5 exactly, so all should be 2.5
        assertEquals(2.5, result[0].owesAmount)
        assertEquals(2.5, result[1].owesAmount)
        assertEquals(2.5, result[2].owesAmount)
        assertEquals(2.5, result[3].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(10.0, total)
    }

    // ========================================================================
    // UNEQUAL SPLIT TESTS (3 tests)
    // ========================================================================

    /**
     * Test: Unequal split calculation between 3 participants
     *
     * Purpose: Verify that amounts are correctly assigned based on exact amounts provided.
     * This is the basic test for manual amount entry.
     *
     * Test Data:
     * - Input: Amount = 100.0, Participants with specific splitValues
     *   Alice: 75.0, Bob: 20.0, Charlie: 5.0
     * - Expected Output: Alice owes 75.0, Bob owes 20.0, Charlie owes 5.0
     *
     * Verification:
     * - Each participant's owesAmount matches their splitValue
     * - Total owesAmount equals original amount (100.0)
     *
     * Edge Case/Scenario: Happy path for unequal split
     */
    @Test
    fun testUnequalSplit_ThreeParticipants_ValidAmounts() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "75.0", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "20.0", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "5.0", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.UNEQUALLY)

        // Assert
        assertEquals(75.0, result[0].owesAmount)
        assertEquals(20.0, result[1].owesAmount)
        assertEquals(5.0, result[2].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Unequal split with invalid splitValue (non-numeric string)
     *
     * Purpose: Verify the function handles invalid input gracefully.
     * Real-world scenario: User accidentally types letters instead of numbers.
     *
     * Test Data:
     * - Input: Amount = 100.0, Alice has "abc" as splitValue, Bob has "50"
     * - Expected Output: Alice owes 0.0 (invalid treated as 0), Bob owes 50.0
     *
     * Verification:
     * - Invalid splitValue is treated as 0.0
     * - Valid splitValue works correctly
     *
     * Edge Case/Scenario: Invalid input handling
     */
    @Test
    fun testUnequalSplit_InvalidSplitValue_TreatsAsZero() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "abc", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "50", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.UNEQUALLY)

        // Assert
        assertEquals(0.0, result[0].owesAmount)  // Invalid treated as 0
        assertEquals(50.0, result[1].owesAmount)

        // Verify total
        val total = result.sumOf { it.owesAmount }
        assertEquals(50.0, total)
    }

    /**
     * Test: Unequal split with empty splitValue
     *
     * Purpose: Verify empty splitValue is handled as 0.0.
     * Real-world scenario: User hasn't filled in amount for a participant yet.
     *
     * Test Data:
     * - Input: Alice has empty splitValue, Bob has "100"
     * - Expected Output: Alice: 0.0, Bob: 100.0
     *
     * Verification:
     * - Empty string treated as 0.0
     *
     * Edge Case/Scenario: Empty input handling
     */
    @Test
    fun testUnequalSplit_EmptySplitValue_TreatsAsZero() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "100", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.UNEQUALLY)

        // Assert
        assertEquals(0.0, result[0].owesAmount)
        assertEquals(100.0, result[1].owesAmount)

        // Verify total
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    // ========================================================================
    // PERCENTAGE SPLIT TESTS (5 tests)
    // ========================================================================

    /**
     * Test: Percentage split calculation between 3 participants
     *
     * Purpose: Verify that amounts are correctly calculated based on percentages provided.
     * This is the happy path for percentage-based splitting.
     *
     * Test Data:
     * - Input: Amount = 100.0, Percentages: Alice: 45%, Bob: 25.5%, Charlie: 29.5%
     * - Expected Output: Alice: 45.0, Bob: 25.5, Charlie: 29.5
     *
     * Verification:
     * - Each owesAmount = (percentage/100) * total amount
     * - Total equals 100.0
     *
     * Edge Case/Scenario: Valid percentages totaling 100%
     */
    @Test
    fun testPercentageSplit_ThreeParticipants_ValidPercentages() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "45.0", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "25.5", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "29.5", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.PERCENTAGES)

        // Assert
        assertEquals(45.0, result[0].owesAmount)
        assertEquals(25.5, result[1].owesAmount)
        assertEquals(29.5, result[2].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Percentage split with all zero percentages
     *
     * Purpose: Verify handling when all percentages are 0.
     * Real-world scenario: User hasn't entered percentages yet.
     *
     * Test Data:
     * - Input: Amount = 100.0, All percentages = 0.0
     * - Expected Output: All participants owe 0.0
     *
     * Verification:
     * - All owesAmount values are 0.0
     * - Total is 0.0
     *
     * Edge Case/Scenario: Zero percentage total
     */
    @Test
    fun testPercentageSplit_ZeroPercentages_ReturnsZero() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "0.0", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "0.0", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "0.0", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.PERCENTAGES)

        // Assert
        assertEquals(0.0, result[0].owesAmount)
        assertEquals(0.0, result[1].owesAmount)
        assertEquals(0.0, result[2].owesAmount)

        // Verify total
        val total = result.sumOf { it.owesAmount }
        assertEquals(0.0, total)
    }

    /**
     * Test: Percentage split with total exceeding 100%
     *
     * Purpose: Test behavior when percentages total more than 100%.
     * Real-world scenario: User error - enters 60% + 50% = 110%.
     * The algorithm doesn't validate this, so it will calculate based on entered values.
     *
     * Test Data:
     * - Input: Amount = 100.0, Alice: 60%, Bob: 50% (total 110%)
     * - Expected Output: Alice: 60.0, Bob: 50.0 (total: 110.0)
     *
     * Verification:
     * - Each participant gets their percentage amount
     * - Total will be 110.0 (not 100.0) - this is expected behavior
     *
     * Edge Case/Scenario: Invalid percentage total > 100%
     */
    @Test
    fun testPercentageSplit_TotalExceeds100Percent() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "60", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "50", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.PERCENTAGES)

        // Assert
        assertEquals(60.0, result[0].owesAmount)  // 60% of 100
        assertEquals(50.0, result[1].owesAmount)  // 50% of 100

        // Verify total - will be 110, NOT 100 (algorithm doesn't validate percentages)
        val total = result.sumOf { it.owesAmount }
        assertEquals(110.0, total)  // Expected: exceeds original amount
    }

    /**
     * Test: Percentage split with total less than 100%
     *
     * Purpose: Test behavior when percentages total less than 100%.
     * Real-world scenario: User enters 30% + 20% = 50% (forgot remaining 50%).
     *
     * Test Data:
     * - Input: Amount = 100.0, Alice: 30%, Bob: 20% (total 50%)
     * - Expected Output: Alice: 30.0, Bob: 20.0 (total: 50.0)
     *
     * Verification:
     * - Each participant gets their percentage amount
     * - Total will be 50.0 (less than 100.0)
     *
     * Edge Case/Scenario: Invalid percentage total < 100%
     */
    @Test
    fun testPercentageSplit_TotalLessThan100Percent() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "30", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "20", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.PERCENTAGES)

        // Assert
        assertEquals(30.0, result[0].owesAmount)
        assertEquals(20.0, result[1].owesAmount)

        // Verify total - will be 50, NOT 100
        val total = result.sumOf { it.owesAmount }
        assertEquals(50.0, total)  // Expected: less than original amount
    }

    /**
     * Test: Percentage split with decimal percentages
     *
     * Purpose: Verify decimal percentages work correctly.
     * Real-world scenario: User wants precise control like 33.33% each.
     *
     * Test Data:
     * - Input: Amount = 90.0, Each person: 33.33%
     * - Expected Output: Each owes ~30.0
     *
     * Verification:
     * - Decimal percentages are processed correctly
     *
     * Edge Case/Scenario: Decimal percentage values
     */
    @Test
    fun testPercentageSplit_DecimalPercentages() {
        // Arrange
        val amount = 90.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "33.33", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "33.33", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "33.34", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.PERCENTAGES)

        // Assert
        assertEquals(29.997, result[0].owesAmount, 0.001)  // 33.33% of 90
        assertEquals(29.997, result[1].owesAmount, 0.001)  // 33.33% of 90
        assertEquals(30.006, result[2].owesAmount, 0.001)  // 33.34% of 90

        // Verify total is close to 90.0
        val total = result.sumOf { it.owesAmount }
        assertEquals(90.0, total, 0.01)
    }

    // ========================================================================
    // SHARE SPLIT TESTS (4 tests)
    // ========================================================================

    /**
     * Test: Share split calculation between 3 participants
     *
     * Purpose: Verify that amounts are correctly calculated based on shares provided.
     * Formula: owes = (participant_shares / total_shares) * amount
     *
     * Test Data:
     * - Input: Amount = 100.0, Shares: Alice: 3, Bob: 3, Charlie: 4 (total: 10 shares)
     * - Expected Output: Alice: 30.0, Bob: 30.0, Charlie: 40.0
     *
     * Verification:
     * - Each owesAmount = (shares/total_shares) * amount
     * - Total equals 100.0
     *
     * Edge Case/Scenario: Basic share-based split
     */
    @Test
    fun testSharesSplit_ThreeParticipants_ValidShares() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "3", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "3", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "4", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.SHARES)

        // Assert
        assertEquals(30.0, result[0].owesAmount)  // 3/10 * 100
        assertEquals(30.0, result[1].owesAmount)  // 3/10 * 100
        assertEquals(40.0, result[2].owesAmount)  // 4/10 * 100

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Share split with all zero shares
     *
     * Purpose: Verify handling when all shares are 0.
     * Real-world scenario: User hasn't entered shares yet.
     *
     * Test Data:
     * - Input: Amount = 100.0, All shares = 0
     * - Expected Output: All participants owe 0.0
     *
     * Verification:
     * - All owesAmount values are 0.0
     * - Total is 0.0
     *
     * Edge Case/Scenario: Zero shares total
     */
    @Test
    fun testSharesSplit_ZeroShares_ReturnsZero() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "0.0", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "0.0", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "0.0", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.SHARES)

        // Assert
        assertEquals(0.0, result[0].owesAmount)
        assertEquals(0.0, result[1].owesAmount)
        assertEquals(0.0, result[2].owesAmount)

        // Verify total
        val total = result.sumOf { it.owesAmount }
        assertEquals(0.0, total)
    }

    /**
     * Test: Share split with decimal shares
     *
     * Purpose: Verify decimal shares are handled correctly.
     * Real-world scenario: More precise share ratios like 1.5 shares.
     *
     * Test Data:
     * - Input: Amount = 100.0, Alice: 1.5 shares, Bob: 2.5 shares (total: 4 shares)
     * - Expected Output: Alice: 37.5, Bob: 62.5
     *
     * Verification:
     * - Decimal shares work correctly
     * - Total equals 100.0
     *
     * Edge Case/Scenario: Decimal share values
     */
    @Test
    fun testSharesSplit_DecimalShares() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "1.5", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "2.5", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.SHARES)

        // Assert
        assertEquals(37.5, result[0].owesAmount)  // 1.5/4 * 100
        assertEquals(62.5, result[1].owesAmount)  // 2.5/4 * 100

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(100.0, total)
    }

    /**
     * Test: Share split with unequal share ratios
     *
     * Purpose: Test different share ratios (like 1:2:3).
     * Real-world scenario: Different consumption levels (adult vs child portions).
     *
     * Test Data:
     * - Input: Amount = 120.0, Shares: Alice: 1, Bob: 2, Charlie: 3 (total: 6)
     * - Expected Output: Alice: 20.0, Bob: 40.0, Charlie: 60.0
     *
     * Verification:
     * - Ratios are calculated correctly
     * - Total equals 120.0
     *
     * Edge Case/Scenario: Unequal share distribution
     */
    @Test
    fun testSharesSplit_UnequalRatios() {
        // Arrange
        val amount = 120.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "1", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "2", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "3", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.SHARES)

        // Assert
        assertEquals(20.0, result[0].owesAmount)  // 1/6 * 120
        assertEquals(40.0, result[1].owesAmount)  // 2/6 * 120
        assertEquals(60.0, result[2].owesAmount)  // 3/6 * 120

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(120.0, total)
    }

    // ========================================================================
    // EDGE CASE TESTS (4 tests)
    // ========================================================================

    /**
     * Test: Empty participant list
     *
     * Purpose: Verify the function handles edge case of no participants gracefully.
     * This tests defensive programming - function should not crash.
     *
     * Test Data:
     * - Input: Amount = 100.0, Participants = empty list
     * - Expected Output: Empty list or list with no amounts
     *
     * Verification:
     * - Function does not crash
     * - Returns empty list or all zeros
     *
     * Edge Case/Scenario: No participants - boundary condition
     */
    @Test
    fun testEqualSplit_EmptyParticipantList_HandlesGracefully() {
        // Arrange
        val amount = 100.0
        val participants = emptyList<Participant>()

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert - should not crash and return empty or all zeros
        assertTrue(result.isEmpty() || result.all { it.owesAmount == 0.0 })
    }

    /**
     * Test: Negative amount
     *
     * Purpose: Verify the function handles negative amounts correctly.
     * According to the code logic (amount <= 0), negative amounts should return 0.
     * Real-world scenario: Shouldn't happen in normal use, but defensive programming.
     *
     * Test Data:
     * - Input: Amount = -100.0, Participants = 2 people
     * - Expected Output: All participants owe 0.0
     *
     * Verification:
     * - Negative amount is treated as invalid (returns 0 for all)
     *
     * Edge Case/Scenario: Invalid negative amount
     */
    @Test
    fun testEqualSplit_NegativeAmount_ReturnsZero() {
        // Arrange
        val amount = -100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert - negative amount should result in 0 for all (amount <= 0 check)
        assertEquals(0.0, result[0].owesAmount)
        assertEquals(0.0, result[1].owesAmount)

        val total = result.sumOf { it.owesAmount }
        assertEquals(0.0, total)
    }

    /**
     * Test: All participants unchecked
     *
     * Purpose: Verify behavior when all participants are unchecked.
     * Real-world scenario: User unchecked everyone by accident.
     *
     * Test Data:
     * - Input: Amount = 100.0, All participants unchecked
     * - Expected Output: All owe 0.0
     *
     * Verification:
     * - All owesAmount values are 0.0
     * - Total is 0.0
     *
     * Edge Case/Scenario: No active participants
     */
    @Test
    fun testEqualSplit_AllParticipantsUnchecked_ReturnsZero() {
        // Arrange
        val amount = 100.0
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = false, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = false, splitValue = "", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = false, splitValue = "", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        assertEquals(0.0, result[0].owesAmount)
        assertEquals(0.0, result[1].owesAmount)
        assertEquals(0.0, result[2].owesAmount)

        val total = result.sumOf { it.owesAmount }
        assertEquals(0.0, total)
    }

    /**
     * Test: Large amount (stress test)
     *
     * Purpose: Verify the algorithm works with large monetary values.
     * Tests for potential overflow or precision issues.
     *
     * Test Data:
     * - Input: Amount = 999999.99, Participants = 3 people
     * - Expected Output: Proper distribution with rounding
     *
     * Verification:
     * - Large amounts are handled correctly
     * - No overflow errors
     * - Total equals original amount
     *
     * Edge Case/Scenario: Maximum practical amount
     */
    @Test
    fun testEqualSplit_LargeAmount() {
        // Arrange
        val amount = 999999.99
        val participants = listOf(
            Participant(uid = "1", name = "Alice", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "2", name = "Bob", isChecked = true, splitValue = "", owesAmount = 0.0),
            Participant(uid = "3", name = "Charlie", isChecked = true, splitValue = "", owesAmount = 0.0)
        )

        // Act
        val calculateSplitUseCase = CalculateSplitUseCase()
        val result = calculateSplitUseCase(amount, participants, SplitType.EQUALLY)

        // Assert
        // 999999.99 / 3 = 333333.33 each
        assertEquals(333333.33, result[0].owesAmount)
        assertEquals(333333.33, result[1].owesAmount)
        assertEquals(333333.33, result[2].owesAmount)

        // Verify total matches original amount
        val total = result.sumOf { it.owesAmount }
        assertEquals(999999.99, total, 0.01)  // Allow small delta for large numbers
    }
}