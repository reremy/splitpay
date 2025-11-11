# Data Models Documentation

Complete reference for all data models used in SplitPay.

---

## Overview

Data models are located in: `app/src/main/java/com/example/splitpay/data/model/`

All models are Kotlin data classes that map to Firestore documents and are used throughout the app for type-safe data handling.

---

## Core Models

### 1. User

**File**: `data/model/User.kt`

**Purpose**: Represents a user account

```kotlin
data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val qrCodeUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val friends: List<String> = emptyList(),
    val blockedUsers: List<String> = emptyList()
)
```

**Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `uid` | String | Unique user ID (matches Firebase Auth UID) |
| `fullName` | String | User's full name (e.g., "John Doe") |
| `username` | String | Unique username for friend discovery |
| `email` | String | User's email address |
| `phoneNumber` | String | User's phone number (with country code) |
| `profilePictureUrl` | String | Firebase Storage URL for profile picture |
| `qrCodeUrl` | String | Firebase Storage URL for QR code image |
| `createdAt` | Long | Account creation timestamp (milliseconds) |
| `friends` | List<String> | Array of friend UIDs (bilateral friendships) |
| `blockedUsers` | List<String> | Array of blocked user UIDs |

**Validation Rules**:
- `uid`: Must match Firebase Auth UID, non-empty
- `username`: Must be unique across all users, 3-20 characters, alphanumeric + underscore
- `email`: Valid email format
- `phoneNumber`: Valid phone number with country code
- `friends`: Can contain other users' UIDs only
- `blockedUsers`: Cannot contain self

**Firestore Collection**: `/users/{uid}`

---

### 2. Group

**File**: `data/model/Group.kt`

**Purpose**: Represents an expense-sharing group

```kotlin
data class Group(
    val id: String = "",
    val name: String = "",
    val createdByUid: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val iconIdentifier: String = "friends",
    val photoUrl: String = "",
    @get:PropertyName("isArchived")
    val isArchived: Boolean = false,
    val settledDate: Long? = null
)
```

**Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique group ID (matches Firestore document ID) |
| `name` | String | Group name (e.g., "Weekend Trip") |
| `createdByUid` | String | UID of user who created the group |
| `members` | List<String> | Array of member UIDs (includes creator) |
| `createdAt` | Long | Group creation timestamp (milliseconds) |
| `iconIdentifier` | String | Icon name for UI display (e.g., "trip", "home") |
| `photoUrl` | String | Firebase Storage URL for group photo (optional) |
| `isArchived` | Boolean | Whether group is archived (soft delete) |
| `settledDate` | Long? | Timestamp when all balances reached zero (nullable) |

**Icon Identifiers**:
- `friends` - Default icon
- `trip` - Travel/vacation
- `home` - Home/apartment
- `couple` - Couples
- `other` - Other categories

**Validation Rules**:
- `name`: 1-50 characters, non-empty
- `members`: Must include `createdByUid`, at least 1 member
- `iconIdentifier`: Must be from predefined list
- `settledDate`: Can only be set when all balances are zero

**Firestore Collection**: `/groups/{groupId}`

---

### 3. Expense

**File**: `data/model/Expense.kt`

**Purpose**: Represents an expense or payment

```kotlin
data class Expense(
    val id: String = "",
    val groupId: String? = null,
    val description: String = "",
    val totalAmount: Double = 0.0,
    val createdByUid: String = "",
    val date: Long = System.currentTimeMillis(),

    val splitType: String = "EQUALLY",
    val paidBy: List<ExpensePayer> = emptyList(),
    val participants: List<ExpenseParticipant> = emptyList(),

    val memo: String = "",
    val imageUrl: String = "",
    val category: String = "misc",

    val expenseType: String = ExpenseType.EXPENSE
)
```

**Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique expense ID (matches Firestore document ID) |
| `groupId` | String? | Group ID (null for friend-to-friend expenses) |
| `description` | String | Short description (e.g., "Dinner at restaurant") |
| `totalAmount` | Double | Total expense amount |
| `createdByUid` | String | UID of user who created the expense |
| `date` | Long | Expense date timestamp (milliseconds) |
| `splitType` | String | How to split: "EQUALLY", "EXACT_AMOUNTS", "PERCENTAGES", "SHARES" |
| `paidBy` | List<ExpensePayer> | Array of payers (who paid) |
| `participants` | List<ExpenseParticipant> | Array of participants (who owes) |
| `memo` | String | Additional notes (optional) |
| `imageUrl` | String | Firebase Storage URL for receipt image (optional) |
| `category` | String | Expense category (e.g., "food", "transport") |
| `expenseType` | String | "EXPENSE" or "PAYMENT" |

**Split Types**:

| Split Type | Description | Example |
|------------|-------------|---------|
| `EQUALLY` | Split total equally | $100 / 4 people = $25 each |
| `EXACT_AMOUNTS` | Specify exact amounts | A: $50, B: $30, C: $20 |
| `PERCENTAGES` | Specify percentages (total 100%) | A: 50%, B: 30%, C: 20% |
| `SHARES` | Specify shares | A: 2 shares, B: 1 share |

