package com.example.splitpay.integration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.ExpensePayer
import com.example.splitpay.data.model.ExpenseParticipant
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import kotlin.math.absoluteValue

/**
 * Integration Test Class: Expense Creation → Database Recording Flow
 *
 * Purpose:
 * Verifies that the complete expense creation and storage process works end-to-end,
 * including expense document creation in Firestore, split calculation accuracy,
 * and group expense list retrieval.
 *
 * Test Coverage:
 * - Expense document creation in Firestore /expenses collection
 * - Split calculation accuracy across different split types
 * - Group expense list queries
 * - Non-group (friend-to-friend) expense creation
 * - Data integrity validation (all fields saved correctly)
 * - Error handling (blank fields, invalid data)
 *
 * Testing Standards:
 * - AAA Pattern: Arrange-Act-Assert
 * - Naming: test[MethodName]_[Scenario]_[ExpectedBehavior]
 * - Documentation: Level 1 (Method comments) + Level 2 (Class overview)
 *
 * Dependencies:
 * - Cloud Firestore Emulator (port 8080)
 *
 * Setup Instructions:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Initialize emulators: firebase init emulators (select Firestore)
 * 3. Start emulators: firebase emulators:start
 * 4. Run tests: ./gradlew connectedAndroidTest
 *
 * Note: These tests use Firebase Emulator for isolation and speed.
 * Ensure emulator is running before executing tests.
 *
 * @author [Your Name]
 * @version 1.0
 * @since 2026-01-08
 */
