# Testing Documentation

Comprehensive testing guide for SplitPay.

---

## Overview

SplitPay uses a multi-layered testing strategy:

1. **Unit Tests** - Test individual components in isolation
2. **Integration Tests** - Test component interactions
3. **UI Tests** - Test user interface and flows
4. **Manual Testing** - Manual QA and exploratory testing

---

## Test Structure

```
app/src/
├── test/                           # Unit tests (run on JVM)
│   └── java/com/example/splitpay/
│       ├── domain/
│       │   └── usecase/
│       │       └── CalculateSplitUseCaseTest.kt
│       ├── viewmodel/
│       │   ├── ExpenseViewModelTest.kt
│       │   └── GroupViewModelTest.kt
│       └── utils/
│           └── UtilsTest.kt
│
└── androidTest/                    # Instrumented tests (run on device/emulator)
    └── java/com/example/splitpay/
        ├── ui/
        │   ├── LoginScreenTest.kt
        │   ├── ExpenseFlowTest.kt
        │   └── NavigationTest.kt
        └── repository/
            ├── UserRepositoryTest.kt
            └── ExpenseRepositoryTest.kt
```

---

## Unit Testing

### Purpose
Test business logic, ViewModels, and use cases in isolation.

### Framework
- **JUnit 4**: Test framework
- **MockK** or **Mockito**: Mocking framework (recommended: MockK for Kotlin)
- **Kotlinx Coroutines Test**: Testing coroutines

### Dependencies

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0") // For Flow testing
}
```

### Example: Testing Use Case

**File**: `CalculateSplitUseCaseTest.kt`

```kotlin
class CalculateSplitUseCaseTest {

    private lateinit var useCase: CalculateSplitUseCase

    @Before
    fun setup() {
        useCase = CalculateSplitUseCase()
    }

    @Test
    fun `calculate equal split for 3 people`() {
        // Given
        val totalAmount = 100.0
        val participants = listOf("user1", "user2", "user3")
        val splitType = "EQUALLY"

        // When
        val result = useCase.calculateSplit(
            totalAmount = totalAmount,
            participants = participants,
            splitType = splitType
        )

        // Then
        assertEquals(3, result.size)
        result.forEach { participant ->
            assertEquals(33.33, participant.owesAmount, 0.01)
        }
    }

