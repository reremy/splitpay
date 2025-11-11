# Firestore Database Schema

## Overview

SplitPay uses **Cloud Firestore** as its primary database. The schema is designed for real-time updates, efficient queries, and scalability.

---

## Collections Structure

```
Firestore Root
├── users/                      (Collection)
│   └── {userId}/               (Document)
│
├── groups/                     (Collection)
│   └── {groupId}/              (Document)
│
├── expenses/                   (Collection)
│   └── {expenseId}/            (Document)
│
└── activities/                 (Collection)
    └── {activityId}/           (Document)
```

---

## 1. Users Collection

**Collection Path**: `/users`

Stores user profiles and account information.

### Document Structure

**Document ID**: Firebase Auth UID

```typescript
{
  uid: string                    // Firebase Auth user ID
  fullName: string               // User's full name
  username: string               // Unique username
  email: string                  // User's email address
  phoneNumber: string            // User's phone number
  profilePictureUrl: string      // Firebase Storage URL for profile picture
  qrCodeUrl: string              // Firebase Storage URL for QR code image
  createdAt: number              // Timestamp (milliseconds)
  friends: string[]              // Array of friend UIDs
  blockedUsers: string[]         // Array of blocked user UIDs
}
```

### Example Document

```json
{
  "uid": "abc123xyz",
  "fullName": "John Doe",
  "username": "johndoe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1234567890",
  "profilePictureUrl": "https://firebasestorage.googleapis.com/.../profile.jpg",
  "qrCodeUrl": "https://firebasestorage.googleapis.com/.../qrcode.png",
  "createdAt": 1678901234567,
  "friends": ["uid1", "uid2", "uid3"],
  "blockedUsers": ["uid4"]
}
```

### Key Fields Explained

- **friends**: List of user IDs representing bilateral friendships
- **blockedUsers**: Users blocked by this user (prevents friend requests, visibility)
- **qrCodeUrl**: Generated QR code containing username for easy friend discovery

### Queries

```kotlin
// Find user by username
usersCollection.whereEqualTo("username", username).get()

// Get user profile
usersCollection.document(uid).get()

// Get all friends of a user
usersCollection.document(uid).get()
  .then { user.friends.forEach { ... } }
```

### Indexes Required
- **username**: Single field index (for searching)

---

## 2. Groups Collection

**Collection Path**: `/groups`

Stores group information for multi-person expense tracking.

### Document Structure

**Document ID**: Auto-generated Firestore ID

```typescript
{
  id: string                     // Group ID (matches document ID)
  name: string                   // Group name
  createdByUid: string           // UID of user who created the group
  members: string[]              // Array of member UIDs (includes creator)
  createdAt: number              // Timestamp (milliseconds)
  iconIdentifier: string         // Icon name (e.g., "friends", "trip", "home")
  photoUrl: string               // Firebase Storage URL for group photo
  isArchived: boolean            // Whether group is archived
  settledDate: number | null     // Timestamp when all balances reached zero
}
```

### Example Document

```json
{
  "id": "group_abc123",
  "name": "Weekend Trip",
  "createdByUid": "abc123xyz",
  "members": ["abc123xyz", "def456uvw", "ghi789rst"],
  "createdAt": 1678901234567,
  "iconIdentifier": "trip",
  "photoUrl": "https://firebasestorage.googleapis.com/.../group.jpg",
  "isArchived": false,
  "settledDate": null
}
```

### Key Fields Explained

- **members**: All users in the group (always includes creator)
- **iconIdentifier**: Predefined icon name for UI display
- **isArchived**: Soft delete - keeps data but hides from active lists
- **settledDate**: Tracks when group reached zero balance (useful for analytics)

### Queries

```kotlin
// Get all groups for a user
groupsCollection.whereArrayContains("members", currentUserId).get()

// Get active groups only
groupsCollection
  .whereArrayContains("members", currentUserId)
  .whereEqualTo("isArchived", false)
  .get()

// Get archived groups
groupsCollection
  .whereArrayContains("members", currentUserId)
  .whereEqualTo("isArchived", true)
  .get()
```