@RunWith(AndroidJUnit4::class)
class ExpenseCreationIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // ═══════════════════════════════════════════════════════════
    // TEST CONFIGURATION
    // ═══════════════════════════════════════════════════════════

    /** System Under Test - ExpenseRepository instance */
    private lateinit var expenseRepository: ExpenseRepository

    /** ActivityRepository for verifying activity logs (optional) */
    private lateinit var activityRepository: ActivityRepository

    /** Cloud Firestore instance configured for testing */
    private lateinit var firestore: FirebaseFirestore

    /** Test expense ID for cleanup */
    private var testExpenseId: String? = null

    /** Test group ID for group expense tests */
    private lateinit var testGroupId: String

    /** Test user UIDs for participants and payers */
    private lateinit var testUser1Uid: String
    private lateinit var testUser2Uid: String
    private lateinit var testUser3Uid: String

    companion object {
        /** Firebase Emulator host (10.0.2.2 for Android emulator) */
        private const val EMULATOR_HOST = "10.0.2.2"

        /** Firestore emulator port */
        private const val FIRESTORE_PORT = 8080

        /** Whether to use emulators (set to false for real Firebase) */
        private const val USE_EMULATOR = true

        private lateinit var firestoreInstance: FirebaseFirestore

        /**
         * Runs ONCE before all tests in this class.
         * Configures Firebase emulator for the entire test suite.
         */
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            // Initialize Firebase if needed
            val context = ApplicationProvider.getApplicationContext<Context>()
            try {
                FirebaseApp.initializeApp(context)
            } catch (e: IllegalStateException) {
                // Already initialized - OK
            }

            // Get Firestore instance
            firestoreInstance = FirebaseFirestore.getInstance()

            // ✅ Configure emulator ONCE
            if (USE_EMULATOR) {
                try {
                    firestoreInstance.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
                    println("✅ Firebase Firestore emulator configured successfully")
                } catch (e: IllegalStateException) {
                    println("⚠️ Emulator already configured: ${e.message}")
                }
            }
        }

        /**
         * Runs ONCE after all tests complete.
         * Optional cleanup if needed.
         */
        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            println("✅ Test suite cleanup complete")
        }
    }

    /**
     * Runs before EACH test.
     * Creates fresh test data and repository instances.
     */
    @Before
    fun setUp() {
        // ✅ Create repository with existing Firestore instance
        firestore = firestoreInstance
        expenseRepository = ExpenseRepository(firestore)
        activityRepository = ActivityRepository(firestore)

        // Generate unique test data
        val timestamp = System.currentTimeMillis()
        testGroupId = "test-group-$timestamp"
        testUser1Uid = "test-user-1-$timestamp"
        testUser2Uid = "test-user-2-$timestamp"
        testUser3Uid = "test-user-3-$timestamp"

        testExpenseId = null
    }

    /**
     * Runs after EACH test.
     * Cleans up test-specific data from Firestore.
     */
    @After
    fun tearDown() = runTest {
        try {
            // Delete test expense document
            if (testExpenseId != null) {
                firestore.collection("expenses")
                    .document(testExpenseId!!)
                    .delete()
                    .await()
                println("🧹 Cleaned up test expense: $testExpenseId")
            }
        } catch (e: Exception) {
            println("⚠️ Cleanup warning: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CATEGORY 1: HAPPY PATH TESTS (PRIORITY: HIGH)
    // ═══════════════════════════════════════════════════════════

    /**
     * Level 1 Documentation: Happy Path - Valid Group Expense Creation
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Create a group expense with valid data and equal split
     * Expected Behavior: Expense saved successfully, Result.success returned
     *
     * AAA Pattern:
     * - ARRANGE: Create test expense with 3 participants, equal split
     * - ACT: Call expenseRepository.addExpense()
     * - ASSERT: Verify Result.isSuccess, expense ID returned, Firestore document exists
     *
     * Success Criteria:
     * ✓ addExpense() returns Result.success
     * ✓ Valid expense ID is returned
     * ✓ No exceptions thrown
     *
     * Test Data:
     * - Description: "Team Dinner"
     * - Amount: 90.00 (split equally among 3 users = 30.00 each)
     * - Split Type: EQUALLY
     * - Single Payer: User 1 pays full amount
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_ValidGroupExpense_CreatesExpenseSuccessfully() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Prepare test expense data
        // ─────────────────────────────────────────────────────
        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = "Team Dinner",
            totalAmount = 90.0,
            splitType = "EQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, 90.0)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 30.0, 0.0),
                ExpenseParticipant(testUser2Uid, 30.0, 0.0),
                ExpenseParticipant(testUser3Uid, 30.0, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        // ─────────────────────────────────────────────────────
        // ACT: Execute expense creation
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)

        // ⚠️ DEBUG: Print exception if it failed
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            println("❌ Add expense failed with exception:")
            println("   Type: ${exception?.javaClass?.simpleName}")
            println("   Message: ${exception?.message}")
            exception?.printStackTrace()
        }

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify results
        // ─────────────────────────────────────────────────────

        // Assert 1: Expense creation succeeded
        assertTrue(
            "Add expense should succeed with valid data",
            result.isSuccess
        )

        // Assert 2: Valid expense ID returned
        testExpenseId = result.getOrNull()
        assertNotNull(
            "Expense ID should be returned on success",
            testExpenseId
        )

        // Assert 3: Expense ID is not blank
        assertTrue(
            "Expense ID should not be blank",
            !testExpenseId.isNullOrBlank()
        )

        println("✅ Test expense created with ID: $testExpenseId")
    }

    /**
     * Level 1 Documentation: Happy Path - Firestore Document Creation
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Verify expense document is created in Firestore with correct data
     * Expected Behavior: Document exists at /expenses/{expenseId} with all correct fields
     *
     * AAA Pattern:
     * - ARRANGE: Create test expense
     * - ACT: Call addExpense() to save to Firestore
     * - ASSERT: Verify Firestore document exists and contains correct data
     *
     * Success Criteria:
     * ✓ Firestore document exists at /expenses/{expenseId}
     * ✓ Document can be deserialized to Expense object
     * ✓ All fields match input data
     * ✓ Description, amount, groupId, splitType match
     *
     * Firestore Verification:
     * 1. Document existence check
     * 2. Data deserialization (Expense.class.java)
     * 3. Field-by-field validation
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_ValidExpense_CreatesFirestoreDocumentCorrectly() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Prepare test expense
        // ─────────────────────────────────────────────────────
        val description = "Movie Night"
        val totalAmount = 60.0
        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = description,
            totalAmount = totalAmount,
            splitType = "EQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, totalAmount)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 20.0, 0.0),
                ExpenseParticipant(testUser2Uid, 20.0, 0.0),
                ExpenseParticipant(testUser3Uid, 20.0, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        // ─────────────────────────────────────────────────────
        // ACT: Save expense to Firestore
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)
        assertTrue("Expense creation should succeed", result.isSuccess)

        testExpenseId = result.getOrNull()
        assertNotNull("Expense ID should not be null", testExpenseId)

        println("🔍 Created expense with ID: $testExpenseId")
        println("🔍 Reading from Firestore...")

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify Firestore document
        // ─────────────────────────────────────────────────────

        // Assert 1: Document exists
        val expenseDoc = firestore.collection("expenses")
            .document(testExpenseId!!)
            .get()
            .await()

        println("🔍 Document exists: ${expenseDoc.exists()}")
        println("🔍 Document data: ${expenseDoc.data}")

        assertTrue(
            "Expense document should exist in Firestore at /expenses/{expenseId}",
            expenseDoc.exists()
        )

        // Assert 2: Document deserializable to Expense object
        val savedExpense = expenseDoc.toObject(Expense::class.java)
        assertNotNull(
            "Expense document should be deserializable to Expense object",
            savedExpense
        )

        // Assert 3: Description matches
        assertEquals(
            "Description should match input",
            description,
            savedExpense?.description
        )

        // Assert 4: Total amount matches
        savedExpense?.totalAmount?.let {
            assertEquals(
                "Total amount should match input",
                totalAmount,
                it,
                0.01
            )
        }

        // Assert 5: Group ID matches
        assertEquals(
            "Group ID should match input",
            testGroupId,
            savedExpense?.groupId
        )

        // Assert 6: Split type matches
        assertEquals(
            "Split type should match input",
            "EQUALLY",
            savedExpense?.splitType
        )

        println("✅ All Firestore document fields verified successfully")
    }

    /**
     * Level 1 Documentation: Happy Path - Group Expense List Retrieval
     *
     * Test Method: ExpenseRepository.getExpensesForGroups()
     * Scenario: Expense appears in group's expense list after creation
     * Expected Behavior: Expense is retrievable via group ID query
     *
     * AAA Pattern:
     * - ARRANGE: Create expense for specific group
     * - ACT: Query expenses by group ID
     * - ASSERT: Verify created expense appears in results
     *
     * Success Criteria:
     * ✓ Expense list is not empty
     * ✓ Created expense is found in list
     * ✓ Expense data matches original
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_GroupExpense_AppearsInGroupExpenseList() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create expense for group
        // ─────────────────────────────────────────────────────
        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = "Group Grocery Shopping",
            totalAmount = 150.0,
            splitType = "EQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, 150.0)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 50.0, 0.0),
                ExpenseParticipant(testUser2Uid, 50.0, 0.0),
                ExpenseParticipant(testUser3Uid, 50.0, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        val result = expenseRepository.addExpense(testExpense)
        testExpenseId = result.getOrNull()

        println("🔍 Created expense: $testExpenseId for group: $testGroupId")

        // ─────────────────────────────────────────────────────
        // ACT: Query expenses for group
        // ─────────────────────────────────────────────────────
        val groupExpenses = expenseRepository.getExpensesForGroups(listOf(testGroupId))

        println("🔍 Retrieved ${groupExpenses.size} expenses for group")

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify expense in list
        // ─────────────────────────────────────────────────────

        // Assert 1: Group expense list is not empty
        assertTrue(
            "Group expense list should not be empty",
            groupExpenses.isNotEmpty()
        )

        // Assert 2: Created expense is in the list
        val foundExpense = groupExpenses.find { it.id == testExpenseId }
        assertNotNull(
            "Created expense should be found in group expense list",
            foundExpense
        )

        // Assert 3: Expense data matches
        assertEquals(
            "Expense description should match",
            "Group Grocery Shopping",
            foundExpense?.description
        )

        foundExpense?.totalAmount?.let {
            assertEquals(
                "Expense amount should match",
                150.0,
                it,
                0.01
            )
        }

        println("✅ Expense successfully retrieved from group expense list")
    }

    /**
     * Level 1 Documentation: Happy Path - Non-Group (Friend-to-Friend) Expense
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Create expense between friends (no group)
     * Expected Behavior: Expense saved with groupId = "non_group"
     *
     * AAA Pattern:
     * - ARRANGE: Create expense with groupId = "non_group"
     * - ACT: Save expense
     * - ASSERT: Verify saved correctly with "non_group" groupId
     *
     * Success Criteria:
     * ✓ Expense creation succeeds
     * ✓ GroupId saved as "non_group"
     * ✓ Split calculations correct
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_NonGroupExpense_SavesWithNonGroupId() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create friend-to-friend expense
        // ─────────────────────────────────────────────────────
        val testExpense = createTestExpense(
            groupId = "non_group", // Friend-to-friend marker
            description = "Lunch Together",
            totalAmount = 40.0,
            splitType = "EQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, 40.0)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 20.0, 0.0),
                ExpenseParticipant(testUser2Uid, 20.0, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        // ─────────────────────────────────────────────────────
        // ACT: Save non-group expense
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)
        testExpenseId = result.getOrNull()

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify non-group expense saved correctly
        // ─────────────────────────────────────────────────────

        assertTrue("Non-group expense creation should succeed", result.isSuccess)

        val expenseDoc = firestore.collection("expenses")
            .document(testExpenseId!!)
            .get()
            .await()

        val savedExpense = expenseDoc.toObject(Expense::class.java)

        // Assert: GroupId is "non_group"
        assertEquals(
            "GroupId should be 'non_group' for friend-to-friend expenses",
            "non_group",
            savedExpense?.groupId
        )

        println("✅ Non-group expense saved correctly with groupId: ${savedExpense?.groupId}")
    }

    // ═══════════════════════════════════════════════════════════
    // CATEGORY 2: SPLIT CALCULATION TESTS (PRIORITY: HIGH)
    // ═══════════════════════════════════════════════════════════

    /**
     * Level 1 Documentation: Split Calculations - Equal Split Accuracy
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Verify equal split calculations are accurate and sum to total
     * Expected Behavior: Each participant owes equal share, total matches expense
     *
     * AAA Pattern:
     * - ARRANGE: Create expense with 3 participants, equal split
     * - ACT: Save expense
     * - ASSERT: Verify each participant owes 1/3 of total, sum = total
     *
     * Split Logic:
     * - Total: 90.00
     * - Participants: 3
     * - Expected per person: 30.00
     * - Sum of splits should equal 90.00
     *
     * Success Criteria:
     * ✓ Each participant's owesAmount = 30.00
     * ✓ Sum of all owesAmounts = 90.00 (within 0.01 tolerance)
     * ✓ Number of participants = 3
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_EqualSplit_CalculatesCorrectly() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create equal split expense
        // ─────────────────────────────────────────────────────
        val totalAmount = 90.0
        val expectedPerPerson = 30.0

        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = "Equal Split Test",
            totalAmount = totalAmount,
            splitType = "EQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, totalAmount)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, expectedPerPerson, 0.0),
                ExpenseParticipant(testUser2Uid, expectedPerPerson, 0.0),
                ExpenseParticipant(testUser3Uid, expectedPerPerson, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        // ─────────────────────────────────────────────────────
        // ACT: Save expense
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)
        testExpenseId = result.getOrNull()

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify split calculations
        // ─────────────────────────────────────────────────────

        val expenseDoc = firestore.collection("expenses")
            .document(testExpenseId!!)
            .get()
            .await()

        val savedExpense = expenseDoc.toObject(Expense::class.java)!!

        // Assert 1: Correct number of participants
        assertEquals(
            "Should have 3 participants",
            3,
            savedExpense.participants.size
        )

        // Assert 2: Each participant owes expected amount
        savedExpense.participants.forEach { participant ->
            assertEquals(
                "Each participant should owe $expectedPerPerson",
                expectedPerPerson,
                participant.owesAmount,
                0.01
            )
        }

        // Assert 3: Sum of splits equals total
        val totalOwed = savedExpense.participants.sumOf { it.owesAmount }
        assertEquals(
            "Sum of splits should equal total amount",
            totalAmount,
            totalOwed,
            0.01
        )

        println("✅ Equal split calculations verified: $totalOwed = $totalAmount")
    }

    /**
     * Level 1 Documentation: Split Calculations - Unequal Split Accuracy
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Verify unequal (custom amount) split calculations
     * Expected Behavior: Each participant owes specified custom amount
     *
     * AAA Pattern:
     * - ARRANGE: Create expense with unequal amounts (40, 35, 15)
     * - ACT: Save expense
     * - ASSERT: Verify each participant owes their specified amount
     *
     * Split Logic:
     * - Total: 90.00
     * - User 1: 40.00
     * - User 2: 35.00
     * - User 3: 15.00
     *
     * Success Criteria:
     * ✓ Participant amounts match specified values
     * ✓ Sum of amounts = 90.00
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_UnequalSplit_CalculatesCorrectly() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create unequal split expense
        // ─────────────────────────────────────────────────────
        val totalAmount = 90.0
        val user1Amount = 40.0
        val user2Amount = 35.0
        val user3Amount = 15.0

        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = "Unequal Split Test",
            totalAmount = totalAmount,
            splitType = "UNEQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, totalAmount)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, user1Amount, user1Amount),
                ExpenseParticipant(testUser2Uid, user2Amount, user2Amount),
                ExpenseParticipant(testUser3Uid, user3Amount, user3Amount)
            ),
            createdByUid = testUser1Uid
        )

        // ─────────────────────────────────────────────────────
        // ACT: Save expense
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)
        testExpenseId = result.getOrNull()

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify unequal split
        // ─────────────────────────────────────────────────────

        val expenseDoc = firestore.collection("expenses")
            .document(testExpenseId!!)
            .get()
            .await()

        val savedExpense = expenseDoc.toObject(Expense::class.java)!!

        // Assert 1: User 1 amount correct
        val user1Participant = savedExpense.participants.find { it.uid == testUser1Uid }
        user1Participant?.owesAmount?.let {
            assertEquals(
                "User 1 should owe $user1Amount",
                user1Amount,
                it,
                0.01
            )
        }

        // Assert 2: User 2 amount correct
        val user2Participant = savedExpense.participants.find { it.uid == testUser2Uid }
        user2Participant?.owesAmount?.let {
            assertEquals(
                "User 2 should owe $user2Amount",
                user2Amount,
                it,
                0.01
            )
        }

        // Assert 3: User 3 amount correct
        val user3Participant = savedExpense.participants.find { it.uid == testUser3Uid }
        user3Participant?.owesAmount?.let {
            assertEquals(
                "User 3 should owe $user3Amount",
                user3Amount,
                it,
                0.01
            )
        }

        // Assert 4: Sum equals total
        val totalOwed = savedExpense.participants.sumOf { it.owesAmount }
        assertEquals(
            "Sum of unequal splits should equal total",
            totalAmount,
            totalOwed,
            0.01
        )

        println("✅ Unequal split calculations verified: $user1Amount + $user2Amount + $user3Amount = $totalAmount")
    }

    /**
     * Level 1 Documentation: Split Calculations - Multiple Payers
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Expense split among multiple payers
     * Expected Behavior: Sum of payer amounts equals total expense
     *
     * AAA Pattern:
     * - ARRANGE: Create expense with 2 payers (50 + 40 = 90)
     * - ACT: Save expense
     * - ASSERT: Verify payer amounts saved correctly and sum to total
     *
     * Payment Logic:
     * - Total: 90.00
     * - Payer 1: 50.00
     * - Payer 2: 40.00
     *
     * Success Criteria:
     * ✓ Number of payers = 2
     * ✓ Payer amounts match input
     * ✓ Sum of payments = 90.00
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_MultiplePayers_AmountsMatchTotal() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create expense with multiple payers
        // ─────────────────────────────────────────────────────
        val totalAmount = 90.0
        val payer1Amount = 50.0
        val payer2Amount = 40.0

        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = "Multiple Payers Test",
            totalAmount = totalAmount,
            splitType = "EQUALLY",
            payers = listOf(
                ExpensePayer(testUser1Uid, payer1Amount),
                ExpensePayer(testUser2Uid, payer2Amount)
            ),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 30.0, 0.0),
                ExpenseParticipant(testUser2Uid, 30.0, 0.0),
                ExpenseParticipant(testUser3Uid, 30.0, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        // ─────────────────────────────────────────────────────
        // ACT: Save expense
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)
        testExpenseId = result.getOrNull()

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify multiple payers
        // ─────────────────────────────────────────────────────

        val expenseDoc = firestore.collection("expenses")
            .document(testExpenseId!!)
            .get()
            .await()

        val savedExpense = expenseDoc.toObject(Expense::class.java)!!

        // Assert 1: Correct number of payers
        assertEquals(
            "Should have 2 payers",
            2,
            savedExpense.paidBy.size
        )

        // Assert 2: Payer 1 amount correct
        val payer1 = savedExpense.paidBy.find { it.uid == testUser1Uid }
        payer1?.paidAmount?.let {
            assertEquals(
                "Payer 1 should have paid $payer1Amount",
                payer1Amount,
                it,
                0.01
            )
        }

        // Assert 3: Payer 2 amount correct
        val payer2 = savedExpense.paidBy.find { it.uid == testUser2Uid }
        payer2?.paidAmount?.let {
            assertEquals(
                "Payer 2 should have paid $payer2Amount",
                payer2Amount,
                it,
                0.01
            )
        }

        // Assert 4: Sum of payments equals total
        val totalPaid = savedExpense.paidBy.sumOf { it.paidAmount }
        assertEquals(
            "Sum of payer amounts should equal total expense",
            totalAmount,
            totalPaid,
            0.01
        )

        println("✅ Multiple payers verified: $payer1Amount + $payer2Amount = $totalAmount")
    }

    // ═══════════════════════════════════════════════════════════
    // CATEGORY 3: DATA INTEGRITY TESTS (PRIORITY: HIGH)
    // ═══════════════════════════════════════════════════════════

    /**
     * Level 1 Documentation: Data Integrity - All Fields Validation
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Comprehensive validation of all Expense model fields
     * Expected Behavior: All fields saved correctly including optional ones
     *
     * AAA Pattern:
     * - ARRANGE: Create expense with all fields populated
     * - ACT: Save expense
     * - ASSERT: Validate every field (mandatory + optional + defaults)
     *
     * Fields Validated:
     * Mandatory:
     *   - id, groupId, description, totalAmount, createdByUid, splitType
     * Optional:
     *   - memo, category, imageUrl
     * Lists:
     *   - paidBy, participants
     * Timestamps:
     *   - date (recent timestamp)
     *
     * Success Criteria:
     * ✓ All mandatory fields populated
     * ✓ All optional fields have correct values
     * ✓ date timestamp is within last 10 seconds
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_AllFields_SavedCorrectly() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create expense with all fields
        // ─────────────────────────────────────────────────────
        val description = "Complete Expense Test"
        val totalAmount = 120.0
        val memo = "Test memo with all fields"
        val category = "food"
        val imageUrl = "https://example.com/receipt.jpg"
        val currentTime = System.currentTimeMillis()

        val testExpense = Expense(
            id = "", // Will be auto-generated
            groupId = testGroupId,
            description = description,
            totalAmount = totalAmount,
            createdByUid = testUser1Uid,
            date = currentTime,
            splitType = "EQUALLY",
            paidBy = listOf(ExpensePayer(testUser1Uid, totalAmount)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 40.0, 0.0),
                ExpenseParticipant(testUser2Uid, 40.0, 0.0),
                ExpenseParticipant(testUser3Uid, 40.0, 0.0)
            ),
            memo = memo,
            imageUrl = imageUrl,
            category = category
        )

        // ─────────────────────────────────────────────────────
        // ACT: Save expense
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)
        testExpenseId = result.getOrNull()

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify ALL fields
        // ─────────────────────────────────────────────────────

        val expenseDoc = firestore.collection("expenses")
            .document(testExpenseId!!)
            .get()
            .await()

        val savedExpense = expenseDoc.toObject(Expense::class.java)!!

        // ═══ Mandatory Fields ═══

        // Assert 1: ID matches
        assertEquals("ID should match", testExpenseId, savedExpense.id)

        // Assert 2: Group ID matches
        assertEquals("Group ID should match", testGroupId, savedExpense.groupId)

        // Assert 3: Description matches
        assertEquals("Description should match", description, savedExpense.description)

        // Assert 4: Total amount matches
        assertEquals("Total amount should match", totalAmount, savedExpense.totalAmount, 0.01)

        // Assert 5: Creator UID matches
        assertEquals("Creator UID should match", testUser1Uid, savedExpense.createdByUid)

        // Assert 6: Split type matches
        assertEquals("Split type should match", "EQUALLY", savedExpense.splitType)

        // ═══ Optional Fields ═══

        // Assert 7: Memo matches
        assertEquals("Memo should match", memo, savedExpense.memo)

        // Assert 8: Category matches
        assertEquals("Category should match", category, savedExpense.category)

        // Assert 9: Image URL matches
        assertEquals("Image URL should match", imageUrl, savedExpense.imageUrl)

        // ═══ Lists ═══

        // Assert 10: Payers list has 1 entry
        assertEquals("Should have 1 payer", 1, savedExpense.paidBy.size)

        // Assert 11: Participants list has 3 entries
        assertEquals("Should have 3 participants", 3, savedExpense.participants.size)

        // ═══ Timestamp ═══

        // Assert 12: Date timestamp is recent
        val timeDifference = (System.currentTimeMillis() - savedExpense.date).absoluteValue
        assertTrue(
            "Date should be within last 10 seconds (was ${timeDifference}ms ago)",
            timeDifference < 10_000
        )

        println("✅ All expense fields verified successfully")
    }

    /**
     * Level 1 Documentation: Data Integrity - Expense ID Consistency
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Verify expense ID returned matches Firestore document ID
     * Expected Behavior: Perfect consistency between return value and document
     *
     * AAA Pattern:
     * - ARRANGE: Create test expense
     * - ACT: Save expense and get returned ID
     * - ASSERT: Verify returned ID equals Firestore document ID and expense.id field
     *
     * Why This Matters:
     * - ID is the primary key for expense lookups
     * - Mismatch would break expense retrieval and updates
     * - Critical for data integrity
     *
     * Success Criteria:
     * ✓ addExpense() returns valid ID
     * ✓ Firestore document exists at that ID
     * ✓ expense.id field equals document ID
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_ExpenseId_ConsistencyAcrossSystemsk() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE & ACT: Create and save expense
        // ─────────────────────────────────────────────────────
        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = "ID Consistency Test",
            totalAmount = 50.0,
            splitType = "EQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, 50.0)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 25.0, 0.0),
                ExpenseParticipant(testUser2Uid, 25.0, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        val result = expenseRepository.addExpense(testExpense)

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify ID consistency
        // ─────────────────────────────────────────────────────

        // Assert 1: Valid ID returned
        val returnedId = result.getOrNull()
        assertNotNull("Expense ID should be returned", returnedId)
        testExpenseId = returnedId

        // Assert 2: Firestore document exists at that ID
        val expenseDoc = firestore.collection("expenses")
            .document(returnedId!!)
            .get()
            .await()

        assertTrue("Document should exist at returned ID", expenseDoc.exists())

        // Assert 3: expense.id field matches document ID
        val savedExpense = expenseDoc.toObject(Expense::class.java)
        assertEquals(
            "Expense.id field MUST match Firestore document ID for data integrity",
            returnedId,
            savedExpense?.id
        )

        println("✅ Expense ID consistency verified: $returnedId")
    }

    // ═══════════════════════════════════════════════════════════
    // CATEGORY 4: ERROR HANDLING TESTS (PRIORITY: MEDIUM)
    // ═══════════════════════════════════════════════════════════

    /**
     * Level 1 Documentation: Error Handling - Blank Description
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Attempt to create expense with blank description
     * Expected Behavior: Repository accepts (validation is UI/ViewModel responsibility)
     *
     * Note: ExpenseRepository is a data layer and does not validate business rules.
     * Validation should happen in ViewModel. Repository will save blank descriptions.
     *
     * AAA Pattern:
     * - ARRANGE: Create expense with empty description
     * - ACT: Attempt to save
     * - ASSERT: Verify it succeeds (repository doesn't validate)
     *
     * Success Criteria:
     * ✓ addExpense() succeeds despite blank description
     * ✓ Document saved with blank description field
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_BlankDescription_RepositoryAccepts() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create expense with blank description
        // ─────────────────────────────────────────────────────
        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = "", // Blank description
            totalAmount = 50.0,
            splitType = "EQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, 50.0)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 50.0, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        // ─────────────────────────────────────────────────────
        // ACT: Attempt to save
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)

        // ─────────────────────────────────────────────────────
        // ASSERT: Repository accepts blank description
        // ─────────────────────────────────────────────────────

        // Assert: Expense creation succeeded (repository doesn't validate)
        assertTrue(
            "Repository should accept blank description (validation is ViewModel responsibility)",
            result.isSuccess
        )

        testExpenseId = result.getOrNull()

        val expenseDoc = firestore.collection("expenses")
            .document(testExpenseId!!)
            .get()
            .await()

        val savedExpense = expenseDoc.toObject(Expense::class.java)

        assertEquals(
            "Description should be blank as saved",
            "",
            savedExpense?.description
        )

        println("✅ Repository correctly accepts blank description (validation is UI layer responsibility)")
    }

    /**
     * Level 1 Documentation: Error Handling - Zero Amount
     *
     * Test Method: ExpenseRepository.addExpense()
     * Scenario: Attempt to create expense with zero amount
     * Expected Behavior: Repository accepts (validation is UI/ViewModel responsibility)
     *
     * Note: ExpenseRepository doesn't validate business rules.
     * ViewModel should prevent zero-amount expenses.
     *
     * AAA Pattern:
     * - ARRANGE: Create expense with 0.0 amount
     * - ACT: Attempt to save
     * - ASSERT: Verify it succeeds (repository doesn't validate)
     *
     * Success Criteria:
     * ✓ addExpense() succeeds with zero amount
     * ✓ Document saved with totalAmount = 0.0
     *
     * Dependencies: Firestore Emulator
     * Execution Time: ~1-2 seconds
     */
    @Test
    fun testAddExpense_ZeroAmount_RepositoryAccepts() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create expense with zero amount
        // ─────────────────────────────────────────────────────
        val testExpense = createTestExpense(
            groupId = testGroupId,
            description = "Zero Amount Test",
            totalAmount = 0.0, // Zero amount
            splitType = "EQUALLY",
            payers = listOf(ExpensePayer(testUser1Uid, 0.0)),
            participants = listOf(
                ExpenseParticipant(testUser1Uid, 0.0, 0.0)
            ),
            createdByUid = testUser1Uid
        )

        // ─────────────────────────────────────────────────────
        // ACT: Attempt to save
        // ─────────────────────────────────────────────────────
        val result = expenseRepository.addExpense(testExpense)

        // ─────────────────────────────────────────────────────
        // ASSERT: Repository accepts zero amount
        // ─────────────────────────────────────────────────────

        assertTrue(
            "Repository should accept zero amount (validation is ViewModel responsibility)",
            result.isSuccess
        )

        testExpenseId = result.getOrNull()

        val expenseDoc = firestore.collection("expenses")
            .document(testExpenseId!!)
            .get()
            .await()

        val savedExpense = expenseDoc.toObject(Expense::class.java)

        savedExpense?.totalAmount?.let {
            assertEquals(
                "Total amount should be 0.0 as saved",
                0.0,
                it,
                0.01
            )
        }

        println("✅ Repository correctly accepts zero amount (validation is UI layer responsibility)")
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Helper function to create test expense objects with common defaults.
     *
     * Simplifies test setup by providing sensible defaults for all required fields
     * while allowing specific fields to be customized per test.
     *
     * @param groupId Group ID for the expense (or "non_group")
     * @param description Expense description
     * @param totalAmount Total expense amount
     * @param splitType Split calculation method ("EQUALLY", "UNEQUALLY", etc.)
     * @param payers List of users who paid for the expense
     * @param participants List of users who owe money
     * @param createdByUid UID of user who created the expense
     * @return Configured Expense object ready for testing
     */
    private fun createTestExpense(
        groupId: String,
        description: String,
        totalAmount: Double,
        splitType: String,
        payers: List<ExpensePayer>,
        participants: List<ExpenseParticipant>,
        createdByUid: String,
        memo: String = "",
        category: String = "misc",
        imageUrl: String = ""
    ): Expense {
        return Expense(
            //id = "", // Will be auto-generated by repository
            groupId = groupId,
            description = description,
            totalAmount = totalAmount,
            createdByUid = createdByUid,
            date = System.currentTimeMillis(),
            splitType = splitType,
            paidBy = payers,
            participants = participants,
            memo = memo,
            imageUrl = imageUrl,
            category = category
        )
    }
}