    @Test
    fun `calculate exact amounts split`() {
        // Given
        val totalAmount = 100.0
        val participants = mapOf(
            "user1" to 50.0,
            "user2" to 30.0,
            "user3" to 20.0
        )

        // When
        val result = useCase.calculateExactAmounts(participants)

        // Then
        assertEquals(50.0, result["user1"]?.owesAmount)
        assertEquals(30.0, result["user2"]?.owesAmount)
        assertEquals(20.0, result["user3"]?.owesAmount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throw exception when total amount is negative`() {
        // Given
        val totalAmount = -100.0

        // When/Then
        useCase.calculateSplit(
            totalAmount = totalAmount,
            participants = listOf("user1"),
            splitType = "EQUALLY"
        )
    }
}
```

### Example: Testing ViewModel

**File**: `ExpenseViewModelTest.kt`

```kotlin
@ExperimentalCoroutinesTest
class ExpenseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ExpenseViewModel
    private lateinit var expenseRepository: ExpenseRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        expenseRepository = mockk()
        viewModel = ExpenseViewModel(expenseRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addExpense success updates ui state`() = runTest {
        // Given
        val expense = Expense(
            description = "Test",
            totalAmount = 100.0
        )
        coEvery { expenseRepository.addExpense(any()) } returns Result.success("exp123")

        // When
        viewModel.addExpense(expense)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertFalse(state.isLoading)
    }

    @Test
    fun `addExpense failure shows error`() = runTest {
        // Given
        val expense = Expense(description = "Test", totalAmount = 100.0)
        val errorMessage = "Network error"
        coEvery { expenseRepository.addExpense(any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.addExpense(expense)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals(errorMessage, (state as UiState.Error).message)
    }
}
```

### Testing Flows

```kotlin
@Test
fun `observeExpenses emits updates`() = runTest {
    // Given
    val expenses = listOf(
        Expense(id = "1", description = "Expense 1", totalAmount = 100.0),
        Expense(id = "2", description = "Expense 2", totalAmount = 200.0)
    )
    coEvery { expenseRepository.observeExpenses(any()) } returns flowOf(expenses)

    // When/Then
    viewModel.expenses.test {
        assertEquals(emptyList(), awaitItem()) // Initial empty state
        viewModel.loadExpenses("group123")
        assertEquals(expenses, awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## Integration Testing

### Purpose
Test integration between repositories and Firebase services.

### Firebase Emulator Suite

Use Firebase emulators for local testing:

**Setup**:
1. Install Firebase CLI: `npm install -g firebase-tools`
2. Initialize emulators: `firebase init emulators`
3. Select Auth, Firestore, and Storage
4. Start emulators: `firebase emulators:start`

**Configuration** (in `androidTest`):

```kotlin
@Before
fun setupFirebaseEmulators() {
    if (BuildConfig.DEBUG) {
        Firebase.auth.useEmulator("10.0.2.2", 9099)
        Firebase.firestore.useEmulator("10.0.2.2", 8080)
        Firebase.storage.useEmulator("10.0.2.2", 9199)
    }
}
```

### Example: Repository Integration Test

**File**: `UserRepositoryTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class UserRepositoryTest {

    private lateinit var repository: UserRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    @Before
    fun setup() {
        // Use Firebase Emulators
        auth = Firebase.auth
        firestore = Firebase.firestore

        auth.useEmulator("10.0.2.2", 9099)
        firestore.useEmulator("10.0.2.2", 8080)

        repository = UserRepository(auth, firestore)
    }

    @After
    fun tearDown() = runBlocking {
        // Clean up test data
        auth.currentUser?.delete()
        firestore.clearPersistence()
    }

    @Test
    fun signUp_createsUserInAuthAndFirestore() = runBlocking {
        // Given
        val email = "test@example.com"
        val password = "Test123!"
        val username = "testuser"
        val fullName = "Test User"

        // When
        val result = repository.signUp(
            fullName = fullName,
            username = username,
            email = email,
            phoneNumber = "+1234567890",
            password = password
        )

        // Then
        assertTrue(result.isSuccess)

        // Verify user in Auth
        val currentUser = auth.currentUser
        assertNotNull(currentUser)
        assertEquals(email, currentUser?.email)

        // Verify user document in Firestore
        val userDoc = firestore.collection("users")
            .document(currentUser?.uid ?: "")
            .get()
            .await()

        assertTrue(userDoc.exists())
        assertEquals(username, userDoc.getString("username"))
        assertEquals(fullName, userDoc.getString("fullName"))
    }
}
```

---

## UI Testing

### Purpose
Test Compose UI and user flows end-to-end.

### Framework
- **Compose UI Test**: Testing Compose components
- **Espresso**: Android UI testing (for non-Compose components)

### Dependencies

```kotlin
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
```

### Example: Compose UI Test

**File**: `LoginScreenTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysEmailAndPasswordFields() {
        // Given
        composeTestRule.setContent {
            SplitPayTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToSignUp = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsErrorForInvalidEmail() {
        // Given
        composeTestRule.setContent {
            SplitPayTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToSignUp = {}
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Email")
            .performTextInput("invalid-email")
        composeTestRule.onNodeWithText("Password")
            .performTextInput("password123")
        composeTestRule.onNodeWithText("Login")
            .performClick()

        // Then
        composeTestRule.onNodeWithText("Invalid email format")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_navigatesOnSuccessfulLogin() {
        // Given
        var loginSuccessCalled = false
        composeTestRule.setContent {
            SplitPayTheme {
                LoginScreen(
                    onLoginSuccess = { loginSuccessCalled = true },
                    onNavigateToSignUp = {}
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Email")
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password")
            .performTextInput("Test123!")
        composeTestRule.onNodeWithText("Login")
            .performClick()

        // Wait for async operation
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            loginSuccessCalled
        }

        // Then
        assertTrue(loginSuccessCalled)
    }
}
```

### Example: Navigation Test

**File**: `NavigationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigation_switchesTabs() {
        // When - Click on Groups tab
        composeTestRule.onNodeWithText("Groups").performClick()

        // Then - Groups screen is displayed
        composeTestRule.onNodeWithText("My Groups").assertIsDisplayed()

        // When - Click on Activity tab
        composeTestRule.onNodeWithText("Activity").performClick()

        // Then - Activity screen is displayed
        composeTestRule.onNodeWithText("Recent Activity").assertIsDisplayed()
    }
}
```

### Testing Semantics

```kotlin
@Test
fun expenseCard_hasCorrectSemantics() {
    val expense = Expense(
        description = "Lunch",
        totalAmount = 25.0
    )

    composeTestRule.setContent {
        ExpenseCard(expense = expense, onClick = {})
    }

    // Test content description
    composeTestRule.onNodeWithContentDescription("Expense: Lunch")
        .assertIsDisplayed()

    // Test clickable
    composeTestRule.onNodeWithText("Lunch")
        .assertHasClickAction()
}
```

---

## Manual Testing

### Test Cases

#### User Authentication
- [ ] Sign up with valid credentials
- [ ] Sign up with invalid email
- [ ] Sign up with weak password
- [ ] Sign up with duplicate email
- [ ] Login with valid credentials
- [ ] Login with invalid credentials
- [ ] Logout

#### Friend Management
- [ ] Add friend by username
- [ ] Add friend by QR code
- [ ] View friends list
- [ ] View friend detail
- [ ] Remove friend
- [ ] Block user
- [ ] Unblock user

#### Group Management
- [ ] Create group
- [ ] Edit group
- [ ] Add members to group
- [ ] Remove member from group
- [ ] Leave group
- [ ] Archive group
- [ ] View group detail

#### Expense Management
- [ ] Add expense (equal split)
- [ ] Add expense (exact amounts)
- [ ] Add expense (percentages)
- [ ] Add expense (shares)
- [ ] Add expense with multiple payers
- [ ] Edit expense
- [ ] Delete expense
- [ ] View expense detail
- [ ] Add receipt image

#### Settlement
- [ ] Record payment (full settlement)
- [ ] Record payment (partial settlement)
- [ ] View payment detail
- [ ] Edit payment
- [ ] Delete payment

#### Activity Feed
- [ ] View activity feed
- [ ] Tap on activity to view detail
- [ ] Refresh activity feed

---

## Test Coverage

### Minimum Coverage Goals
- **Unit Tests**: 70% code coverage
- **Integration Tests**: Key user flows covered
- **UI Tests**: Critical paths tested

### Measuring Coverage

Run tests with coverage:

```bash
./gradlew testDebugUnitTestCoverage
./gradlew createDebugCoverageReport
```

View report: `app/build/reports/coverage/debug/index.html`

---

## Continuous Integration (CI)

### GitHub Actions Example

**File**: `.github/workflows/android-test.yml`

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'adopt'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run unit tests
        run: ./gradlew test

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: app/build/reports/tests/
```

---

## Best Practices

1. **Test Naming**: Use descriptive names
   ```kotlin
   @Test
   fun `calculateSplit with equal split returns correct amounts`()
   ```

2. **AAA Pattern**: Arrange, Act, Assert
   ```kotlin
   // Arrange
   val input = ...

   // Act
   val result = ...

   // Assert
   assertEquals(expected, result)
   ```

3. **Test One Thing**: Each test should verify one behavior

4. **Mock External Dependencies**: Mock Firebase, network calls, etc.

5. **Use Test Fixtures**: Create reusable test data

6. **Clean Up**: Reset state after each test

7. **Test Edge Cases**: Empty lists, null values, large numbers, etc.

8. **Fast Tests**: Unit tests should run quickly

---

## Debugging Tests

### Logcat in Tests
```kotlin
@Test
fun myTest() {
    Log.d("TEST", "Debug message")
    // Test code
}
```

### Breakpoints
Set breakpoints in Android Studio and run tests in debug mode.

### Test Reports
View detailed reports in `app/build/reports/tests/`

---

## References

- [Android Testing Guide](https://developer.android.com/training/testing)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [JUnit 4](https://junit.org/junit4/)
- [MockK](https://mockk.io/)
- [Coroutines Testing](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