### Indexes Required
- **members + isArchived**: Composite index

---

## 3. Expenses Collection

**Collection Path**: `/expenses`

Stores all expenses and payments.

### Document Structure

**Document ID**: Auto-generated Firestore ID

```typescript
{
  id: string                         // Expense ID (matches document ID)
  groupId: string | null             // Group ID (null for friend-to-friend)
  description: string                // Expense description
  totalAmount: number                // Total expense amount
  createdByUid: string               // UID of user who created expense
  date: number                       // Expense date (timestamp in milliseconds)

  splitType: string                  // "EQUALLY" | "EXACT_AMOUNTS" | "PERCENTAGES" | "SHARES"
  paidBy: ExpensePayer[]             // Array of payers
  participants: ExpenseParticipant[] // Array of participants (who owes)

  memo: string                       // Additional notes
  imageUrl: string                   // Firebase Storage URL for receipt image
  category: string                   // Category (e.g., "food", "groceries", "misc")

  expenseType: string                // "EXPENSE" | "PAYMENT"
}
```

### Sub-structures

#### ExpensePayer
```typescript
{
  uid: string          // User ID of payer
  paidAmount: number   // Amount this user paid
}
```

#### ExpenseParticipant
```typescript
{
  uid: string              // User ID of participant
  owesAmount: number       // Final amount this user owes
  initialSplitValue: number // Original input value (amount/percentage/share)
}
```

### Example Document (Expense)

```json
{
  "id": "exp_abc123",
  "groupId": "group_xyz789",
  "description": "Dinner at restaurant",
  "totalAmount": 150.00,
  "createdByUid": "abc123xyz",
  "date": 1678905678901,
  "splitType": "EQUALLY",
  "paidBy": [
    {
      "uid": "abc123xyz",
      "paidAmount": 150.00
    }
  ],
  "participants": [
    {
      "uid": "abc123xyz",
      "owesAmount": 50.00,
      "initialSplitValue": 50.00
    },
    {
      "uid": "def456uvw",
      "owesAmount": 50.00,
      "initialSplitValue": 50.00
    },
    {
      "uid": "ghi789rst",
      "owesAmount": 50.00,
      "initialSplitValue": 50.00
    }
  ],
  "memo": "Celebrated Alice's birthday",
  "imageUrl": "https://firebasestorage.googleapis.com/.../receipt.jpg",
  "category": "food",
  "expenseType": "EXPENSE"
}
```

### Example Document (Payment)

```json
{
  "id": "pay_def456",
  "groupId": "group_xyz789",
  "description": "Payment from Bob to Alice",
  "totalAmount": 50.00,
  "createdByUid": "def456uvw",
  "date": 1678909876543,
  "splitType": "EQUALLY",
  "paidBy": [
    {
      "uid": "def456uvw",
      "paidAmount": 50.00
    }
  ],
  "participants": [
    {
      "uid": "abc123xyz",
      "owesAmount": -50.00,
      "initialSplitValue": -50.00
    }
  ],
  "memo": "Settling dinner expense",
  "imageUrl": "",
  "category": "payment",
  "expenseType": "PAYMENT"
}
```

### Key Fields Explained

- **groupId**: `null` for friend-to-friend expenses (outside groups)
- **splitType**: Determines how to interpret `initialSplitValue`
  - `EQUALLY`: Split total equally among participants
  - `EXACT_AMOUNTS`: `initialSplitValue` is exact amount each person owes
  - `PERCENTAGES`: `initialSplitValue` is percentage (0-100)
  - `SHARES`: `initialSplitValue` is number of shares
- **paidBy**: Supports multiple payers for complex scenarios
- **expenseType**:
  - `EXPENSE`: Regular expense
  - `PAYMENT`: Settlement payment (reduces balances)

### Queries

