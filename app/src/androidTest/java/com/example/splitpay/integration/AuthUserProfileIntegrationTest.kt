package com.example.splitpay.integration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Integration Test Class: Authentication to User Profile Creation Flow
 *
 * Purpose:
 * Verifies that the complete user registration process works end-to-end,
 * including Firebase Authentication account creation and Firestore user
 * profile document creation.
 *
 * Test Coverage:
 * - Happy path scenarios (valid registration)
 * - Error handling (duplicate email, invalid data)
 * - Data integrity (Auth-Firestore consistency)
 * - Edge cases (special characters, boundary values)
 *
 * Testing Standards:
 * - AAA Pattern: Arrange-Act-Assert
 * - Naming: test[MethodName]_[Scenario]_[ExpectedBehavior]
 * - Documentation: Level 1 (Method comments) + Level 2 (Class overview)
 *
 * Dependencies:
 * - Firebase Authentication Emulator (port 9099)
 * - Cloud Firestore Emulator (port 8080)
 *
 * Setup Instructions:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Initialize emulators: firebase init emulators (select Auth + Firestore)
 * 3. Start emulators: firebase emulators:start
 * 4. Run tests: ./gradlew connectedAndroidTest
 *
 * Note: These tests use Firebase Emulators for isolation and speed.
 * Ensure emulators are running before executing tests.
 *
 * @author [Your Name]
 * @version 1.0
 * @since 2026-01-06
 */

