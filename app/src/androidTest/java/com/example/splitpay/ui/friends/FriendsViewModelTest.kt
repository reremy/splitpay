package com.example.splitpay.ui.friends

import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.model.ExpensePayer
import com.example.splitpay.data.model.ExpenseParticipant
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Unit Tests for FriendsViewModel.calculateBalanceChangeForExpense()
 *
 * FUNCTION UNDER TEST:
 * calculateBalanceChangeForExpense() calculates the net balance change between two users
 * for a single expense. It handles both regular shared expenses and payment-type expenses
 * (settle-ups).
 *
 * CALCULATION LOGIC:
 * For regular expenses (expenseType = ExpenseType.EXPENSE):
 * - Calculates net contribution: (paid amount) - (owed amount) for each user
 * - Returns: (currentUser's contribution - friend's contribution) / number of participants
 * - Positive result = friend owes current user
 * - Negative result = current user owes friend
 *
 * For payment expenses (expenseType = ExpenseType.PAYMENT):
 * - Returns paid amount if current user paid (positive)
 * - Returns negative paid amount if friend paid
 * - Represents a direct money transfer/settle-up
 *
 * TEST COVERAGE:
 * This test suite covers:
 * - Basic 2-person and 3-person scenarios (8 tests)
 * - Payment-type expenses (4 tests)
 * - Unequal splits (6 tests)
 * - Edge cases (6 tests)
 * - Symmetry property (3 tests)
 * - Complex scenarios (1 test)
 * TOTAL: 28 comprehensive tests
 *
 * TESTING APPROACH:
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Delta of 0.01 for floating-point comparisons
 * - Tests both user perspectives for symmetry
 * - Organized into 6 logical sections
 *
 * AUTHOR: [Your Name]
 * DATE: January 2026
 * FYP: SplitPay Bill-Splitting Application
 * COURSE: [Your Course Code]
 * UNIVERSITY: [Your University]
 *
 * @see FriendsViewModel.calculateBalanceChangeForExpense
 */
class CalculateBalanceChangeForExpenseTest {

    // ========================================
    // TEST CLASS SETUP
    // ========================================

    /**
     * The ViewModel instance we're testing.
     * This is recreated before each test to ensure test isolation.
     */
    private lateinit var viewModel: FriendsViewModel

    /**
     * Setup method that runs before EACH test.
     *
     * Purpose: Creates a fresh FriendsViewModel instance for each test
     * to ensure tests don't interfere with each other (test isolation).
     *
     * The @Before annotation tells JUnit to run this method before every @Test method.
     */
    @Before
    fun setUp() {
        viewModel = FriendsViewModel()
    }

    // ========================================
    // SECTION 1: BASIC FUNCTIONALITY TESTS
    // ========================================
    // Tests: Normal scenarios with 2-3 people, regular expenses, equal splits
    // These are the foundation tests - if these fail, core functionality is broken

    /**
     * Test: Two-person expense with simple equal split where one person paid everything
     *
     * Scenario: Alice paid $100 for lunch, split equally with Bob.
     * Expected: Bob owes Alice $50
     */
    @Test
    fun testCalculateBalanceChangeForExpense_twoPersonEqualSplit_onePersonPaidAll() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_001",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 50.0),
                ExpenseParticipant(bobUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice paid $100, owes $50 → +$50 contribution
        // Bob paid $0, owes $50 → -$50 contribution
        // Difference = $100, divided by 2 participants = $50
        assertEquals(50.0, result, 0.01)
        assertTrue(result > 0)
    }

