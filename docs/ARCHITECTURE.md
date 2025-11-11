# Architecture Documentation

## Overview

SplitPay follows clean architecture principles with a clear separation of concerns using the **MVVM (Model-View-ViewModel)** pattern combined with the **Repository pattern** for data management.

---

## Architecture Layers

### 1. UI Layer (Presentation)
**Location**: `app/src/main/java/com/example/splitpay/ui/`

The presentation layer is built with **Jetpack Compose** and follows the unidirectional data flow pattern.

#### Components:
- **Composables**: UI screens and components
- **ViewModels**: State management and business logic coordination
- **UI State**: Immutable data classes representing screen state
- **UI Events**: User interactions and one-time events

#### Key Screens:
```
ui/
├── welcome/            # Onboarding screen
├── login/              # Authentication
├── signup/             # User registration
├── home/               # Main navigation hub (bottom navigation)
├── friends/            # Friend list and management
├── friendsDetail/      # Individual friend balances and expenses
├── groups/             # Group list and creation
├── expense/            # Add/edit expenses
├── settleUp/           # Payment recording
├── activity/           # Activity feed
├── profile/            # User profile management
├── theme/              # Material Design 3 theming
└── common/             # Reusable components (buttons, dialogs, etc.)
```

#### ViewModel Responsibilities:
- Expose UI state via StateFlow/State
- Handle user events
- Coordinate with repositories
- Transform data for UI consumption
- Manage loading/error states

**Example Pattern**:
```kotlin
class ExpenseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    fun onEvent(event: ExpenseEvent) {
        // Handle user events
    }
}
```

---

### 2. Domain Layer (Business Logic)
**Location**: `app/src/main/java/com/example/splitpay/domain/`

Contains business logic and use cases that are independent of Android framework.

#### Use Cases:
- **CalculateSplitUseCase**: Complex expense splitting calculations
  - Equal split
  - Exact amounts
  - Percentage-based
  - Share-based
  - Multiple payer support

#### Responsibilities:
- Encapsulate complex business rules
- Coordinate multiple repository operations
- Transform domain models
- Validate business constraints

**File**: `domain/usecase/CalculateSplitUseCase.kt`

---

### 3. Data Layer
**Location**: `app/src/main/java/com/example/splitpay/data/`

Manages data sources and provides a clean API to the rest of the app.

#### 3.1 Models
**Location**: `data/model/`

Core data classes representing business entities:
- **User**: User profile and authentication data
- **Expense**: Expense details, split information, participants
- **Group**: Group metadata and members
- **Activity**: Activity feed entries
- **ExpenseParticipant**: Individual user's share in an expense
- **ExpensePayer**: User who paid for an expense
- **FriendWithBalance**: Friend data combined with balance calculations
- **GroupWithBalance**: Group data combined with balance calculations

See [DATA_MODELS.md](DATA_MODELS.md) for detailed schemas.

#### 3.2 Repositories
**Location**: `data/repository/`

Abstract data sources and provide clean APIs:

- **UserRepository**: User authentication and profile management
  - Sign up, login, logout
  - Profile updates
  - Friend management
  - User search

- **ExpenseRepository**: Expense CRUD operations
  - Create/read/update/delete expenses
  - Query by group or user
  - Handle payments vs expenses

- **GroupsRepository**: Group management
  - Create/edit/delete groups
  - Member management
  - Archive groups

- **ActivityRepository**: Activity feed management
  - Log activities
  - Query user-specific activities
  - Real-time updates

- **FileStorageRepository**: Image and file handling
  - Profile picture uploads
  - QR code generation and storage
  - Expense image uploads

#### Repository Pattern Benefits:
- Single source of truth
- Testable data layer
- Abstraction over Firebase
- Consistent error handling
- Offline capability foundation

---

## Navigation Architecture

**Location**: `app/src/main/java/com/example/splitpay/navigation/`

### Navigation System
Built with **Jetpack Navigation Compose** for type-safe navigation.

#### Key Files:
- **Navigation.kt**: Navigation graph setup and route definitions
- **Screen.kt**: Centralized screen route constants
- **NavTransitions.kt**: Custom screen transition animations

#### Navigation Pattern:
```kotlin
object Screen {
    const val Welcome = "welcome"
    const val Home = "Home"
    const val GroupDetail = "group_detail/{groupId}"
    const val AddExpenseRoute = "add_expense?groupId={groupId}&expenseId={expenseId}"
}
```

#### Home Navigation:
Bottom navigation with three main tabs:
1. **Friends**: Friend list with balances
2. **Groups**: Group list with overall balances
3. **Activity**: Chronological activity feed

---

## State Management

### UI State Pattern
Each screen defines an immutable UI state data class:

```kotlin
data class ExpenseUiState(
    val isLoading: Boolean = false,
    val expense: Expense? = null,
    val error: String? = null,
    val participants: List<ExpenseParticipant> = emptyList()
)
```

### Event Handling
User interactions are modeled as sealed classes or events:

```kotlin
sealed class ExpenseEvent {
    data class UpdateAmount(val amount: Double) : ExpenseEvent()
    data class AddParticipant(val uid: String) : ExpenseEvent()
    object SaveExpense : ExpenseEvent()
}
```

### State Flow
- **StateFlow**: For UI state that survives configuration changes
- **Flow**: For reactive data streams from repositories
- **callbackFlow**: For Firebase real-time listeners

---

## Data Flow

### Unidirectional Data Flow (UDF)