**Categories**:
- `misc` - General/Miscellaneous
- `food` - Food & Drink
- `groceries` - Groceries
- `shopping` - Shopping
- `entertainment` - Entertainment
- `transport` - Transportation
- `utilities` - Utilities
- `rent` - Rent
- `travel` - Travel
- `healthcare` - Healthcare
- `education` - Education
- `payment` - Payment (for settle-up)

**Expense Types**:

```kotlin
object ExpenseType {
    const val EXPENSE = "EXPENSE"  // Regular expense
    const val PAYMENT = "PAYMENT"  // Settlement payment
}
```

**Validation Rules**:
- `totalAmount`: Must be positive, > 0
- `description`: 1-100 characters, non-empty
- `paidBy`: At least one payer, sum of `paidAmount` should equal `totalAmount`
- `participants`: At least one participant
- For `PERCENTAGES`: sum of all percentages must equal 100%
- For `EXACT_AMOUNTS`: sum of all amounts must equal `totalAmount`

**Firestore Collection**: `/expenses/{expenseId}`

---

### 4. ExpensePayer

**File**: `data/model/Expense.kt`

**Purpose**: Represents a user who paid for an expense

```kotlin
data class ExpensePayer(
    val uid: String = "",
    val paidAmount: Double = 0.0
)
```

**Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `uid` | String | User ID of the payer |
| `paidAmount` | Double | Amount this user paid |

**Usage**:
- Embedded in Expense model
- Supports multiple payers (split payment scenario)
- Example: Two friends pay $50 each for a $100 dinner

---

### 5. ExpenseParticipant

**File**: `data/model/Expense.kt`

**Purpose**: Represents a user's share in an expense

```kotlin
data class ExpenseParticipant(
    val uid: String = "",
    val owesAmount: Double = 0.0,
    val initialSplitValue: Double = 0.0
)
```

**Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `uid` | String | User ID of the participant |
| `owesAmount` | Double | Final calculated amount this user owes |
| `initialSplitValue` | Double | Original input value (amount/percentage/share) |

**Usage**:
- Embedded in Expense model
- `initialSplitValue` interpretation depends on `splitType`:
  - `EQUALLY`: Equal split amount (e.g., 25.00)
  - `EXACT_AMOUNTS`: Exact amount (e.g., 50.00)
  - `PERCENTAGES`: Percentage value (e.g., 33.33)
  - `SHARES`: Number of shares (e.g., 2)
- `owesAmount`: Always the final amount owed (calculated by `CalculateSplitUseCase`)

---

### 6. Activity

**File**: `data/model/Activity.kt`

**Purpose**: Represents an activity feed entry

```kotlin
data class Activity(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val activityType: String = ActivityType.EXPENSE_ADDED.name,
    val actorUid: String = "",
    val actorName: String = "",
    val involvedUids: List<String> = emptyList(),
    val groupId: String? = null,
    val groupName: String? = null,
    val entityId: String? = null,
    val displayText: String? = null,
    val totalAmount: Double? = null,
    val financialImpacts: Map<String, Double>? = null
)
```

**Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique activity ID |
| `timestamp` | Long | When activity occurred (milliseconds) |
| `activityType` | String | Type of activity (see ActivityType enum) |
| `actorUid` | String | UID of user who performed the action |
| `actorName` | String | Display name of actor |
| `involvedUids` | List<String> | All user UIDs relevant to this activity (for querying) |
| `groupId` | String? | Related group ID (if applicable) |
| `groupName` | String? | Display name of group |
| `entityId` | String? | ID of related entity (expense ID, member UID, etc.) |
| `displayText` | String? | Main subject text (e.g., expense description) |
| `totalAmount` | Double? | Amount involved (for expenses/payments) |
| `financialImpacts` | Map<String, Double>? | Per-user financial impact (UID → amount) |

**ActivityType Enum**:

```kotlin
enum class ActivityType {
    // Group Management
    GROUP_CREATED,
    GROUP_DELETED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    MEMBER_LEFT,

    // Expense & Payment
    EXPENSE_ADDED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    PAYMENT_MADE,
    PAYMENT_UPDATED,
    PAYMENT_DELETED
}
```

**Financial Impacts**:
- Map of UID to net change in balance
- Positive value: user gets back money
- Negative value: user owes money
- Example: `{"uid1": -50.0, "uid2": 50.0}` means uid1 owes $50 to uid2

**Firestore Collection**: `/activities/{activityId}`

---

## Computed Models

### 7. FriendWithBalance

**File**: `data/model/FriendWithBalance.kt`

**Purpose**: Combines friend data with calculated balance

```kotlin
data class FriendWithBalance(
    val user: User,
    val balance: Double  // Positive = they owe you, Negative = you owe them
)
```