    /**
     * Test: Two-person expense where both people paid, equal split
     *
     * Scenario: Alice paid $60, Bob paid $40, both owe $50
     * Expected: Bob owes Alice $10 (Alice overpaid by $10, Bob underpaid by $10)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_twoPersonEqualSplit_bothPersonsPaid() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_002",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(
                ExpensePayer(aliceUid, 60.0),
                ExpensePayer(bobUid, 40.0)
            ),
            participants = listOf(
                ExpenseParticipant(aliceUid, 50.0),
                ExpenseParticipant(bobUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $60, owes $50 → +$10 contribution
        // Bob: paid $40, owes $50 → -$10 contribution
        // Difference = $20, divided by 2 = $10
        assertEquals(10.0, result, 0.01)
    }

    /**
     * Test: Three-person expense, equal split, one person paid all
     *
     * Scenario: Alice, Bob, and Charlie split $150 equally. Alice paid everything.
     * Expected: Bob owes Alice $50 (his share)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_threePersonEqualSplit_onePersonPaidAll() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val charlieUid = "user_charlie_789"

        val expense = Expense(
            id = "exp_003",
            totalAmount = 150.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 150.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 50.0),
                ExpenseParticipant(bobUid, 50.0),
                ExpenseParticipant(charlieUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $150, owes $50 → +$100 contribution
        // Bob: paid $0, owes $50 → -$50 contribution
        // Difference = $150, divided by 3 participants = $50
        assertEquals(50.0, result, 0.01)
    }

    /**
     * Test: Three-person expense, equal split, multiple payers
     *
     * Scenario: Alice paid $80, Bob paid $40, Charlie paid $30. Each owes $50.
     * Expected: Bob owes Alice $10 (Alice overpaid $30, Bob underpaid $10)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_threePersonEqualSplit_multiplePayers() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val charlieUid = "user_charlie_789"

        val expense = Expense(
            id = "exp_004",
            totalAmount = 150.0,
            createdByUid = aliceUid,
            paidBy = listOf(
                ExpensePayer(aliceUid, 80.0),
                ExpensePayer(bobUid, 40.0),
                ExpensePayer(charlieUid, 30.0)
            ),
            participants = listOf(
                ExpenseParticipant(aliceUid, 50.0),
                ExpenseParticipant(bobUid, 50.0),
                ExpenseParticipant(charlieUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $80, owes $50 → +$30 contribution
        // Bob: paid $40, owes $50 → -$10 contribution
        // Difference = $40, divided by 3 = $13.33
        assertEquals(13.33, result, 0.01)
    }

    /**
     * Test: Two-person expense where currentUser owes exactly zero
     *
     * Scenario: Alice paid $100, Alice owes $100, Bob owes $0
     * Expected: Balance change is 0 (Bob owes nothing)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_currentUserOwesExactlyZero() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_005",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 100.0),
                ExpenseParticipant(bobUid, 0.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $100, owes $100 → $0 contribution
        // Bob: paid $0, owes $0 → $0 contribution
        // Difference = $0
        assertEquals(0.0, result, 0.01)
    }

    /**
     * Test: Two-person expense where friend owes exactly zero
     *
     * Scenario: Bob paid $100, Alice owes $100, Bob owes $0
     * Expected: Balance change is 0 (Alice owes Bob, but in this relationship, it's 0)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_friendOwesExactlyZero() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_006",
            totalAmount = 100.0,
            createdByUid = bobUid,
            paidBy = listOf(ExpensePayer(bobUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 100.0),
                ExpenseParticipant(bobUid, 0.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $0, owes $100 → -$100 contribution
        // Bob: paid $100, owes $0 → +$100 contribution
        // Difference = -$200, divided by 2 = -$100
        assertEquals(-100.0, result, 0.01)
    }

    /**
     * Test: Four-person expense, equal split, one payer
     *
     * Scenario: Four friends split $200 equally. Alice paid all.
     * Expected: Bob owes Alice $50 (his 1/4 share)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_fourPersonEqualSplit_onePayer() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val charlieUid = "user_charlie_789"
        val dianaUid = "user_diana_101"

        val expense = Expense(
            id = "exp_007",
            totalAmount = 200.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 200.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 50.0),
                ExpenseParticipant(bobUid, 50.0),
                ExpenseParticipant(charlieUid, 50.0),
                ExpenseParticipant(dianaUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $200, owes $50 → +$150 contribution
        // Bob: paid $0, owes $50 → -$50 contribution
        // Difference = $200, divided by 4 = $50
        assertEquals(50.0, result, 0.01)
    }

    /**
     * Test: Two people, both owe zero (free/promotional item scenario)
     *
     * Scenario: Someone else paid for Alice and Bob (corporate expense, gift, etc.)
     * Expected: Balance change is 0
     */
    @Test
    fun testCalculateBalanceChangeForExpense_bothOweZero_freeItemScenario() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val companyUid = "company_corporate_999"

