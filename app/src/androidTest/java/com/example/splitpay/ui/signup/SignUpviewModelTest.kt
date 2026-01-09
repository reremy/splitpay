import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.signup.SignUpViewModel
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import io.mockk.coVerify
import org.junit.Assert.*

/**
 * Unit Tests for SignUpViewModel
 *
 * Test Class Purpose:
 * Validates the user registration form validation logic to ensure proper error handling
 * and user feedback before attempting Firebase authentication.
 *
 * Functionality Under Test:
 * - Full name validation (non-empty requirement)
 * - Username validation (non-empty requirement)
 * - Email validation (format using Android Patterns.EMAIL_ADDRESS)
 * - Phone number validation (format and minimum length)
 * - Password validation (minimum 6 characters - Firebase requirement)
 * - Password matching validation (password == retypePassword)
 *
 * Test Coverage:
 * This minimal test suite focuses on critical validation paths that prevent invalid
 * data from reaching Firebase. Integration with Firebase is tested separately.
 *
 * Testing Approach:
 * - Mock UserRepository to isolate validation logic from network operations
 * - Test each validation rule in isolation with one test per rule
 * - Use AAA (Arrange-Act-Assert) pattern for clarity
 * - Verify UI state changes (error messages) after validation
 *
 * Total Tests: 8
 * Test Suite Type: Minimal (Core Validations Only)
 * Author: [Your Name]
 * Date: January 2026
 */
//@RunWith(MockitoJUnitRunner::class)  // Or use JUnit4 without this annotation
class SignUpViewModelTest {

    // ========== SETUP ==========
    // Mock repository to avoid Firebase calls
    private lateinit var mockRepository: UserRepository
    private lateinit var viewModel: SignUpViewModel

    @Before
    fun setup() {
        // Initialize fresh mocks and ViewModel before each test
        // This ensures tests don't interfere with each other
        mockRepository = mockk<UserRepository>(relaxed = true)
        viewModel = SignUpViewModel(repository = mockRepository)
    }

    // ========== FULL NAME VALIDATION (1 test) ==========

    /**
     * Test: Validates that an empty full name field is caught during sign-up
     *
     * Purpose: Ensures users cannot create accounts without providing their full name,
     * which is required for displaying user information throughout the app.
     *
     * Test Data:
     * - Input: Empty string for full name, valid values for all other fields
     * - Expected Output: fullNameError should contain "Full name cannot be empty"
     *
     * Verification:
     * - UI state should have non-null fullNameError message
     * - Other fields should have no errors (since they're valid)
     * - Repository signUp should NOT be called (validation failed)
     *
     * Edge Case/Scenario: Boundary test - empty field validation
     */
    @Test
    fun onSignUpClick_emptyFullName_setsFullNameError() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Create a mock UserRepository to avoid actual Firebase calls
        // MockK will prevent real network operations during testing
        val mockRepository = mockk<UserRepository>()

        // Initialize the ViewModel with our mock repository
        // This ViewModel instance will be used for this test only
        val viewModel = SignUpViewModel(repository = mockRepository)

        // Set up test data: Empty full name (the field we're testing)
        // All other fields are VALID to isolate just the full name validation
        viewModel.onFullNameChange("")  // EMPTY - this should trigger error
        viewModel.onUsernameChange("validuser123")  // Valid username
        viewModel.onEmailChange("test@example.com")  // Valid email format
        viewModel.onPhoneNumberChange("+60123456789")  // Valid Malaysian number
        viewModel.onPasswordChange("password123")  // Valid password (6+ chars)
        viewModel.onRetypePasswordChange("password123")  // Matching password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // Since full name is empty, validation should fail and set an error
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that the UI state contains the expected error message
        // The error message should match exactly what's in SignUpViewModel
        val currentState = viewModel.uiState.value
        assertEquals(
            "Full name cannot be empty",  // Expected error message
            currentState.fullNameError     // Actual error from UI state
        )

        // Verify that other fields have NO errors (they were valid)
        assertNull(currentState.usernameError)
        assertNull(currentState.emailError)
        assertNull(currentState.phoneNumberError)
        assertNull(currentState.passwordError)
        assertNull(currentState.retypePasswordError)

        // IMPORTANT: Verify repository signUp was NOT called
        // Since validation failed, we should never reach the repository call
        coVerify(exactly = 0) {
            mockRepository.signUp(any(), any(), any(), any(), any())
        }
    }

    // ========== USERNAME VALIDATION (1 test) ==========