```kotlin
// Get all expenses for a group
expensesCollection
  .whereEqualTo("groupId", groupId)
  .orderBy("date", Query.Direction.DESCENDING)
  .get()

// Get friend-to-friend expenses (non-group)
expensesCollection
  .whereEqualTo("groupId", null)
  .whereArrayContains("participants.uid", userId)
  .get()

// Get expenses by category
expensesCollection
  .whereEqualTo("category", "food")
  .get()
```

### Indexes Required
- **groupId + date**: Composite index
- **groupId + expenseType**: Composite index
- **createdByUid + date**: Composite index

---

## 4. Activities Collection

**Collection Path**: `/activities`

Stores activity feed entries for all user actions.

### Document Structure

**Document ID**: Auto-generated Firestore ID

```typescript
{
  id: string                      // Activity ID (matches document ID)
  timestamp: number               // When activity occurred (milliseconds)
  activityType: string            // Type of activity (see ActivityType enum)
  actorUid: string                // UID of user who performed action
  actorName: string               // Display name of actor

  involvedUids: string[]          // All UIDs relevant to this activity (for querying)

  groupId: string | null          // Related group ID (if applicable)
  groupName: string | null        // Display name of group
  entityId: string | null         // ID of related entity (expense ID, member UID, etc.)
  displayText: string | null      // Main subject text (e.g., expense description)
  totalAmount: number | null      // Amount involved (for expenses/payments)

  financialImpacts: Map<string, number> | null  // Per-user financial impact
}
```

### ActivityType Enum Values

```
GROUP_CREATED
GROUP_DELETED
MEMBER_ADDED
MEMBER_REMOVED
MEMBER_LEFT
EXPENSE_ADDED
EXPENSE_UPDATED
EXPENSE_DELETED
PAYMENT_MADE
PAYMENT_UPDATED
PAYMENT_DELETED
```

### Example Document

```json
{
  "id": "activity_abc123",
  "timestamp": 1678905678901,
  "activityType": "EXPENSE_ADDED",
  "actorUid": "abc123xyz",
  "actorName": "John Doe",
  "involvedUids": ["abc123xyz", "def456uvw", "ghi789rst"],
  "groupId": "group_xyz789",
  "groupName": "Weekend Trip",
  "entityId": "exp_abc123",
  "displayText": "Dinner at restaurant",
  "totalAmount": 150.00,
  "financialImpacts": {
    "abc123xyz": 100.00,
    "def456uvw": -50.00,
    "ghi789rst": -50.00
  }
}
```

### Key Fields Explained

- **involvedUids**: Critical for querying - includes ALL users affected by this activity
  - For group expense: all group members
  - For friend expense: both friends
  - For member added: existing members + new member
- **financialImpacts**: Shows net change for each user
  - Positive: user is owed money (gets back)
  - Negative: user owes money
  - Zero: neutral transaction for that user

### Queries

```kotlin
// Get all activities for current user
activitiesCollection
  .whereArrayContains("involvedUids", currentUserId)
  .orderBy("timestamp", Query.Direction.DESCENDING)
  .limit(50)
  .get()

// Get activities for a specific group
activitiesCollection
  .whereEqualTo("groupId", groupId)
  .orderBy("timestamp", Query.Direction.DESCENDING)
  .get()

// Get activities of specific type
activitiesCollection
  .whereArrayContains("involvedUids", currentUserId)
  .whereEqualTo("activityType", "EXPENSE_ADDED")
  .orderBy("timestamp", Query.Direction.DESCENDING)
  .get()
```

### Indexes Required
- **involvedUids + timestamp**: Composite index (array-contains + orderBy)
- **groupId + timestamp**: Composite index
- **activityType + timestamp**: Composite index
- **involvedUids + activityType + timestamp**: Composite index

---

## Data Relationships

### User ↔ User (Friends)
- **Type**: Many-to-Many
- **Implementation**: Array of friend UIDs in User document
- **Bilateral**: Both users must have each other in their `friends` array

### User ↔ Group (Membership)
- **Type**: Many-to-Many
- **Implementation**: Array of member UIDs in Group document
- **Query**: Groups where `members` array contains user UID