**Usage**:
- UI display in Friends list
- Not stored in Firestore (computed on-the-fly)
- Balance calculated from all expenses involving both users

---

### 8. GroupWithBalance

**File**: `data/model/GroupWithBalance.kt`

**Purpose**: Combines group data with calculated balances

```kotlin
data class GroupWithBalance(
    val group: Group,
    val totalBalance: Double,           // Your net balance in the group
    val memberBalances: Map<String, Double>  // Per-member balances
)
```

**Usage**:
- UI display in Groups list
- Not stored in Firestore (computed on-the-fly)
- Balances calculated from all group expenses

---

## Helper Models

### 9. Participant

**File**: `data/model/Participant.kt`

**Purpose**: UI helper for participant selection

```kotlin
data class Participant(
    val uid: String,
    val name: String,
    val profilePictureUrl: String = "",
    val isSelected: Boolean = false
)
```

**Usage**:
- Used in UI for selecting participants when creating expenses
- Not stored in Firestore

---

### 10. Payer

**File**: `data/model/Payer.kt`

**Purpose**: UI helper for payer selection

```kotlin
data class Payer(
    val uid: String,
    val name: String,
    val profilePictureUrl: String = "",
    val amountPaid: Double = 0.0
)
```

**Usage**:
- Used in UI for selecting payers when creating expenses
- Converted to `ExpensePayer` before saving

---

## Data Model Relationships

### User ↔ User (Friends)
```
User.friends: List<String>  // Array of friend UIDs
```

### User ↔ Group (Membership)
```
Group.members: List<String>  // Array of member UIDs
```

### Group ↔ Expense
```
Expense.groupId: String?  // Reference to Group
```

### User ↔ Expense (Participation)
```
Expense.paidBy: List<ExpensePayer>      // Who paid
Expense.participants: List<ExpenseParticipant>  // Who owes
```

### Activity → Multiple Entities
```
Activity.entityId: String?       // Reference to Expense, User, or Group
Activity.activityType: String    // Determines entity type
```

---

## Data Validation

### Client-Side Validation

Validation is performed in ViewModels before saving to Firestore:

```kotlin
fun validateExpense(expense: Expense): ValidationResult {
    if (expense.description.isBlank()) {
        return ValidationResult.Error("Description cannot be empty")
    }
    if (expense.totalAmount <= 0) {
        return ValidationResult.Error("Amount must be positive")
    }
    if (expense.paidBy.isEmpty()) {
        return ValidationResult.Error("At least one payer required")
    }
    if (expense.participants.isEmpty()) {
        return ValidationResult.Error("At least one participant required")
    }
    // More validations...
    return ValidationResult.Success
}
```

### Server-Side Validation

Firestore Security Rules enforce validation on the server:

```javascript
function isValidExpense(expense) {
  return expense.totalAmount > 0
      && expense.description.size() > 0
      && expense.paidBy.size() > 0
      && expense.participants.size() > 0;
}
```

---

## Type Converters

### Firestore to Kotlin

Firestore automatically converts documents to data classes using:

```kotlin
val expense = snapshot.toObject(Expense::class.java)
```

**Requirements**:
- Data class must have default values for all fields
- Field names must match Firestore document fields (or use `@PropertyName`)

### Kotlin to Firestore

```kotlin
val expenseMap = expense.toMap()  // Or use set() directly
firestore.collection("expenses").document(id).set(expense)
```

---

## Default Values

All models use default values to support Firestore deserialization:

```kotlin
data class User(
    val uid: String = "",  // Empty string default
    val createdAt: Long = System.currentTimeMillis(),  // Current time default
    val friends: List<String> = emptyList()  // Empty list default
)
```

**Why?**
- Firestore requires no-argument constructor
- Allows safe deserialization even if fields are missing
- Provides sensible fallbacks

---

## Serialization Annotations

### @PropertyName

Used when Kotlin property name differs from Firestore field name:

```kotlin
@get:PropertyName("isArchived")
val isArchived: Boolean = false
```

**Reason**: Boolean fields starting with "is" need special handling in Firestore.

### @Exclude

Exclude fields from Firestore serialization:

```kotlin
@Exclude
val isSelected: Boolean = false  // UI state, not stored
```

---

## Best Practices

1. **Immutability**: Use `data class` with `val` (immutable properties)
2. **Default Values**: Always provide defaults for Firestore compatibility
3. **Validation**: Validate data before saving to Firestore
4. **Nullable Fields**: Use nullable types (`String?`) for optional fields
5. **Type Safety**: Avoid stringly-typed data (use enums/sealed classes when possible)
6. **Documentation**: Document complex fields and relationships
7. **Consistency**: Keep naming conventions consistent across models

---

## References

- [Kotlin Data Classes](https://kotlinlang.org/docs/data-classes.html)
- [Firestore Data Model](https://firebase.google.com/docs/firestore/data-model)
- [Firestore Custom Objects](https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects)