// ========== USERNAME VALIDATION (1 test) ==========

    /**
     * Test: Validates that an empty username field is caught during sign-up
     *
     * Purpose: Ensures users provide a username, which is required for user identification
     * and displaying in friend lists, group members, and transactions.
     *
     * Test Data:
     * - Input: Empty string for username, valid values for all other fields
     * - Expected Output: usernameError should contain "Username cannot be empty"
     *
     * Verification:
     * - UI state should have non-null usernameError message
     * - Other fields should have no errors (since they're valid)
     * - Repository signUp should NOT be called (validation failed)
     *
     * Edge Case/Scenario: Boundary test - empty field validation
     */
    @Test
    fun onSignUpClick_emptyUsername_setsUsernameError() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Set up test data: Empty username (the field we're testing)
        // All other fields are VALID to isolate just the username validation
        viewModel.onFullNameChange("John Doe")  // Valid full name
        viewModel.onUsernameChange("")  // EMPTY - this should trigger error
        viewModel.onEmailChange("test@example.com")  // Valid email format
        viewModel.onPhoneNumberChange("+60123456789")  // Valid Malaysian number
        viewModel.onPasswordChange("password123")  // Valid password (6+ chars)
        viewModel.onRetypePasswordChange("password123")  // Matching password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // Since username is empty, validation should fail and set an error
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that the UI state contains the expected error message
        val currentState = viewModel.uiState.value
        assertEquals(
            "Username cannot be empty",  // Expected error message
            currentState.usernameError    // Actual error from UI state
        )

        // Verify that other fields have NO errors (they were valid)
        assertNull(currentState.fullNameError)
        assertNull(currentState.emailError)
        assertNull(currentState.phoneNumberError)
        assertNull(currentState.passwordError)
        assertNull(currentState.retypePasswordError)

        // Verify repository signUp was NOT called (validation failed)
        coVerify(exactly = 0) {
            mockRepository.signUp(any(), any(), any(), any(), any())
        }
    }

    // ========== EMAIL VALIDATION (2 tests) ==========

    /**
     * Test: Validates that an empty email field is caught during sign-up
     *
     * Purpose: Ensures users provide an email address, which is required by Firebase
     * Authentication as the primary identifier for user accounts.
     *
     * Test Data:
     * - Input: Empty string for email, valid values for all other fields
     * - Expected Output: emailError should contain "Invalid email"
     *
     * Verification:
     * - UI state should have non-null emailError message
     * - Other fields should have no errors (since they're valid)
     * - Repository signUp should NOT be called (validation failed)
     *
     * Edge Case/Scenario: Boundary test - empty field validation
     */
    @Test
    fun onSignUpClick_emptyEmail_setsEmailError() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Set up test data: Empty email (the field we're testing)
        // All other fields are VALID to isolate just the email validation
        viewModel.onFullNameChange("John Doe")  // Valid full name
        viewModel.onUsernameChange("johndoe")  // Valid username
        viewModel.onEmailChange("")  // EMPTY - this should trigger error
        viewModel.onPhoneNumberChange("+60123456789")  // Valid Malaysian number
        viewModel.onPasswordChange("password123")  // Valid password (6+ chars)
        viewModel.onRetypePasswordChange("password123")  // Matching password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // Since email is empty, validation should fail and set an error
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that the UI state contains the expected error message
        // Empty email is treated as invalid format in the ViewModel
        val currentState = viewModel.uiState.value
        assertEquals(
            "Invalid email",          // Expected error message
            currentState.emailError   // Actual error from UI state
        )

        // Verify that other fields have NO errors (they were valid)
        assertNull(currentState.fullNameError)
        assertNull(currentState.usernameError)
        assertNull(currentState.phoneNumberError)
        assertNull(currentState.passwordError)
        assertNull(currentState.retypePasswordError)

        // Verify repository signUp was NOT called (validation failed)
        coVerify(exactly = 0) {
            mockRepository.signUp(any(), any(), any(), any(), any())
        }
    }

    /**
     * Test: Validates that invalid email format is caught during sign-up
     *
     * Purpose: Ensures users provide a properly formatted email address using
     * Android's Patterns.EMAIL_ADDRESS validation, preventing Firebase authentication
     * errors and ensuring reliable user communication.
     *
     * Test Data:
     * - Input: "invalid-email" (missing @ symbol and domain), valid other fields
     * - Expected Output: emailError should contain "Invalid email"
     *
     * Verification:
     * - UI state should have non-null emailError message
     * - Other fields should have no errors (since they're valid)
     * - Repository signUp should NOT be called (validation failed)
     *
     * Edge Case/Scenario: Format validation test - email pattern matching
     */
    @Test
    fun onSignUpClick_invalidEmailFormat_setsEmailError() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Set up test data: Invalid email format (the field we're testing)
        // Using "invalid-email" which lacks @ symbol and domain
        // All other fields are VALID to isolate just the email format validation
        viewModel.onFullNameChange("John Doe")  // Valid full name
        viewModel.onUsernameChange("johndoe")  // Valid username
        viewModel.onEmailChange("invalid-email")  // INVALID - no @ or domain
        viewModel.onPhoneNumberChange("+60123456789")  // Valid Malaysian number
        viewModel.onPasswordChange("password123")  // Valid password (6+ chars)
        viewModel.onRetypePasswordChange("password123")  // Matching password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // Since email format is invalid, validation should fail and set an error
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that the UI state contains the expected error message
        // Patterns.EMAIL_ADDRESS should reject this format
        val currentState = viewModel.uiState.value
        assertEquals(
            "Invalid email",          // Expected error message
            currentState.emailError   // Actual error from UI state
        )

        // Verify that other fields have NO errors (they were valid)
        assertNull(currentState.fullNameError)
        assertNull(currentState.usernameError)
        assertNull(currentState.phoneNumberError)
        assertNull(currentState.passwordError)
        assertNull(currentState.retypePasswordError)

        // Verify repository signUp was NOT called (validation failed)
        coVerify(exactly = 0) {
            mockRepository.signUp(any(), any(), any(), any(), any())
        }
    }

    // ========== PHONE NUMBER VALIDATION (2 tests) ==========

    /**
     * Test: Validates that an empty phone number field is caught during sign-up
     *
     * Purpose: Ensures users provide a phone number, which is stored in Firestore
     * for user profile information and potential future features like SMS notifications.
     *
     * Test Data:
     * - Input: Empty string for phone number, valid values for all other fields
     * - Expected Output: phoneNumberError should contain "Phone number cannot be empty"
     *
     * Verification:
     * - UI state should have non-null phoneNumberError message
     * - Other fields should have no errors (since they're valid)
     * - Repository signUp should NOT be called (validation failed)
     *
     * Edge Case/Scenario: Boundary test - empty field validation
     */
    @Test
    fun onSignUpClick_emptyPhoneNumber_setsPhoneNumberError() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Set up test data: Empty phone number (the field we're testing)
        // All other fields are VALID to isolate just the phone number validation
        viewModel.onFullNameChange("John Doe")  // Valid full name
        viewModel.onUsernameChange("johndoe")  // Valid username
        viewModel.onEmailChange("test@example.com")  // Valid email format
        viewModel.onPhoneNumberChange("")  // EMPTY - this should trigger error
        viewModel.onPasswordChange("password123")  // Valid password (6+ chars)
        viewModel.onRetypePasswordChange("password123")  // Matching password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // Since phone number is empty, validation should fail and set an error
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that the UI state contains the expected error message
        val currentState = viewModel.uiState.value
        assertEquals(
            "Phone number cannot be empty",  // Expected error message
            currentState.phoneNumberError    // Actual error from UI state
        )

        // Verify that other fields have NO errors (they were valid)
        assertNull(currentState.fullNameError)
        assertNull(currentState.usernameError)
        assertNull(currentState.emailError)
        assertNull(currentState.passwordError)
        assertNull(currentState.retypePasswordError)

        // Verify repository signUp was NOT called (validation failed)
        coVerify(exactly = 0) {
            mockRepository.signUp(any(), any(), any(), any(), any())
        }
    }

    /**
     * Test: Validates that invalid phone number format is caught during sign-up
     *
     * Purpose: Ensures users provide a valid phone number format with at least 10 digits.
     * The isValidPhoneNumber() function checks both digit count and format pattern,
     * preventing invalid phone numbers from being stored.
     *
     * Test Data:
     * - Input: "12345" (only 5 digits - below 10 digit minimum), valid other fields
     * - Expected Output: phoneNumberError should contain "Invalid phone number format"
     *
     * Verification:
     * - UI state should have non-null phoneNumberError message
     * - Other fields should have no errors (since they're valid)
     * - Repository signUp should NOT be called (validation failed)
     *
     * Edge Case/Scenario: Format validation test - phone number length and pattern
     */
    @Test
    fun onSignUpClick_invalidPhoneFormat_setsPhoneNumberError() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Set up test data: Invalid phone format (the field we're testing)
        // Using "12345" which has only 5 digits (minimum is 10)
        // This will fail the isValidPhoneNumber() validation
        // All other fields are VALID to isolate just the phone validation
        viewModel.onFullNameChange("John Doe")  // Valid full name
        viewModel.onUsernameChange("johndoe")  // Valid username
        viewModel.onEmailChange("test@example.com")  // Valid email format
        viewModel.onPhoneNumberChange("12345")  // INVALID - only 5 digits
        viewModel.onPasswordChange("password123")  // Valid password (6+ chars)
        viewModel.onRetypePasswordChange("password123")  // Matching password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // The isValidPhoneNumber() function should reject this format
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that the UI state contains the expected error message
        // This message comes from line 95 in SignUpViewModel
        val currentState = viewModel.uiState.value
        assertEquals(
            "Invalid phone number format",  // Expected error message
            currentState.phoneNumberError   // Actual error from UI state
        )

        // Verify that other fields have NO errors (they were valid)
        assertNull(currentState.fullNameError)
        assertNull(currentState.usernameError)
        assertNull(currentState.emailError)
        assertNull(currentState.passwordError)
        assertNull(currentState.retypePasswordError)

        // Verify repository signUp was NOT called (validation failed)
        coVerify(exactly = 0) {
            mockRepository.signUp(any(), any(), any(), any(), any())
        }
    }

    // ========== PASSWORD VALIDATION (2 tests) ==========

    /**
     * Test: Validates that password must be at least 6 characters long
     *
     * Purpose: Enforces Firebase Authentication's minimum password requirement of 6 characters.
     * This prevents users from creating accounts with passwords that Firebase would reject,
     * ensuring security standards and avoiding authentication errors.
     *
     * Test Data:
     * - Input: "pass1" (only 5 characters - below 6 character minimum), valid other fields
     * - Expected Output: passwordError should contain "Password must be at least 6 characters"
     *
     * Verification:
     * - UI state should have non-null passwordError message
     * - Other fields should have no errors (since they're valid)
     * - Repository signUp should NOT be called (validation failed)
     *
     * Edge Case/Scenario: Boundary test - minimum password length requirement
     */
    @Test
    fun onSignUpClick_shortPassword_setsPasswordError() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Set up test data: Short password (the field we're testing)
        // Using "pass1" which has only 5 characters (minimum is 6)
        // This tests the Firebase minimum password length requirement
        // All other fields are VALID to isolate just the password validation
        viewModel.onFullNameChange("John Doe")  // Valid full name
        viewModel.onUsernameChange("johndoe")  // Valid username
        viewModel.onEmailChange("test@example.com")  // Valid email format
        viewModel.onPhoneNumberChange("+60123456789")  // Valid Malaysian number
        viewModel.onPasswordChange("pass1")  // INVALID - only 5 characters
        viewModel.onRetypePasswordChange("pass1")  // Matching the short password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // Password length check should fail since it's less than 6 characters
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that the UI state contains the expected error message
        // This message comes from line 99 in SignUpViewModel
        val currentState = viewModel.uiState.value
        assertEquals(
            "Password must be at least 6 characters",  // Expected error message
            currentState.passwordError                  // Actual error from UI state
        )

        // Verify that other fields have NO errors (they were valid)
        assertNull(currentState.fullNameError)
        assertNull(currentState.usernameError)
        assertNull(currentState.emailError)
        assertNull(currentState.phoneNumberError)
        // Note: retypePasswordError might be null because the passwords match,
        // we're just testing that password is too short
        assertNull(currentState.retypePasswordError)

        // Verify repository signUp was NOT called (validation failed)
        coVerify(exactly = 0) {
            mockRepository.signUp(any(), any(), any(), any(), any())
        }
    }

    /**
     * Test: Validates that password and retype password fields must match
     *
     * Purpose: Ensures users correctly confirm their password by typing it twice,
     * preventing account creation with mistyped passwords that users cannot remember.
     * This is a critical UX feature that saves users from account access issues.
     *
     * Test Data:
     * - Input: password="password123", retypePassword="password456" (different), valid other fields
     * - Expected Output: retypePasswordError should contain "Passwords do not match"
     *
     * Verification:
     * - UI state should have non-null retypePasswordError message
     * - Other fields should have no errors (since they're valid)
     * - Repository signUp should NOT be called (validation failed)
     *
     * Edge Case/Scenario: Business rule validation - password confirmation matching
     */
    @Test
    fun onSignUpClick_passwordMismatch_setsRetypePasswordError() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Set up test data: Mismatched passwords (the fields we're testing)
        // Password is "password123" but retypePassword is "password456"
        // This tests the password matching validation rule
        // All other fields are VALID to isolate just the password match validation
        viewModel.onFullNameChange("John Doe")  // Valid full name
        viewModel.onUsernameChange("johndoe")  // Valid username
        viewModel.onEmailChange("test@example.com")  // Valid email format
        viewModel.onPhoneNumberChange("+60123456789")  // Valid Malaysian number
        viewModel.onPasswordChange("password123")  // Valid password (6+ chars)
        viewModel.onRetypePasswordChange("password456")  // MISMATCH - different password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // Password match check should fail since they're different
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that the UI state contains the expected error message
        // This message comes from line 104 in SignUpViewModel
        val currentState = viewModel.uiState.value
        assertEquals(
            "Passwords do not match",           // Expected error message
            currentState.retypePasswordError    // Actual error from UI state
        )

        // Verify that other fields have NO errors (they were valid)
        assertNull(currentState.fullNameError)
        assertNull(currentState.usernameError)
        assertNull(currentState.emailError)
        assertNull(currentState.phoneNumberError)
        assertNull(currentState.passwordError)  // Password itself is valid (6+ chars)

        // Verify repository signUp was NOT called (validation failed)
        coVerify(exactly = 0) {
            mockRepository.signUp(any(), any(), any(), any(), any())
        }
    }

    // ========== HAPPY PATH (1 test) ==========

    /**
     * Test: Validates that all valid inputs pass validation without errors
     *
     * Purpose: Verifies the happy path where a user provides all valid information,
     * ensuring the validation logic doesn't incorrectly reject valid data. This test
     * confirms that when all fields are properly filled, no validation errors are set.
     *
     * Test Data:
     * - Input: All fields contain valid data meeting all requirements
     * - Expected Output: All error fields should be null (no validation errors)
     *
     * Verification:
     * - All error fields in UI state should be null
     * - No validation errors should be present
     * - This test does NOT verify Firebase call (that's integration testing)
     *
     * Edge Case/Scenario: Happy path - all validations pass
     */
    @Test
    fun onSignUpClick_allValidInputs_noValidationErrors() {
        // ============================================
        // ARRANGE (Setup)
        // ============================================
        // Set up test data: ALL fields are valid
        // This is the "happy path" where everything is correct
        viewModel.onFullNameChange("John Doe")  // Valid - not empty, 8 characters
        viewModel.onUsernameChange("johndoe123")  // Valid - not empty, alphanumeric
        viewModel.onEmailChange("john.doe@example.com")  // Valid - proper email format
        viewModel.onPhoneNumberChange("+60123456789")  // Valid - 10 digits with country code
        viewModel.onPasswordChange("password123")  // Valid - 11 characters (6+ required)
        viewModel.onRetypePasswordChange("password123")  // Valid - matches password

        // ============================================
        // ACT (Execute)
        // ============================================
        // Call the sign-up function - this will trigger validation
        // All validations should pass, no errors should be set
        viewModel.onSignUpClick()

        // ============================================
        // ASSERT (Verify)
        // ============================================
        // Check that NO error messages are set in the UI state
        // All error fields should be null when validation passes
        val currentState = viewModel.uiState.value
        assertNull(currentState.fullNameError)
        assertNull(currentState.usernameError)
        assertNull(currentState.emailError)
        assertNull(currentState.phoneNumberError)
        assertNull(currentState.passwordError)
        assertNull(currentState.retypePasswordError)

        // Additional check: Verify the generic errorMessage is also empty
        assertEquals("", currentState.errorMessage)

        // NOTE: We are NOT verifying that repository.signUp() was called here
        // because that would require setting up mock responses and testing
        // the coroutine launch block, which is beyond the scope of this
        // minimal validation-focused test suite. That would be tested in
        // integration tests with proper coroutine testing setup.
    }
}