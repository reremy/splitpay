# AI Context - Quick Reference

**This document is designed specifically for AI assistants like Claude to quickly understand the SplitPay project.**

---

## Project Elevator Pitch

**SplitPay** is an Android expense-splitting app built with Kotlin and Jetpack Compose. Users can track shared expenses with friends and groups, split bills in multiple ways, and settle up with each other. Think "Splitwise clone" but built with modern Android technologies.

---

## Tech Stack at a Glance

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 100% Kotlin |
| UI Framework | Jetpack Compose | Latest |
| Design System | Material Design 3 | Latest |
| Architecture | MVVM | - |
| Backend | Firebase Suite | BOM-managed |
| Auth | Firebase Authentication | Email/Password |
| Database | Cloud Firestore | Real-time NoSQL |
| Storage | Firebase Storage | Images/Files |
| Image Loading | Coil | 2.5.0 |
| Navigation | Navigation Compose | Jetpack |
| Async | Kotlin Coroutines | Latest |
| Build System | Gradle (Kotlin DSL) | Latest |
| Min SDK | 24 (Android 7.0) | - |
| Target SDK | 36 | - |

---

## Project Structure (Key Directories)

```
app/src/main/java/com/example/splitpay/
├── data/
│   ├── model/              # Data classes: User, Expense, Group, Activity
│   └── repository/         # Data layer: UserRepository, ExpenseRepository, etc.
├── domain/
│   └── usecase/            # Business logic: CalculateSplitUseCase
├── ui/
│   ├── login/              # Login screen
│   ├── signup/             # Sign up screen
│   ├── home/               # Main hub (bottom nav)
│   ├── friends/            # Friend list & management
│   ├── friendsDetail/      # Friend detail with balances
│   ├── groups/             # Group list & management
│   ├── expense/            # Add/edit expense screens
│   ├── settleUp/           # Payment recording
│   ├── activity/           # Activity feed
│   ├── profile/            # User profile
│   ├── common/             # Reusable composables
│   └── theme/              # Material 3 theming
├── navigation/
│   ├── Navigation.kt       # NavHost setup
│   └── Screen.kt           # Route constants
└── logger/
    └── Logger.kt           # Custom logging
```

---

## Core Data Models

### User
```kotlin
data class User(
    val uid: String,           // Firebase Auth UID
    val fullName: String,
    val username: String,      // Unique
    val email: String,
    val phoneNumber: String,
    val profilePictureUrl: String,
    val qrCodeUrl: String,
    val friends: List<String>, // Friend UIDs
    val blockedUsers: List<String>
)
```

### Group
```kotlin
data class Group(
    val id: String,
    val name: String,
    val createdByUid: String,
    val members: List<String>,  // Member UIDs
    val iconIdentifier: String, // e.g., "trip", "home"
    val photoUrl: String,
    val isArchived: Boolean
)
```

### Expense
```kotlin
data class Expense(
    val id: String,
    val groupId: String?,       // Null for friend-to-friend
    val description: String,
    val totalAmount: Double,
    val createdByUid: String,
    val date: Long,            // Timestamp
    val splitType: String,     // "EQUALLY", "EXACT_AMOUNTS", "PERCENTAGES", "SHARES"
    val paidBy: List<ExpensePayer>,
    val participants: List<ExpenseParticipant>,
    val category: String,      // "food", "transport", etc.
    val expenseType: String    // "EXPENSE" or "PAYMENT"
)
```

### Activity
```kotlin
data class Activity(
    val id: String,
    val timestamp: Long,
    val activityType: String,  // "EXPENSE_ADDED", "PAYMENT_MADE", etc.
    val actorUid: String,
    val involvedUids: List<String>,  // For querying user-specific activities
    val groupId: String?,
    val displayText: String?,
    val totalAmount: Double?,
    val financialImpacts: Map<String, Double>?
)
```

---

## Firestore Collections

| Collection | Document ID | Purpose |
|------------|-------------|---------|
| `/users` | Firebase Auth UID | User profiles |
| `/groups` | Auto-generated | Groups |
| `/expenses` | Auto-generated | Expenses & payments |
| `/activities` | Auto-generated | Activity feed entries |

**Key Query**: Activities filtered by `involvedUids` array contains current user UID.

---

## Common File Locations

### When user asks about...

**Authentication**:
- `ui/login/LoginScreen.kt`
- `ui/signup/SignUpScreen.kt`
- `data/repository/UserRepository.kt` (signUp, login, logout methods)

**Expenses**:
- `ui/expense/AddExpenseScreen.kt` (add/edit)
- `ui/expense/ExpenseDetailScreen.kt`
- `data/repository/ExpenseRepository.kt`
- `domain/usecase/CalculateSplitUseCase.kt` (split calculations)

**Groups**:
- `ui/groups/GroupsScreen.kt` (list)
- `ui/groups/GroupDetailScreen.kt`
- `ui/groups/CreateGroupScreen.kt`
- `data/repository/GroupsRepository.kt`

**Friends**:
- `ui/friends/FriendsScreen.kt`
- `ui/friendsDetail/FriendDetailScreen.kt`
- `ui/friends/AddFriendScreen.kt`

**Activity Feed**:
- `ui/activity/ActivityScreen.kt`
- `data/repository/ActivityRepository.kt`