### Group ↔ Expense
- **Type**: One-to-Many
- **Implementation**: `groupId` field in Expense document
- **Special**: `groupId` can be `null` for friend-to-friend expenses

### User ↔ Expense (Participation)
- **Type**: Many-to-Many
- **Implementation**:
  - `paidBy` array (who paid)
  - `participants` array (who owes)
- **Note**: User can be both payer and participant

### Activity → Multiple Entities
- **Type**: References multiple types
- **Implementation**: `entityId` field + `activityType` to determine entity type
- **Examples**:
  - `EXPENSE_ADDED`: entityId = expense ID
  - `MEMBER_ADDED`: entityId = new member's UID
  - `GROUP_CREATED`: entityId = group ID

---

## Security Rules Overview

### General Principles
1. Users can only read their own user document
2. Users can only access groups they are members of
3. Users can only access expenses in their groups or involving them
4. Activities are filtered by `involvedUids`

### Example Rules (Simplified)

```javascript
service cloud.firestore {
  match /databases/{database}/documents {

    // Users collection
    match /users/{userId} {
      allow read: if request.auth.uid == userId;
      allow write: if request.auth.uid == userId;
    }

    // Groups collection
    match /groups/{groupId} {
      allow read: if request.auth.uid in resource.data.members;
      allow create: if request.auth.uid in request.resource.data.members;
      allow update: if request.auth.uid in resource.data.members;
      allow delete: if request.auth.uid == resource.data.createdByUid;
    }

    // Expenses collection
    match /expenses/{expenseId} {
      allow read: if request.auth.uid == resource.data.createdByUid
                  || request.auth.uid in getParticipantUids(resource.data);
      allow create: if request.auth.uid == request.resource.data.createdByUid;
      allow update, delete: if request.auth.uid == resource.data.createdByUid;
    }

    // Activities collection
    match /activities/{activityId} {
      allow read: if request.auth.uid in resource.data.involvedUids;
      allow create: if request.auth.uid == request.resource.data.actorUid;
    }
  }
}
```

---

## Data Lifecycle

### User Creation Flow
1. Firebase Auth creates user account
2. User document created in `/users` with UID as document ID
3. Profile fields populated (name, username, email, phone)
4. Profile picture uploaded → URL stored in `profilePictureUrl`
5. QR code generated → uploaded → URL stored in `qrCodeUrl`

### Expense Creation Flow
1. User creates expense via UI
2. Expense document created in `/expenses`
3. Split calculations performed (via `CalculateSplitUseCase`)
4. `paidBy` and `participants` arrays populated
5. Activity document created in `/activities`
6. Real-time listeners update UI for all involved users

### Group Deletion (Archive)
1. User selects "Archive Group"
2. Group document updated: `isArchived = true`
3. Activity logged: `GROUP_DELETED`
4. Group hidden from active lists (still accessible via query)
5. Expenses preserved (historical data)

---

## Performance Considerations

### Indexing Strategy
- Compound indexes for common queries (see each collection)
- Array-contains queries require single-field indexes on array fields
- OrderBy requires indexes on sort fields

### Denormalization
- **actorName** in Activity: Avoids extra user lookup
- **groupName** in Activity: Avoids extra group lookup
- **financialImpacts** in Activity: Pre-computed balances for display

### Query Optimization
- Use `.limit()` for pagination
- Leverage Firestore offline persistence
- Use real-time listeners only when needed
- Batch reads when fetching multiple documents

---

## Migration & Versioning

### Current Schema Version: 1.0

### Future Considerations
- Add schema version field to documents for migration tracking
- Use Cloud Functions for complex migrations
- Maintain backward compatibility during transitions

---

## Backup & Recovery

### Firestore Backups
- Enable automated daily backups in Firebase Console
- Export data periodically for long-term archival
- Test restore procedures regularly

---

## References

- [Firestore Data Model](https://firebase.google.com/docs/firestore/data-model)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Indexing Best Practices](https://firebase.google.com/docs/firestore/query-data/indexing-best-practices)