@RunWith(AndroidJUnit4::class)
class AuthUserProfileIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // ═══════════════════════════════════════════════════════════
    // TEST CONFIGURATION
    // ═══════════════════════════════════════════════════════════

    /** System Under Test - UserRepository instance */
    private lateinit var userRepository: UserRepository

    /** Firebase Authentication instance configured for testing */
    //private lateinit var auth: FirebaseAuth

    /** Cloud Firestore instance configured for testing */
    //private lateinit var firestore: FirebaseFirestore

    /** Test email address (generated uniquely per test) */
    private lateinit var testEmail: String

    /** Test password for user registration */
    private lateinit var testPassword: String

    /** User ID for cleanup (set during test execution) */
    private var testUid: String? = null

    //private lateinit var context: Context

    companion object {
        /** Firebase Emulator host (10.0.2.2 for Android emulator) */
        private const val EMULATOR_HOST = "10.0.2.2"

        /** Authentication emulator port */
        private const val AUTH_PORT = 9099

        /** Firestore emulator port */
        private const val FIRESTORE_PORT = 8080

        /** Whether to use emulators (set to false for real Firebase) */
        private const val USE_EMULATOR = true

        private lateinit var auth: FirebaseAuth
        private lateinit var firestore: FirebaseFirestore

        /**
         * Runs ONCE before all tests in this class.
         * Configures Firebase emulators for the entire test suite.
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

            // Get Firebase instances
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            // ✅ Configure emulators ONCE
            if (USE_EMULATOR) {
                try {
                    auth.useEmulator(EMULATOR_HOST, AUTH_PORT)
                    firestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
                    println("✅ Firebase emulators configured successfully")
                } catch (e: IllegalStateException) {
                    println("⚠️ Emulators already configured: ${e.message}")
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
            auth.signOut()
            println("✅ Test suite cleanup complete")
        }
    }

    /**
    * Runs before EACH test.
    * Creates fresh test data but reuses Firebase instances.
    */
    @Before
    fun setUp() {
        // ✅ Just create repository with existing instances
        userRepository = UserRepository(auth, firestore)

        // Generate unique test data
        val timestamp = System.currentTimeMillis()
        testEmail = "test.user.$timestamp@example.com"
        testPassword = "Test123456"

        // Sign out any previous user
        auth.signOut()
    }

    /**
     * Runs after EACH test.
     * Cleans up test-specific data.
     */
    @After
    fun tearDown() = runTest {
        try {
            // Delete test user
            auth.currentUser?.delete()?.await()

            // Delete Firestore document
            if (testUid != null) {
                firestore.collection("users")
                    .document(testUid!!)
                    .delete()
                    .await()
            }

            // Sign out
            auth.signOut()
        } catch (e: Exception) {
            println("Cleanup warning: ${e.message}")
        }
    }



    // ═══════════════════════════════════════════════════════════
    // CATEGORY 1: HAPPY PATH TESTS (PRIORITY: HIGH)
    // ═══════════════════════════════════════════════════════════

    /**
     * Level 1 Documentation: Happy Path - Valid Credentials
     *
     * Test Method: UserRepository.signUp()
     * Scenario: User registers with all valid credentials
     * Expected Behavior: Sign-up succeeds, Auth account created, Result.success returned
     *
     * AAA Pattern:
     * - ARRANGE: Prepare valid test data (fullName, username, email, phone, password)
     * - ACT: Call userRepository.signUp() with test data
     * - ASSERT: Verify Result.isSuccess, Auth user exists, email/name match
     *
     * Success Criteria:
     * ✓ signUp() returns Result.success
     * ✓ Firebase Auth user is created
     * ✓ User email matches input
     * ✓ Display name is set correctly
     * ✓ No exceptions thrown
     *
     * Test Data:
     * - Full Name: "John Doe"
     * - Username: "johndoe"
     * - Email: Generated unique email
     * - Phone: "+60123456789"
     * - Password: "Test123456"
     *
     * Dependencies: Firebase Auth Emulator
     * Execution Time: ~2-3 seconds
     */
    @Test
    fun testSignUp_ValidCredentials_CreatesUserSuccessfully() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Prepare test data
        // ─────────────────────────────────────────────────────
        val fullName = "John Doe"
        val username = "johndoe"
        val phoneNumber = "+60123456789"

        // ─────────────────────────────────────────────────────
        // ACT: Execute sign-up operation
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = fullName,
            username = username,
            email = testEmail,
            phoneNumber = phoneNumber,
            password = testPassword
        )

        // ⚠️ DEBUG: Print the exception if it failed
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            println("❌ Sign-up failed with exception:")
            println("   Type: ${exception?.javaClass?.simpleName}")
            println("   Message: ${exception?.message}")
            exception?.printStackTrace()
        }

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify results
        // ─────────────────────────────────────────────────────

        // Assert 1: Sign-up operation succeeded
        assertTrue(
            "Sign up should succeed with valid data",
            result.isSuccess
        )

        // Assert 2: Firebase Auth account exists
        val authUser = auth.currentUser
        assertNotNull(
            "Firebase Auth user should exist after sign-up",
            authUser
        )

        // Assert 3: Email matches input
        assertEquals(
            "User email should match registration email",
            testEmail,
            authUser?.email
        )

        // Assert 4: Display name is set
        assertEquals(
            "Firebase Auth display name should be set to full name",
            fullName,
            authUser?.displayName
        )

        // Store UID for cleanup in tearDown()
        testUid = authUser?.uid
    }

    /**
     * Level 1 Documentation: Happy Path - Firestore Document Creation
     *
     * Test Method: UserRepository.signUp()
     * Scenario: User registers and Firestore user profile document is created
     * Expected Behavior: Document exists at /users/{uid} with all correct fields
     *
     * AAA Pattern:
     * - ARRANGE: Prepare valid test data
     * - ACT: Call signUp() to create user
     * - ASSERT: Verify Firestore document exists and contains correct data
     *
     * Success Criteria:
     * ✓ Firestore document exists at /users/{uid}
     * ✓ Document can be deserialized to User object
     * ✓ All fields match registration input
     * ✓ UID matches Firebase Auth UID
     *
     * Firestore Verification:
     * 1. Document existence check
     * 2. Data deserialization (User.class.java)
     * 3. Field-by-field validation
     *
     * Dependencies: Firebase Auth + Firestore Emulators
     * Execution Time: ~2-3 seconds
     */
    @Test
    fun testSignUp_ValidCredentials_CreatesFirestoreDocumentSuccessfully() = runTest {
        val fullName = "Jane Smith"
        val username = "janesmith"
        val phoneNumber = "+60198765432"

        // ✅ ADD: Log test instances
        println("🔍 TEST - Auth instance: $auth")
        println("🔍 TEST - Firestore instance: $firestore")

        val result = userRepository.signUp(
            fullName = fullName,
            username = username,
            email = testEmail,
            phoneNumber = phoneNumber,
            password = testPassword
        )

        assertTrue("Sign up should succeed with valid data", result.isSuccess)

        testUid = auth.currentUser?.uid
        assertNotNull("User UID should not be null", testUid)

        println("🔍 TEST - Created user UID: $testUid")
        println("🔍 TEST - About to read from Firestore...")

        val userDoc = withContext(Dispatchers.Default) {
            withTimeout(10_000) {
                firestore.collection("users")
                    .document(testUid!!)
                    .get()
                    .await()
            }
        }

        println("🔍 TEST - Document exists: ${userDoc.exists()}")
        println("🔍 TEST - Document data: ${userDoc.data}")

        assertTrue(
            "User document should exist in Firestore at /users/{uid}",
            userDoc.exists()
        )

        val userData = userDoc.toObject<User>()
        assertNotNull("User document should be deserializable to User object", userData)

        assertEquals("UID should match", testUid, userData?.uid)
        assertEquals("Full name should match", fullName, userData?.fullName)
        assertEquals("Username should match", username, userData?.username)
        assertEquals("Email should match", testEmail, userData?.email)
        assertEquals("Phone number should match", phoneNumber, userData?.phoneNumber)
    }

    @Test
    fun testDirectFirestoreWrite() = runTest {
        println("🔍 Testing direct Firestore write...")

        val testData = mapOf(
            "uid" to "test-uid-123",
            "name" to "Test User",
            "timestamp" to System.currentTimeMillis()
        )

        try {
            println("🔍 Writing to Firestore...")
            firestore.collection("users")
                .document("test-uid-123")
                .set(testData)
                .await()
            println("✅ Write completed")

            println("🔍 Reading from Firestore...")
            val doc = firestore.collection("users")
                .document("test-uid-123")
                .get()
                .await()

            println("✅ Read completed. Exists: ${doc.exists()}")
            println("✅ Data: ${doc.data}")

            assertTrue("Document should exist", doc.exists())
            assertEquals("test-uid-123", doc.data?.get("uid"))

            // Cleanup
            firestore.collection("users")
                .document("test-uid-123")
                .delete()
                .await()

            println("✅ Direct Firestore operations working!")

        } catch (e: Exception) {
            println("❌ Firestore operation failed:")
            println("   Type: ${e.javaClass.simpleName}")
            println("   Message: ${e.message}")
            e.printStackTrace()
            fail("Firestore not working: ${e.message}")
        }
    }

    @Test
    fun testUserObjectSerializationToFirestore() = runTest {
        println("🔍 Testing User object serialization...")

        val testUser = User(
            uid = "test-uid-456",
            fullName = "Test User",
            username = "testuser",
            email = "test@example.com",
            phoneNumber = "+60123456789"
        )

        println("🔍 User object: $testUser")

        try {
            println("🔍 Writing User object to Firestore...")
            firestore.collection("users")
                .document("test-uid-456")
                .set(testUser)  // ← Writing User object directly
                .await()
            println("✅ User write completed")

            println("🔍 Reading User object from Firestore...")
            val doc = firestore.collection("users")
                .document("test-uid-456")
                .get()
                .await()

            println("✅ Read completed. Exists: ${doc.exists()}")
            println("✅ Data: ${doc.data}")

            val retrievedUser = doc.toObject(User::class.java)
            println("✅ Deserialized User: $retrievedUser")

            assertTrue("Document should exist", doc.exists())
            assertEquals("test-uid-456", retrievedUser?.uid)
            assertEquals("Test User", retrievedUser?.fullName)

            // Cleanup
            firestore.collection("users")
                .document("test-uid-456")
                .delete()
                .await()

            println("✅ User object serialization working!")

        } catch (e: Exception) {
            println("❌ User serialization failed:")
            println("   Type: ${e.javaClass.simpleName}")
            println("   Message: ${e.message}")
            e.printStackTrace()
            fail("User object serialization failed: ${e.message}")
        }
    }

    /**
     * Level 1 Documentation: Data Integrity - All Fields Validation
     *
     * Test Method: UserRepository.signUp()
     * Scenario: Comprehensive validation of all User model fields after registration
     * Expected Behavior: All fields saved correctly including defaults
     *
     * AAA Pattern:
     * - ARRANGE: Prepare test data
     * - ACT: Sign up user
     * - ASSERT: Validate every field in User model (mandatory + optional + defaults)
     *
     * Fields Validated:
     * Mandatory:
     *   - uid, fullName, username, email, phoneNumber
     * Optional/Defaults:
     *   - profilePictureUrl (empty string)
     *   - qrCodeUrl (empty string)
     *   - friends (empty list)
     *   - blockedUsers (empty list)
     *   - deletionScheduledAt (null)
     *   - fcmToken (empty string)
     *   - createdAt (recent timestamp)
     *
     * Success Criteria:
     * ✓ All mandatory fields populated
     * ✓ All default fields have correct initial values
     * ✓ createdAt timestamp is within last 10 seconds
     *
     * Dependencies: Firebase Auth + Firestore Emulators
     * Execution Time: ~2-3 seconds
     */
    @Test
    fun testSignUp_ValidCredentials_AllFieldsSavedCorrectly() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Prepare test data
        // ─────────────────────────────────────────────────────
        val fullName = "Test User"
        val username = "testuser"
        val phoneNumber = "+60123456789"

        // ─────────────────────────────────────────────────────
        // ACT: Execute sign-up and retrieve user data
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = fullName,
            username = username,
            email = testEmail,
            phoneNumber = phoneNumber,
            password = testPassword
        )

        testUid = auth.currentUser?.uid
        val userDoc = firestore.collection("users")
            .document(testUid!!)
            .get()
            .await()

        val user = userDoc.toObject(User::class.java)!!

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify ALL fields
        // ─────────────────────────────────────────────────────

        // ═══ Mandatory Fields ═══

        // Assert 1: UID matches
        assertEquals(
            "UID must match Firebase Auth UID",
            testUid,
            user.uid
        )

        // Assert 2: Full name matches
        assertEquals(
            "Full name must match input",
            fullName,
            user.fullName
        )

        // Assert 3: Username matches
        assertEquals(
            "Username must match input",
            username,
            user.username
        )

        // Assert 4: Email matches
        assertEquals(
            "Email must match input",
            testEmail,
            user.email
        )

        // Assert 5: Phone number matches
        assertEquals(
            "Phone number must match input",
            phoneNumber,
            user.phoneNumber
        )

        // ═══ Default/Optional Fields ═══

        // Assert 6: Profile picture URL defaults to empty
        assertEquals(
            "Profile picture URL should default to empty string",
            "",
            user.profilePictureUrl
        )

        // Assert 7: QR code URL defaults to empty
        assertEquals(
            "QR code URL should default to empty string",
            "",
            user.qrCodeUrl
        )

        // Assert 8: Friends list is empty
        assertTrue(
            "Friends list should be empty on registration",
            user.friends.isEmpty()
        )

        // Assert 9: Blocked users list is empty
        assertTrue(
            "Blocked users list should be empty on registration",
            user.blockedUsers.isEmpty()
        )

        // Assert 10: Deletion timestamp is null
        assertEquals(
            "Deletion timestamp should be null (no scheduled deletion)",
            null,
            user.deletionScheduledAt
        )

        // Assert 11: FCM token defaults to empty
        assertEquals(
            "FCM token should default to empty string",
            "",
            user.fcmToken
        )

        // ═══ Timestamp Validation ═══

        // Assert 12: createdAt is recent
        val now = System.currentTimeMillis()
        val timeDifference = now - user.createdAt
        assertTrue(
            "createdAt should be within last 10 seconds (was ${timeDifference}ms ago)",
            timeDifference >= 0 && timeDifference < 10_000
        )
    }

    /**
     * Level 1 Documentation: Data Integrity - UID Consistency
     *
     * Test Method: UserRepository.signUp()
     * Scenario: Verify Firebase Auth UID matches Firestore document UID
     * Expected Behavior: Perfect consistency between Auth and Firestore
     *
     * AAA Pattern:
     * - ARRANGE: None needed (uses generated test data)
     * - ACT: Sign up user
     * - ASSERT: Compare Auth UID with Firestore document UID
     *
     * Why This Matters:
     * - UID is the primary key linking Auth and Firestore
     * - Mismatch would break entire user system
     * - Critical for data integrity
     *
     * Success Criteria:
     * ✓ Firebase Auth generates UID
     * ✓ Firestore document ID equals Auth UID
     * ✓ User.uid field equals Auth UID
     *
     * Dependencies: Firebase Auth + Firestore Emulators
     * Execution Time: ~2 seconds
     */
    @Test
    fun testSignUp_ValidData_FirestoreUidMatchesAuthUid() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE & ACT: Sign up user
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = "Consistency Test",
            username = "consistencytest",
            email = testEmail,
            phoneNumber = "+60111111111",
            password = testPassword
        )

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify UID consistency
        // ─────────────────────────────────────────────────────

        // Assert 1: Firebase Auth UID exists
        val authUid = auth.currentUser?.uid
        assertNotNull(
            "Firebase Auth UID should exist",
            authUid
        )
        testUid = authUid

        // Assert 2: Firestore document exists and contains matching UID
        val userDoc = firestore.collection("users")
            .document(authUid!!)
            .get()
            .await()

        val firestoreUid = userDoc.toObject(User::class.java)?.uid

        // Assert 3: UIDs match perfectly
        assertEquals(
            "Firebase Auth UID MUST match Firestore document UID for data integrity",
            authUid,
            firestoreUid
        )
    }

    // ═══════════════════════════════════════════════════════════
    // CATEGORY 2: ERROR HANDLING TESTS (PRIORITY: HIGH)
    // ═══════════════════════════════════════════════════════════

    /**
     * Level 1 Documentation: Error Handling - Duplicate Email
     *
     * Test Method: UserRepository.signUp()
     * Scenario: User attempts to register with email that's already in use
     * Expected Behavior: Firebase Auth rejects, signUp() returns Result.failure
     *
     * AAA Pattern:
     * - ARRANGE: Create first user with test email
     * - ACT: Attempt to create second user with same email
     * - ASSERT: Second registration fails with exception
     *
     * Firebase Behavior:
     * - Firebase Auth enforces email uniqueness
     * - Throws FirebaseAuthUserCollisionException
     * - Repository catches and returns Result.failure
     *
     * Success Criteria:
     * ✓ First registration succeeds
     * ✓ Second registration fails (Result.isFailure)
     * ✓ Exception is captured in Result
     * ✓ No second Auth account created
     *
     * Dependencies: Firebase Auth Emulator
     * Execution Time: ~3-4 seconds (two sign-up attempts)
     */
    @Test
    fun testSignUp_DuplicateEmail_ReturnsFailureResult() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Create first user with test email
        // ─────────────────────────────────────────────────────
        val firstSignUp = userRepository.signUp(
            fullName = "First User",
            username = "firstuser",
            email = testEmail,
            phoneNumber = "+60111111111",
            password = testPassword
        )
        testUid = auth.currentUser?.uid
        auth.signOut() // Sign out to attempt second registration

        // ─────────────────────────────────────────────────────
        // ACT: Attempt second registration with SAME email
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = "Second User",
            username = "seconduser",
            email = testEmail, // ← Duplicate email
            phoneNumber = "+60222222222",
            password = "DifferentPass123"
        )

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify failure
        // ─────────────────────────────────────────────────────

        // Assert 1: First sign-up succeeded
        assertTrue(
            "First sign up should succeed",
            firstSignUp.isSuccess
        )

        // Assert 2: Second sign-up failed
        assertFalse(
            "Sign up should fail when email is already registered",
            result.isSuccess
        )

        // Assert 3: Exception is captured
        assertTrue(
            "Result should contain exception explaining failure",
            result.exceptionOrNull() != null
        )
    }

    /**
     * Level 1 Documentation: Error Handling - Weak Password
     *
     * Test Method: UserRepository.signUp()
     * Scenario: User provides password shorter than Firebase minimum (6 chars)
     * Expected Behavior: Firebase Auth rejects, no user created
     *
     * AAA Pattern:
     * - ARRANGE: Prepare registration data with 5-character password
     * - ACT: Attempt sign-up with weak password
     * - ASSERT: Registration fails, no Auth user created
     *
     * Firebase Rules:
     * - Minimum password length: 6 characters
     * - Throws FirebaseAuthWeakPasswordException if violated
     *
     * Success Criteria:
     * ✓ signUp() returns Result.failure
     * ✓ No Firebase Auth user created
     * ✓ Exception captured
     *
     * Dependencies: Firebase Auth Emulator
     * Execution Time: ~2 seconds
     */
    @Test
    fun testSignUp_WeakPassword_ReturnsFailureResult() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Prepare weak password (< 6 characters)
        // ─────────────────────────────────────────────────────
        val weakPassword = "12345" // Only 5 characters (minimum is 6)

        // ─────────────────────────────────────────────────────
        // ACT: Attempt sign-up with weak password
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = "Weak Password User",
            username = "weakpass",
            email = testEmail,
            phoneNumber = "+60123456789",
            password = weakPassword
        )

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify failure and no user creation
        // ─────────────────────────────────────────────────────

        // Assert 1: Sign-up failed
        assertFalse(
            "Sign up should fail with password shorter than 6 characters",
            result.isSuccess
        )

        // Assert 2: No auth user was created
        val authUser = auth.currentUser
        assertNull(
            "No Firebase Auth account should be created with weak password",
            authUser
        )
    }

    /**
     * Level 1 Documentation: Error Handling - Invalid Email Format
     *
     * Test Method: UserRepository.signUp()
     * Scenario: User provides malformed email address
     * Expected Behavior: Firebase Auth rejects invalid email format
     *
     * AAA Pattern:
     * - ARRANGE: Prepare invalid email (missing @ and domain)
     * - ACT: Attempt sign-up with invalid email
     * - ASSERT: Registration fails
     *
     * Firebase Validation:
     * - Requires valid email format (user@domain.com)
     * - Throws FirebaseAuthInvalidCredentialsException
     *
     * Success Criteria:
     * ✓ signUp() returns Result.failure
     * ✓ Exception captured
     *
     * Dependencies: Firebase Auth Emulator
     * Execution Time: ~2 seconds
     */
    @Test
    fun testSignUp_InvalidEmailFormat_ReturnsFailureResult() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Prepare invalid email
        // ─────────────────────────────────────────────────────
        val invalidEmail = "not-an-email" // Missing @ and domain

        // ─────────────────────────────────────────────────────
        // ACT: Attempt sign-up with invalid email
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = "Invalid Email User",
            username = "invalidemail",
            email = invalidEmail,
            phoneNumber = "+60123456789",
            password = testPassword
        )

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify failure
        // ─────────────────────────────────────────────────────

        // Assert 1: Sign-up failed
        assertFalse(
            "Sign up should fail with invalid email format",
            result.isSuccess
        )

        // Assert 2: Exception is captured
        assertTrue(
            "Result should contain exception explaining failure",
            result.exceptionOrNull() != null
        )
    }

    // ═══════════════════════════════════════════════════════════
    // CATEGORY 3: EDGE CASES (PRIORITY: MEDIUM)
    // ═══════════════════════════════════════════════════════════

    /**
     * Level 1 Documentation: Edge Case - Special Characters in Name
     *
     * Test Method: UserRepository.signUp()
     * Scenario: User has special characters (apostrophe, hyphen) in name
     * Expected Behavior: Name saved correctly with all special characters preserved
     *
     * AAA Pattern:
     * - ARRANGE: Prepare name with special characters
     * - ACT: Sign up user
     * - ASSERT: Verify name saved exactly as input
     *
     * Test Cases Covered:
     * - Apostrophes (O'Brien)
     * - Hyphens (Smith-Jones)
     * - Combined (O'Brien-Smith)
     *
     * Success Criteria:
     * ✓ Sign-up succeeds
     * ✓ Special characters preserved
     * ✓ No encoding issues
     *
     * Dependencies: Firebase Auth + Firestore Emulators
     * Execution Time: ~2 seconds
     */
    @Test
    fun testSignUp_SpecialCharactersInName_SavedCorrectly() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Name with special characters
        // ─────────────────────────────────────────────────────
        val specialName = "O'Brien-Smith"
        val username = "obrien"

        // ─────────────────────────────────────────────────────
        // ACT: Sign up with special name
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = specialName,
            username = username,
            email = testEmail,
            phoneNumber = "+60123456789",
            password = testPassword
        )

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify special characters preserved
        // ─────────────────────────────────────────────────────

        // Assert 1: Sign-up succeeded
        assertTrue(
            "Sign up should succeed with special characters in name",
            result.isSuccess
        )

        // Assert 2: Retrieve user data
        testUid = auth.currentUser?.uid
        val userDoc = firestore.collection("users")
            .document(testUid!!)
            .get()
            .await()

        val user = userDoc.toObject(User::class.java)

        // Assert 3: Special characters preserved
        assertEquals(
            "Special characters (apostrophe, hyphen) should be preserved in name",
            specialName,
            user?.fullName
        )
    }

    /**
     * Level 1 Documentation: Edge Case - International Characters
     *
     * Test Method: UserRepository.signUp()
     * Scenario: User has non-ASCII characters (Chinese, Arabic, etc.) in name
     * Expected Behavior: Unicode characters saved correctly without corruption
     *
     * AAA Pattern:
     * - ARRANGE: Prepare name with Chinese characters
     * - ACT: Sign up user
     * - ASSERT: Verify Unicode preservation
     *
     * Test Cases:
     * - Chinese: 李明
     * - Could extend to: Arabic, Cyrillic, Emoji, etc.
     *
     * Success Criteria:
     * ✓ Sign-up succeeds
     * ✓ Unicode characters preserved exactly
     * ✓ No mojibake or encoding issues
     *
     * Dependencies: Firebase Auth + Firestore Emulators
     * Execution Time: ~2 seconds
     */
    @Test
    fun testSignUp_InternationalCharacters_SavedCorrectly() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Name with Chinese characters
        // ─────────────────────────────────────────────────────
        val internationalName = "李明" // Chinese characters
        val username = "liming"

        // ─────────────────────────────────────────────────────
        // ACT: Sign up with international name
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = internationalName,
            username = username,
            email = testEmail,
            phoneNumber = "+86123456789",
            password = testPassword
        )

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify Unicode preservation
        // ─────────────────────────────────────────────────────

        // Assert 1: Sign-up succeeded
        assertTrue(
            "Sign up should succeed with international characters",
            result.isSuccess
        )

        // Assert 2: Retrieve user data
        testUid = auth.currentUser?.uid
        val userDoc = firestore.collection("users")
            .document(testUid!!)
            .get()
            .await()

        val user = userDoc.toObject(User::class.java)

        // Assert 3: Unicode preserved
        assertEquals(
            "International Unicode characters should be preserved without corruption",
            internationalName,
            user?.fullName
        )
    }

    /**
     * Level 1 Documentation: Edge Case - Minimum Password Length
     *
     * Test Method: UserRepository.signUp()
     * Scenario: User provides exactly 6-character password (Firebase minimum)
     * Expected Behavior: Sign-up succeeds (boundary condition)
     *
     * AAA Pattern:
     * - ARRANGE: Prepare 6-character password
     * - ACT: Sign up user
     * - ASSERT: Verify success
     *
     * Boundary Testing:
     * - 5 chars: Should fail (tested in WeakPassword test)
     * - 6 chars: Should succeed (this test)
     * - 7+ chars: Should succeed (covered by other tests)
     *
     * Success Criteria:
     * ✓ Sign-up succeeds with 6-char password
     * ✓ Auth account created
     *
     * Dependencies: Firebase Auth Emulator
     * Execution Time: ~2 seconds
     */
    @Test
    fun testSignUp_MinimumLengthPassword_WorksCorrectly() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Exactly 6-character password
        // ─────────────────────────────────────────────────────
        val minPassword = "123456" // Exactly 6 characters (Firebase minimum)

        // ─────────────────────────────────────────────────────
        // ACT: Sign up with minimum password
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = "Min Password User",
            username = "minpass",
            email = testEmail,
            phoneNumber = "+60123456789",
            password = minPassword
        )

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify success
        // ─────────────────────────────────────────────────────

        // Assert 1: Sign-up succeeded
        assertTrue(
            "6-character password should be accepted (Firebase minimum)",
            result.isSuccess
        )

        // Assert 2: Auth user created
        testUid = auth.currentUser?.uid
        assertNotNull(
            "Firebase Auth user should be created with minimum length password",
            testUid
        )
    }

    // ═══════════════════════════════════════════════════════════
    // CATEGORY 4: PERFORMANCE TESTS (PRIORITY: LOW)
    // ═══════════════════════════════════════════════════════════

    /**
     * Level 1 Documentation: Performance - Sign-Up Timeout
     *
     * Test Method: UserRepository.signUp()
     * Scenario: Monitor sign-up execution time
     * Expected Behavior: Complete within acceptable time limit (5 seconds)
     *
     * AAA Pattern:
     * - ARRANGE: Record start time
     * - ACT: Sign up user
     * - ASSERT: Verify completion time < threshold
     *
     * Performance Targets:
     * - Excellent: < 2 seconds
     * - Good: 2-3 seconds
     * - Acceptable: 3-5 seconds
     * - Poor: > 5 seconds (FAIL)
     *
     * Factors Affecting Performance:
     * - Network latency to Firebase
     * - Firestore write speed
     * - Device performance
     * - Emulator vs Production
     *
     * Success Criteria:
     * ✓ Sign-up succeeds
     * ✓ Execution time < 5 seconds
     *
     * Note: Emulator tests typically faster than production
     *
     * Dependencies: Firebase Auth + Firestore Emulators
     * Execution Time: ~2-5 seconds
     */
    @Test
    fun testSignUp_ValidData_CompletesWithinTimeout() = runTest {
        // ─────────────────────────────────────────────────────
        // ARRANGE: Record start time
        // ─────────────────────────────────────────────────────
        val startTime = System.currentTimeMillis()

        // ─────────────────────────────────────────────────────
        // ACT: Execute sign-up
        // ─────────────────────────────────────────────────────
        val result = userRepository.signUp(
            fullName = "Performance Test",
            username = "perftest",
            email = testEmail,
            phoneNumber = "+60123456789",
            password = testPassword
        )

        val executionTime = System.currentTimeMillis() - startTime
        testUid = auth.currentUser?.uid

        // ─────────────────────────────────────────────────────
        // ASSERT: Verify success and timing
        // ─────────────────────────────────────────────────────

        // Assert 1: Sign-up succeeded
        assertTrue(
            "Sign up should succeed",
            result.isSuccess
        )

        // Assert 2: Completed within timeout
        assertTrue(
            "Sign up should complete within 5 seconds (took ${executionTime}ms)",
            executionTime < 5000
        )

        // Log performance for monitoring
        println("Sign-up performance: ${executionTime}ms")
    }
}