        val expense = Expense(
            id = "exp_008",
            totalAmount = 100.0,
            createdByUid = companyUid,
            paidBy = listOf(ExpensePayer(companyUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 0.0),
                ExpenseParticipant(bobUid, 0.0),
                ExpenseParticipant(companyUid, 100.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Both Alice and Bob: paid $0, owe $0 → $0 contribution each
        assertEquals(0.0, result, 0.01)
    }

    // ========================================
    // SECTION 2: PAYMENT TYPE EXPENSES (SETTLE-UP)
    // ========================================
    // Tests: ExpenseType.PAYMENT - direct money transfers between users

    /**
     * Test: Payment type - current user paid friend (settle-up)
     *
     * Scenario: Alice paid Bob $50 directly to settle a debt
     * Expected: +$50 (Bob owes Alice $50 now)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_paymentType_currentUserPaidFriend() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "payment_001",
            totalAmount = 50.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 50.0)),
            participants = listOf(ExpenseParticipant(bobUid, 50.0)),
            expenseType = ExpenseType.PAYMENT
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice paid Bob $50 → Bob owes Alice $50
        assertEquals(50.0, result, 0.01)
        assertTrue(result > 0)
    }

    /**
     * Test: Payment type - friend paid current user (settle-up)
     *
     * Scenario: Bob paid Alice $30 directly to settle a debt
     * Expected: -$30 (Alice owes Bob $30 less, or Bob is owed $30 less)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_paymentType_friendPaidCurrentUser() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "payment_002",
            totalAmount = 30.0,
            createdByUid = bobUid,
            paidBy = listOf(ExpensePayer(bobUid, 30.0)),
            participants = listOf(ExpenseParticipant(aliceUid, 30.0)),
            expenseType = ExpenseType.PAYMENT
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Bob paid Alice $30 → Alice owes Bob less (negative balance change)
        assertEquals(-30.0, result, 0.01)
        assertTrue(result < 0)
    }

    /**
     * Test: Payment type - zero amount payment (cancelled payment)
     *
     * Scenario: A payment was created but cancelled/zeroed out
     * Expected: 0 balance change
     */
    @Test
    fun testCalculateBalanceChangeForExpense_paymentType_zeroAmount() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "payment_003",
            totalAmount = 0.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 0.0)),
            participants = listOf(ExpenseParticipant(bobUid, 0.0)),
            expenseType = ExpenseType.PAYMENT
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        assertEquals(0.0, result, 0.01)
    }

    /**
     * Test: Payment type - large payment amount
     *
     * Scenario: Alice paid Bob $1,500 to settle multiple debts
     * Expected: +$1,500
     */
    @Test
    fun testCalculateBalanceChangeForExpense_paymentType_largeAmount() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "payment_004",
            totalAmount = 1500.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 1500.0)),
            participants = listOf(ExpenseParticipant(bobUid, 1500.0)),
            expenseType = ExpenseType.PAYMENT
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        assertEquals(1500.0, result, 0.01)
    }

    // ========================================
    // SECTION 3: UNEQUAL SPLITS
    // ========================================
    // Tests: Different owed amounts for different participants

    /**
     * Test: Unequal split - current user owes more (70/30 split)
     *
     * Scenario: $100 expense, Alice owes $70, Bob owes $30, Alice paid all
     * Expected: Bob owes Alice $30
     */
    @Test
    fun testCalculateBalanceChangeForExpense_unequalSplit_currentUserOwesMore() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_009",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 70.0),
                ExpenseParticipant(bobUid, 30.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $100, owes $70 → +$30 contribution
        // Bob: paid $0, owes $30 → -$30 contribution
        // Difference = $60, divided by 2 = $30
        assertEquals(30.0, result, 0.01)
    }

    /**
     * Test: Unequal split - friend owes more (30/70 split)
     *
     * Scenario: $100 expense, Alice owes $30, Bob owes $70, Alice paid all
     * Expected: Bob owes Alice $70
     */
    @Test
    fun testCalculateBalanceChangeForExpense_unequalSplit_friendOwesMore() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_010",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 30.0),
                ExpenseParticipant(bobUid, 70.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $100, owes $30 → +$70 contribution
        // Bob: paid $0, owes $70 → -$70 contribution
        // Difference = $140, divided by 2 = $70
        assertEquals(70.0, result, 0.01)
    }

    /**
     * Test: Unequal split - three people with different shares
     *
     * Scenario: $150 total, Alice owes $60, Bob owes $50, Charlie owes $40. Alice paid all.
     * Expected: Bob owes Alice proportionally
     */
    @Test
    fun testCalculateBalanceChangeForExpense_unequalSplit_threePeopleDifferentShares() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val charlieUid = "user_charlie_789"

        val expense = Expense(
            id = "exp_011",
            totalAmount = 150.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 150.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 60.0),
                ExpenseParticipant(bobUid, 50.0),
                ExpenseParticipant(charlieUid, 40.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $150, owes $60 → +$90 contribution
        // Bob: paid $0, owes $50 → -$50 contribution
        // Difference = $140, divided by 3 = $46.67
        assertEquals(46.67, result, 0.01)
    }

    /**
     * Test: Current user paid more than owed but less than total
     *
     * Scenario: $100 expense, Alice paid $60, owes $40, Bob paid $40, owes $60
     * Expected: Bob owes Alice $20
     */
    @Test
    fun testCalculateBalanceChangeForExpense_currentUserPaidMoreThanOwed() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_012",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(
                ExpensePayer(aliceUid, 60.0),
                ExpensePayer(bobUid, 40.0)
            ),
            participants = listOf(
                ExpenseParticipant(aliceUid, 40.0),
                ExpenseParticipant(bobUid, 60.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $60, owes $40 → +$20 contribution
        // Bob: paid $40, owes $60 → -$20 contribution
        // Difference = $40, divided by 2 = $20
        assertEquals(20.0, result, 0.01)
    }

    /**
     * Test: Friend paid more than owed
     *
     * Scenario: $100 expense, Bob paid $80, owes $30, Alice paid $20, owes $70
     * Expected: Alice owes Bob (negative balance)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_friendPaidMoreThanOwed() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_013",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(
                ExpensePayer(aliceUid, 20.0),
                ExpensePayer(bobUid, 80.0)
            ),
            participants = listOf(
                ExpenseParticipant(aliceUid, 70.0),
                ExpenseParticipant(bobUid, 30.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $20, owes $70 → -$50 contribution
        // Bob: paid $80, owes $30 → +$50 contribution
        // Difference = -$100, divided by 2 = -$50
        assertEquals(-50.0, result, 0.01)
        assertTrue(result < 0)
    }

    /**
     * Test: Current user underpaid (paid less than owed)
     *
     * Scenario: Alice paid $10 but owes $30, Bob paid $90, owes $70
     * Expected: Alice owes Bob (negative)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_currentUserUnderpaid() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_014",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(
                ExpensePayer(aliceUid, 10.0),
                ExpensePayer(bobUid, 90.0)
            ),
            participants = listOf(
                ExpenseParticipant(aliceUid, 30.0),
                ExpenseParticipant(bobUid, 70.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $10, owes $30 → -$20 contribution
        // Bob: paid $90, owes $70 → +$20 contribution
        // Difference = -$40, divided by 2 = -$20
        assertEquals(-20.0, result, 0.01)
    }

    // ========================================
    // SECTION 4: EDGE CASES
    // ========================================
    // Tests: Unusual but valid scenarios that must be handled gracefully

    /**
     * Test: Current user not in expense at all
     *
     * Scenario: Expense exists but Alice is neither payer nor participant
     * Expected: 0 balance change (no relationship to this expense)
     */
    @Test
    fun testCalculateBalanceChangeForExpense_currentUserNotInExpense_returnsZero() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val charlieUid = "user_charlie_789"

        val expense = Expense(
            id = "exp_015",
            totalAmount = 100.0,
            createdByUid = bobUid,
            paidBy = listOf(ExpensePayer(bobUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(bobUid, 50.0),
                ExpenseParticipant(charlieUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, charlieUid)

        // ASSERT
        // Alice is not involved in this expense at all
        assertEquals(0.0, result, 0.01)
    }

    /**
     * Test: Friend not in expense at all
     *
     * Scenario: Expense exists but Bob is neither payer nor participant
     * Expected: 0 balance change
     */
    @Test
    fun testCalculateBalanceChangeForExpense_friendNotInExpense_returnsZero() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val charlieUid = "user_charlie_789"

        val expense = Expense(
            id = "exp_016",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 50.0),
                ExpenseParticipant(charlieUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Bob is not involved in this expense
        assertEquals(0.0, result, 0.01)
    }

    /**
     * Test: Neither user in expense (both not involved)
     *
     * Scenario: Alice and Bob checking an expense they're not part of
     * Expected: 0 balance change
     */
    @Test
    fun testCalculateBalanceChangeForExpense_neitherUserInExpense_returnsZero() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val charlieUid = "user_charlie_789"
        val dianaUid = "user_diana_101"

        val expense = Expense(
            id = "exp_017",
            totalAmount = 100.0,
            createdByUid = charlieUid,
            paidBy = listOf(ExpensePayer(charlieUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(charlieUid, 50.0),
                ExpenseParticipant(dianaUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Neither Alice nor Bob are in this expense
        assertEquals(0.0, result, 0.01)
    }

    /**
     * Test: Empty paidBy list
     *
     * Scenario: Expense created but nobody has paid yet (shouldn't happen in real app)
     * Expected: 0 or handles gracefully
     */
    @Test
    fun testCalculateBalanceChangeForExpense_emptyPaidByList_returnsZero() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_018",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = emptyList(),  // No one has paid
            participants = listOf(
                ExpenseParticipant(aliceUid, 50.0),
                ExpenseParticipant(bobUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // No one paid, so balance should be based on who owes what
        // Alice owes $50, Bob owes $50, no one paid
        // Alice: $0 - $50 = -$50, Bob: $0 - $50 = -$50
        // Difference = 0
        assertEquals(0.0, result, 0.01)
    }

    /**
     * Test: Empty participants list
     *
     * Scenario: Expense created but no participants assigned (edge case)
     * Expected: Should not crash, uses coerceAtLeast(1.0) in formula
     */
    @Test
    fun testCalculateBalanceChangeForExpense_emptyParticipantsList_handlesGracefully() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_019",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 100.0)),
            participants = emptyList(),  // No participants
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Function should not crash due to coerceAtLeast(1.0) in the code
        // Since no participants, contributions are 0, result should be 0
        assertEquals(0.0, result, 0.01)
    }

    /**
     * Test: Very small amount (cents only)
     *
     * Scenario: $0.10 expense split between two people
     * Expected: Bob owes Alice $0.05
     */
    @Test
    fun testCalculateBalanceChangeForExpense_verySmallAmount_handlesCorrectly() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_020",
            totalAmount = 0.10,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 0.10)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 0.05),
                ExpenseParticipant(bobUid, 0.05)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        assertEquals(0.05, result, 0.01)
    }

    // ========================================
    // SECTION 5: SYMMETRY TESTS
    // ========================================
    // Tests: Verify that swapping users negates the result (mathematical property)

    /**
     * Test: Symmetry property - swapping users negates result (basic scenario)
     *
     * Scenario: Verify that calculateBalance(alice, bob) = -calculateBalance(bob, alice)
     * Expected: Results are negatives of each other
     */
    @Test
    fun testCalculateBalanceChangeForExpense_symmetry_swappingUsersNegatesResult() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_021",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 50.0),
                ExpenseParticipant(bobUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val alicePerspective = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)
        val bobPerspective = viewModel.calculateBalanceChangeForExpense(expense, bobUid, aliceUid)

        // ASSERT
        // From Alice's view: Bob owes her $50
        assertEquals(50.0, alicePerspective, 0.01)
        // From Bob's view: He owes Alice $50 (negative)
        assertEquals(-50.0, bobPerspective, 0.01)
        // Verify symmetry: they should be exact negatives
        assertEquals(alicePerspective, -bobPerspective, 0.01)
    }

    /**
     * Test: Symmetry for payment type
     *
     * Scenario: Alice paid Bob $30. Verify symmetry property holds.
     */
    @Test
    fun testCalculateBalanceChangeForExpense_symmetry_paymentType() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "payment_005",
            totalAmount = 30.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 30.0)),
            participants = listOf(ExpenseParticipant(bobUid, 30.0)),
            expenseType = ExpenseType.PAYMENT
        )

        // ACT
        val alicePerspective = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)
        val bobPerspective = viewModel.calculateBalanceChangeForExpense(expense, bobUid, aliceUid)

        // ASSERT
        assertEquals(30.0, alicePerspective, 0.01)
        assertEquals(-30.0, bobPerspective, 0.01)
        assertEquals(alicePerspective, -bobPerspective, 0.01)
    }

    /**
     * Test: Symmetry for unequal split
     *
     * Scenario: Unequal split where Alice owes 70%, Bob owes 30%
     * Verify symmetry still holds
     */
    @Test
    fun testCalculateBalanceChangeForExpense_symmetry_unequalSplit() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_022",
            totalAmount = 100.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 100.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 70.0),
                ExpenseParticipant(bobUid, 30.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val alicePerspective = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)
        val bobPerspective = viewModel.calculateBalanceChangeForExpense(expense, bobUid, aliceUid)

        // ASSERT
        // Verify they're negatives of each other
        assertEquals(alicePerspective, -bobPerspective, 0.01)
    }

    // ========================================
    // SECTION 6: COMPLEX SCENARIOS
    // ========================================
    // Tests: Comprehensive scenarios combining multiple edge cases

    /**
     * Test: Complex four-person scenario with multiple payers and unequal shares
     *
     * Scenario: Four friends, three paid different amounts, all owe different amounts
     * Tests the full complexity of the calculation logic
     */
    @Test
    fun testCalculateBalanceChangeForExpense_complexFourPersonScenario() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"
        val charlieUid = "user_charlie_789"
        val dianaUid = "user_diana_101"

        // Scenario: Dinner bill of $200
        // Alice paid $80, Charlie paid $70, Diana paid $50 (total = $200)
        // Alice owes $40, Bob owes $60, Charlie owes $50, Diana owes $50
        val expense = Expense(
            id = "exp_023",
            totalAmount = 200.0,
            createdByUid = aliceUid,
            paidBy = listOf(
                ExpensePayer(aliceUid, 80.0),
                ExpensePayer(charlieUid, 70.0),
                ExpensePayer(dianaUid, 50.0)
            ),
            participants = listOf(
                ExpenseParticipant(aliceUid, 40.0),
                ExpenseParticipant(bobUid, 60.0),
                ExpenseParticipant(charlieUid, 50.0),
                ExpenseParticipant(dianaUid, 50.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        // Alice: paid $80, owes $40 → +$40 contribution
        // Bob: paid $0, owes $60 → -$60 contribution
        // Difference = $100, divided by 4 participants = $25
        assertEquals(25.0, result, 0.01)
        assertTrue(result > 0)
    }

    /**
     * Test: Very large amount (stress test for numerical stability)
     *
     * Scenario: $1,000,000 expense split between two people
     * Expected: Should handle large numbers without precision loss
     */
    @Test
    fun testCalculateBalanceChangeForExpense_veryLargeAmount_numericallyStable() {
        // ARRANGE
        val aliceUid = "user_alice_123"
        val bobUid = "user_bob_456"

        val expense = Expense(
            id = "exp_024",
            totalAmount = 1000000.0,
            createdByUid = aliceUid,
            paidBy = listOf(ExpensePayer(aliceUid, 1000000.0)),
            participants = listOf(
                ExpenseParticipant(aliceUid, 500000.0),
                ExpenseParticipant(bobUid, 500000.0)
            ),
            expenseType = ExpenseType.EXPENSE
        )

        // ACT
        val result = viewModel.calculateBalanceChangeForExpense(expense, aliceUid, bobUid)

        // ASSERT
        assertEquals(500000.0, result, 0.01)
    }
}