**Navigation**:
- `navigation/Navigation.kt` (NavHost)
- `navigation/Screen.kt` (route constants)

**Data Models**:
- `data/model/User.kt`
- `data/model/Expense.kt`
- `data/model/Group.kt`
- `data/model/Activity.kt`

**Reusable UI**:
- `ui/common/` (buttons, dialogs, cards, etc.)
- `ui/theme/` (colors, typography, theme)

---

## Important Patterns & Conventions

### 1. Repository Pattern
All Firebase operations go through repositories:
```kotlin
class ExpenseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun addExpense(expense: Expense): Result<String> { ... }
}
```

### 2. Result<T> for Error Handling
```kotlin
suspend fun getUser(uid: String): Result<User> {
    return try {
        // Firebase operation
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 3. Coroutines with Firebase
```kotlin
val user = usersCollection.document(uid).get().await()  // Uses kotlinx-coroutines-play-services
```

### 4. Real-time Listeners with Flow
```kotlin
fun observeExpenses(groupId: String): Flow<List<Expense>> = callbackFlow {
    val listener = expensesCollection
        .whereEqualTo("groupId", groupId)
        .addSnapshotListener { snapshot, error ->
            // ...
            trySend(expenses)
        }
    awaitClose { listener.remove() }
}
```

### 5. State Management in ViewModels
```kotlin
class ExpenseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            // Update state
        }
    }
}
```

### 6. Compose UI State Hoisting
```kotlin
@Composable
fun ExpenseScreen(
    uiState: ExpenseUiState,
    onEvent: (ExpenseEvent) -> Unit
) {
    // Stateless composable
}
```

---

## Known Gotchas

1. **groupId can be null**: For friend-to-friend expenses (outside groups)
2. **involvedUids**: Critical for activity queries - includes ALL users relevant to the activity
3. **expenseType**: "EXPENSE" for regular expenses, "PAYMENT" for settlements
4. **Balance calculation**: Not stored - computed on-the-fly from expenses
5. **Bilateral friendship**: Both users must have each other in `friends` array
6. **QR codes**: Generated and stored in Firebase Storage, contain username

---

## Firebase Configuration

- **google-services.json**: In `app/` folder (gitignored)
- **Security Rules**: See `FIRESTORE_SCHEMA.md` for production rules
- **Storage paths**:
  - `profile_pictures/{userId}/profile.jpg`
  - `qr_codes/{userId}/qrcode.png`
  - `group_photos/{groupId}/photo.jpg`
  - `expense_images/{expenseId}/receipt.jpg`

---

## Common Tasks

### Add a new screen
1. Create composable in `ui/{feature}/`
2. Create ViewModel if needed
3. Add route to `navigation/Screen.kt`
4. Add to NavHost in `navigation/Navigation.kt`

### Add a new data model
1. Create data class in `data/model/`
2. Add default values (for Firestore)
3. Update Firestore collection if needed
4. Update security rules

### Add a new Firebase operation
1. Add method to appropriate repository
2. Use `suspend fun` and `Result<T>`
3. Use `.await()` for Firebase Tasks
4. Log with custom Logger

### Add a new feature
1. Plan data model changes
2. Update Firestore schema
3. Create/update repository methods
4. Create UI screens
5. Add navigation
6. Write tests
7. Update documentation

---

## Testing

- **Unit tests**: `app/src/test/`
- **Instrumented tests**: `app/src/androidTest/`
- Run: `./gradlew test` or `./gradlew connectedAndroidTest`

---

## Documentation Files

| File | Purpose |
|------|---------|
| `README.md` | Project overview |
| `docs/ARCHITECTURE.md` | Architecture deep dive |
| `docs/FIRESTORE_SCHEMA.md` | Database schema |
| `docs/SETUP.md` | Development setup |
| `docs/FEATURES.md` | Feature documentation |
| `docs/FIREBASE_SERVICES.md` | Firebase integration |
| `docs/DATA_MODELS.md` | Data model reference |
| `docs/UI_COMPONENTS.md` | Reusable components |
| `docs/TESTING.md` | Testing guide |
| `CONTRIBUTING.md` | Contribution guidelines |
| `CHANGELOG.md` | Version history |

---

## Quick Debugging Tips

1. **Check Logcat** for custom logs (logD, logI, logE)
2. **Firebase Console**: View Auth users, Firestore data, Storage files
3. **Compose Preview**: Use `@Preview` for rapid UI iteration
4. **Database Inspector**: View Firestore in Android Studio
5. **Layout Inspector**: Inspect Compose hierarchy

---

## When Suggesting Changes

### Always Consider:
- [ ] Does this follow MVVM architecture?
- [ ] Are Firebase operations in repositories?
- [ ] Is error handling using Result<T>?
- [ ] Are coroutines used for async operations?
- [ ] Is state hoisted in Compose?
- [ ] Are data models immutable (val)?
- [ ] Are Firestore security rules updated?
- [ ] Are tests added/updated?
- [ ] Is documentation updated?

---

## Contact & Resources

- **Repository**: [GitHub link]
- **Documentation**: `/docs` folder
- **Firebase Console**: [Link to Firebase project]
- **Design System**: Material Design 3

---

**Last Updated**: 2025-11-11

This document provides the essential context for AI assistants to quickly understand and work with the SplitPay codebase.