```
┌─────────────────────────────────────────────────┐
│                  Composable                      │
│  (Observes state, emits events)                 │
└────────────┬───────────────────────▲────────────┘
             │ User Events           │ UI State
             │                       │
             ▼                       │
┌────────────────────────────────────┴────────────┐
│                  ViewModel                       │
│  (Processes events, updates state)              │
└────────────┬────────────────────────▲───────────┘
             │ Data requests          │ Data
             │                        │
             ▼                        │
┌────────────────────────────────────┴────────────┐
│                 Repository                       │
│  (Manages data sources)                         │
└────────────┬────────────────────────▲───────────┘
             │ Queries/Updates        │ Results
             │                        │
             ▼                        │
┌────────────────────────────────────┴────────────┐
│              Firebase Services                   │
│  (Firestore, Auth, Storage)                     │
└─────────────────────────────────────────────────┘
```

### Example Flow: Adding an Expense

1. **UI**: User fills expense form and taps "Save"
2. **Event**: `ExpenseEvent.SaveExpense` emitted to ViewModel
3. **ViewModel**:
   - Validates input
   - Calls `CalculateSplitUseCase` to compute splits
   - Calls `ExpenseRepository.addExpense()`
4. **Repository**:
   - Writes to Firestore
   - Logs activity
   - Returns result
5. **ViewModel**: Updates UI state (success/error)
6. **UI**: Recomposes to show updated state

---

## Dependency Management

### Dependency Injection (Manual)
Currently using manual dependency injection with default parameters:

```kotlin
class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
)
```

**Benefits**:
- Simple for small-medium apps
- Easy to test with constructor injection
- No additional library dependencies

**Future**: Consider Hilt/Dagger for larger scale.

---

## Threading & Concurrency

### Kotlin Coroutines
All asynchronous operations use coroutines:

- **Dispatchers.IO**: Network and database operations
- **Dispatchers.Main**: UI updates
- **viewModelScope**: Automatic cancellation

### Firebase Integration
- **kotlinx-coroutines-play-services**: Converts Firebase Tasks to coroutines
- **await()**: Extension function for Task<T> to suspend function

Example:
```kotlin
suspend fun getUser(uid: String): Result<User> = withContext(Dispatchers.IO) {
    try {
        val snapshot = usersCollection.document(uid).get().await()
        val user = snapshot.toObject(User::class.java)
        Result.success(user ?: throw Exception("User not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## Error Handling

### Repository Layer
- Returns `Result<T>` for error-safe operations
- Logs errors using custom Logger
- Provides meaningful error messages

### ViewModel Layer
- Catches exceptions
- Updates UI state with error messages
- Handles loading states

### UI Layer
- Displays error messages via Snackbars or Dialogs
- Provides retry mechanisms
- Graceful degradation

---

## Logging

**Location**: `app/src/main/java/com/example/splitpay/logger/Logger.kt`

Custom logging utilities for consistent logging across the app:
- `logD()`: Debug logs
- `logI()`: Info logs
- `logE()`: Error logs
- `logW()`: Warning logs

Includes automatic tagging and formatting.

---

## Testing Strategy

### Unit Tests
- **Location**: `app/src/test/`
- Test ViewModels with mock repositories
- Test use cases with test data
- Test utility functions

### Instrumented Tests
- **Location**: `app/src/androidTest/`
- Compose UI tests
- Repository integration tests with Firebase Emulator
- End-to-end user flows

See [TESTING.md](TESTING.md) for detailed testing guidelines.

---

## Security Considerations

### Authentication
- Firebase Authentication for secure user management
- Email/password validation
- Session management

### Data Access
- Firestore Security Rules enforce user-specific data access
- Users can only access their own data and groups they belong to
- Activity feed filtered by `involvedUids`

### Image Storage
- Firebase Storage with security rules
- User-specific upload paths
- File size and type validation

See [FIREBASE_STORAGE_SETUP.md](../FIREBASE_STORAGE_SETUP.md) for storage security rules.

---

## Performance Optimizations

### Compose Performance
- **remember**: Cache computations across recompositions
- **LazyColumn**: Efficient list rendering
- **derivedStateOf**: Optimize expensive calculations
- Avoid unnecessary recompositions

### Database Performance
- **Firestore Indexing**: Composite indexes for complex queries
- **Pagination**: Load data in chunks (future enhancement)
- **Real-time listeners**: Only where needed
- **Offline persistence**: Enabled by default in Firestore

### Image Loading
- **Coil**: Efficient image loading with caching
- Memory and disk caching
- Placeholder and error images

---

## Future Enhancements

### Potential Improvements
1. **Offline-first architecture**: Enhanced offline support with sync
2. **Hilt/Dagger**: Dependency injection framework
3. **Room Database**: Local caching layer
4. **WorkManager**: Background sync and reminders
5. **Paging 3**: Efficient pagination for large datasets
6. **Multi-module architecture**: Feature modules for scalability
7. **Baseline Profiles**: Startup performance optimization

---

## Architecture Decisions

### Why MVVM?
- Clean separation of concerns
- Testable business logic
- Survives configuration changes
- Well-supported by Android Jetpack

### Why Jetpack Compose?
- Declarative UI paradigm
- Less boilerplate than XML
- Type-safe
- Modern Android best practice
- Excellent integration with ViewModel

### Why Firebase?
- Real-time synchronization
- Automatic scaling
- Built-in authentication
- Quick development
- Offline support

### Why Repository Pattern?
- Abstracts data sources
- Single source of truth
- Easier to test
- Flexibility to add local database later

---

## References

- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
