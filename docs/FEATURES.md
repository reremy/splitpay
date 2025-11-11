# Features Documentation

Complete guide to all features in SplitPay.

---

## Table of Contents

1. [User Authentication](#user-authentication)
2. [Profile Management](#profile-management)
3. [Friend Management](#friend-management)
4. [Group Management](#group-management)
5. [Expense Management](#expense-management)
6. [Payment & Settlement](#payment--settlement)
7. [Activity Feed](#activity-feed)
8. [Charts & Analytics](#charts--analytics)
9. [Reminders](#reminders)
10. [Export](#export)

---

## 1. User Authentication

### Sign Up

**User Flow**:
1. User opens app and sees Welcome screen
2. Taps "Get Started"
3. Taps "Sign Up"
4. Fills in registration form:
   - Full Name
   - Username (unique)
   - Email
   - Phone Number
   - Password
5. Taps "Sign Up"
6. Account created in Firebase Auth
7. User profile created in Firestore
8. User redirected to Home screen

**Features**:
- Email/password authentication via Firebase
- Username uniqueness validation
- Input validation (email format, password strength)
- Error handling (duplicate email, weak password, etc.)

**Implementation**:
- **Files**: `ui/signup/SignUpScreen.kt`, `ui/signup/SignUpViewModel.kt`
- **Repository**: `data/repository/UserRepository.kt`

---

### Login

**User Flow**:
1. User taps "Login" on Welcome screen
2. Enters email and password
3. Taps "Login"
4. Authenticated via Firebase Auth
5. Redirected to Home screen

**Features**:
- Persistent login (session management)
- "Remember me" functionality
- Password reset (forgot password)
- Error handling (invalid credentials, network errors)

**Implementation**:
- **Files**: `ui/login/LoginScreen.kt`, `ui/login/LoginViewModel.kt`
- **Repository**: `data/repository/UserRepository.kt`

---

## 2. Profile Management

### View Profile

**User Flow**:
1. User navigates to Home → Profile tab
2. Views own profile information:
   - Profile picture
   - Full name
   - Username
   - Email
   - Phone number
   - QR code

**Implementation**:
- **Files**: `ui/profile/ProfileScreen.kt`

---

### Edit Profile

**User Flow**:
1. User taps "Edit Profile" button
2. Modifies editable fields:
   - Profile picture (upload/crop)
   - Full name
   - Username
   - Phone number
3. Taps "Save"
4. Profile updated in Firestore

**Features**:
- Profile picture upload with cropping
- Image compression for storage efficiency
- Real-time validation
- QR code regeneration if username changes

**Implementation**:
- **Files**: `ui/profile/EditProfileScreen.kt`
- **Repository**: `data/repository/UserRepository.kt`, `data/repository/FileStorageRepository.kt`

---

### QR Code

**Purpose**: Easy friend discovery

**User Flow**:
1. User views their QR code in profile
2. Friend scans QR code with their device
3. Friend redirected to user's profile preview
4. Friend can send friend request

**Features**:
- QR code generation with username embedded
- Stored in Firebase Storage
- Scannable by any QR code scanner
- Contains deep link to app (future enhancement)

**Implementation**:
- **Files**: `ui/profile/ProfileScreen.kt`
- **Repository**: `data/repository/FileStorageRepository.kt`

---

## 3. Friend Management

### Add Friend

**User Flow**:
1. User taps "Add Friend" button
2. Chooses one of:
   - **Search by username**: Enters friend's username
   - **Scan QR code**: Scans friend's QR code
3. Views friend's profile preview
4. Taps "Add Friend"
5. Friend added to user's friends list

**Features**:
- Search by username (case-insensitive)
- QR code scanning
- Profile preview before adding
- Bilateral friendship (both users must add each other)
- Cannot add self
- Cannot add blocked users

**Implementation**:
- **Files**: `ui/friends/AddFriendScreen.kt`, `ui/friends/FriendProfilePreviewScreen.kt`
- **Repository**: `data/repository/UserRepository.kt`

---

### View Friends List

**User Flow**:
1. User navigates to Home → Friends tab
2. Views list of friends with:
   - Profile picture
   - Name
   - Current balance (owe/owed)
   - Total balance summary at top

**Features**:
- Real-time balance calculations
- Color-coded balances (red = owe, green = owed)
- Search/filter friends
- Sort by balance or name

**Implementation**:
- **Files**: `ui/friends/FriendsScreen.kt`, `ui/friends/FriendsViewModel.kt`
- **Data Model**: `data/model/FriendWithBalance.kt`

---

### Friend Detail

**User Flow**:
1. User taps on a friend from Friends list
2. Views detailed friend page:
   - Friend's profile info
   - Overall balance
   - List of shared expenses
   - Balance breakdown by expense
3. Can perform actions:
   - Add expense with friend
   - Settle up
   - View expense details
   - Access friend settings

**Features**:
- Chronological expense list
- Balance calculation
- Charts (spending over time)
- Export expense history
- Set payment reminders

**Implementation**:
- **Files**: `ui/friendsDetail/FriendDetailScreen.kt`, `ui/friendsDetail/FriendDetailViewModel.kt`

---

### Friend Settings

**User Flow**:
1. From Friend Detail, tap "Settings" icon
2. Options:
   - Remove friend
   - Block user
   - View friend's profile

**Features**:
- **Remove Friend**: Removes bilateral friendship
- **Block User**: Prevents all interactions, removes from friends list

**Implementation**:
- **Files**: `ui/friendsDetail/FriendSettingsScreen.kt`
- **Repository**: `data/repository/UserRepository.kt`

---

### Blocked Users

**User Flow**:
1. Navigate to Profile → Blocked Users
2. View list of blocked users
3. Can unblock users

**Features**:
- View all blocked users
- Unblock with confirmation
- Blocked users cannot:
  - Send friend requests
  - Be added to groups with you
  - See your profile

**Implementation**:
- **Files**: `ui/profile/BlockedUsersScreen.kt`

---

## 4. Group Management

### Create Group

**User Flow**:
1. User taps "Create Group" from Groups tab
2. Fills in group details:
   - Group name
   - Group icon (predefined icons)
   - Group photo (optional)
3. Adds members from friends list
4. Taps "Create"
5. Group created in Firestore
6. Activity logged (GROUP_CREATED)

**Features**:
- Select from predefined icons (trip, home, friends, etc.)
- Upload custom group photo
- Add multiple members at once
- Creator automatically added as member

**Implementation**:
- **Files**: `ui/groups/CreateGroupScreen.kt`, `ui/groups/CreateGroupViewModel.kt`
- **Repository**: `data/repository/GroupsRepository.kt`

---

### View Groups List

**User Flow**:
1. User navigates to Home → Groups tab
2. Views list of all groups:
   - Group name and icon
   - Member count
   - Overall group balance
3. Tap on group to view details

**Features**:
- Active groups shown by default
- View archived groups (separate list)
- Overall balance summary across all groups
- Search/filter groups

**Implementation**:
- **Files**: `ui/groups/GroupsScreen.kt`, `ui/groups/GroupsViewModel.kt`
- **Data Model**: `data/model/GroupWithBalance.kt`

---

### Group Detail

**User Flow**:
1. User taps on a group from Groups list
2. Views group detail page:
   - Group info (name, icon, members)
   - Overall balances
   - List of group expenses
   - Balance breakdown per member
3. Can perform actions:
   - Add expense to group
   - Settle up within group
   - View expense details
   - Access group settings

**Features**:
- Member avatars with balances
- Expense timeline
- Charts (group spending analysis)
- Export group expenses
- Reminders for unsettled balances

**Implementation**:
- **Files**: `ui/groups/GroupDetailScreen.kt`, `ui/groups/GroupDetailViewModel.kt`

---

### Edit Group

**User Flow**:
1. From Group Detail, tap "Edit" icon
2. Modify group details:
   - Group name
   - Group icon
   - Group photo
3. Taps "Save"
4. Group updated in Firestore

**Features**:
- Update group metadata
- Only creator or members can edit (configurable)
- Activity logged (GROUP_UPDATED)

**Implementation**:
- **Files**: `ui/groups/EditGroupScreen.kt`
- **Repository**: `data/repository/GroupsRepository.kt`

---

### Group Settings

**User Flow**:
1. From Group Detail, tap "Settings" icon
2. Options:
   - **Add Members**: Add new friends to group
   - **Remove Members**: Remove members (admin only)
   - **Leave Group**: Current user leaves group
   - **Archive Group**: Soft delete group
   - **Delete Group**: Permanently delete (future)

**Features**:
- **Add Members**: Select from friends list (not already in group)
- **Remove Members**: Creator can remove others
- **Leave Group**: Any member can leave (if not creator)
- **Archive Group**: Hides from active list, preserves data

**Implementation**:
- **Files**: `ui/groups/GroupSettingsScreen.kt`, `ui/groups/AddGroupMembersScreen.kt`
- **Repository**: `data/repository/GroupsRepository.kt`

---

### Non-Group Expenses

**Purpose**: Track expenses with friends outside of groups

**User Flow**:
1. User navigates to "Non-Group" detail page
2. Views all friend-to-friend expenses (where `groupId = null`)
3. Can add expenses directly with friends

**Features**:
- Separate view for non-group expenses
- All friend-to-friend balances in one place
- Add expense with specific friend

**Implementation**:
- **Files**: `ui/groups/NonGroupDetailScreen.kt`
- Expense documents with `groupId = null`

---

## 5. Expense Management

### Add Expense

**User Flow**:
1. User taps "Add Expense" (from Group Detail or Friend Detail)
2. Fills in expense form:
   - **Description**: What was purchased
   - **Amount**: Total expense amount
   - **Category**: Expense category (food, transport, etc.)
   - **Date**: When expense occurred
   - **Paid By**: Who paid (single or multiple payers)
   - **Split Type**: How to split
   - **Participants**: Who is involved
   - **Memo**: Optional notes
   - **Image**: Attach receipt photo
3. Taps "Save"
4. Expense calculated and saved to Firestore
5. Activity logged (EXPENSE_ADDED)
6. All participants notified

**Split Types**:

#### Equal Split
- Total amount divided equally among all participants
- Example: $100 split among 3 people = $33.33 each

#### Exact Amounts
- Specify exact amount each person owes
- Example: Person A owes $50, Person B owes $30, Person C owes $20

#### Percentages
- Specify percentage each person owes (must total 100%)
- Example: Person A 50%, Person B 30%, Person C 20%

#### Shares
- Specify number of shares each person gets
- Example: Person A 2 shares, Person B 1 share, Person C 1 share
- $100 total = $50 for A, $25 for B, $25 for C

**Multiple Payers**:
- Support for expenses paid by multiple people
- Example: Person A paid $60, Person B paid $40

**Features**:
- Smart split calculations via `CalculateSplitUseCase`
- Real-time balance preview
- Receipt image upload and cropping
- Category selection with icons
- Date picker for backdated expenses
- Input validation (amounts, participants)

**Implementation**:
- **Files**: `ui/expense/AddExpenseScreen.kt`, `ui/expense/AddExpenseViewModel.kt`
- **Use Case**: `domain/usecase/CalculateSplitUseCase.kt`
- **Repository**: `data/repository/ExpenseRepository.kt`

---

### View Expense Detail

**User Flow**:
1. User taps on an expense from Group/Friend Detail
2. Views full expense details:
   - Description, amount, date
   - Who paid and how much
   - Split breakdown per person
   - Category
   - Receipt image
   - Memo
3. Can perform actions:
   - Edit expense (if creator)
   - Delete expense (if creator)
   - View full-size receipt

**Features**:
- Complete expense breakdown
- Visual split representation
- Timestamp and creator info
- Edit/delete permissions

**Implementation**:
- **Files**: `ui/expense/ExpenseDetailScreen.kt`, `ui/expense/ExpenseDetailViewModel.kt`

---

### Edit Expense

**User Flow**:
1. From Expense Detail, tap "Edit" icon
2. Modify expense fields
3. Taps "Save"
4. Expense updated in Firestore
5. Activity logged (EXPENSE_UPDATED)

**Features**:
- Only expense creator can edit
- Recalculates splits if amount/participants change
- Updates balances automatically
- Notifies all participants

**Implementation**:
- **Files**: `ui/expense/AddExpenseScreen.kt` (reused for editing)
- **Repository**: `data/repository/ExpenseRepository.kt`

---

### Delete Expense

**User Flow**:
1. From Expense Detail, tap "Delete" icon
2. Confirm deletion
3. Expense deleted from Firestore
4. Balances recalculated
5. Activity logged (EXPENSE_DELETED)

**Features**:
- Only expense creator can delete
- Soft delete (mark as deleted) vs hard delete
- Reverses balance changes
- Notifies participants

**Implementation**:
- **Files**: `ui/expense/ExpenseDetailScreen.kt`
- **Repository**: `data/repository/ExpenseRepository.kt`

---

### Expense Categories

**Available Categories**:
- General (Misc)
- Food & Drink
- Groceries
- Shopping
- Entertainment
- Transportation
- Utilities
- Rent
- Travel
- Healthcare
- Education
- Other

**Features**:
- Icon representation for each category
- Color coding
- Filter expenses by category
- Analytics by category

**Implementation**:
- Stored as string in Expense model
- UI icons mapped in `ui/common/CategoryIcon.kt`

---

## 6. Payment & Settlement

### Settle Up (Record Payment)

**User Flow**:
1. From Friend/Group Detail, tap "Settle Up"
2. Choose settlement option:
   - **Settle all balances**: Pay off entire balance
   - **Partial payment**: Enter specific amount
3. Select payer and recipient
4. Enter amount (if partial)
5. Add optional memo
6. Tap "Record Payment"
7. Payment saved as special expense (type = PAYMENT)
8. Balances updated
9. Activity logged (PAYMENT_MADE)

**Features**:
- Full or partial settlement
- Multiple settlement suggestions (optimize debts)
- Payment history tracking
- Confirmation before recording
- Notification to recipient

**Settlement Optimization**:
- Simplify debts (minimize number of transactions)
- Example: If A owes B $10, and B owes A $5, simplify to: A owes B $5

**Implementation**:
- **Files**: `ui/settleUp/SettleUpScreen.kt`, `ui/settleUp/RecordPaymentScreen.kt`
- **Repository**: `data/repository/ExpenseRepository.kt`
- Payment is an Expense with `expenseType = "PAYMENT"`

---

### View Payment Detail

**User Flow**:
1. User taps on a payment from Activity feed or Group/Friend Detail
2. Views payment details:
   - Amount paid
   - Payer and recipient
   - Date
   - Memo
3. Can edit or delete (if creator)

**Implementation**:
- **Files**: `ui/expense/PaymentDetailScreen.kt`

---

### Balance Calculations

**How Balances Work**:

For each user pair (A and B):
1. Sum all expenses where:
   - A paid and B participated
   - B paid and A participated
   - Both paid and both participated (split expense)
2. Sum all payments between A and B
3. Net balance = (A's share of B's payments) - (B's share of A's payments) - (payments from A to B) + (payments from B to A)

**Positive balance**: User is owed money
**Negative balance**: User owes money

**Implementation**:
- Calculated in ViewModels
- Uses Expense repository to fetch relevant expenses
- Real-time updates via Firestore listeners

---

## 7. Activity Feed

### View Activity Feed

**User Flow**:
1. User navigates to Home → Activity tab
2. Views chronological feed of all activities:
   - Expense additions
   - Payments made
   - Group changes
   - Member additions/removals

**Features**:
- Real-time updates
- Filtered to current user's relevant activities
- Color-coded by activity type
- Swipe to refresh
- Pagination (load more)

**Activity Types Displayed**:
- Expense added/updated/deleted
- Payment made/updated/deleted
- Group created/deleted
- Member added/removed/left

**Implementation**:
- **Files**: `ui/activity/ActivityScreen.kt`, `ui/activity/ActivityViewModel.kt`
- **Data Model**: `data/model/Activity.kt`
- Query: `involvedUids` array contains current user ID

---

### Activity Detail

**User Flow**:
1. User taps on an activity from feed
2. Redirected to relevant detail screen:
   - Expense activity → Expense Detail
   - Payment activity → Payment Detail
   - Group activity → Group Detail

**Features**:
- Deep linking to related entities
- Context-aware navigation

**Implementation**:
- **Files**: `ui/activity/ActivityDetailScreen.kt`
- **Navigation**: Routes to appropriate screen based on `activityType`

---

## 8. Charts & Analytics

**Location**: Friend Detail and Group Detail pages

### Spending Over Time

**Chart Type**: Line chart

**Data**:
- X-axis: Time (days, weeks, months)
- Y-axis: Total spent
- Multiple lines for each participant

**User Flow**:
1. Navigate to Friend/Group Detail
2. Scroll to "Charts" section
3. View spending trends

---

### Category Breakdown

**Chart Type**: Pie chart or bar chart

**Data**:
- Categories (food, transport, etc.)
- Amount spent per category
- Percentage of total

**User Flow**:
1. View charts section
2. See visual breakdown of spending by category

---

### Balance History

**Chart Type**: Line chart

**Data**:
- X-axis: Time
- Y-axis: Balance amount
- Shows how balance changed over time

---

**Implementation**:
- **Files**: `ui/friendsDetail/charts/`, `ui/groups/charts/`
- Custom Compose charts or library (e.g., MPAndroidChart)
- Data aggregated from expenses

---

## 9. Reminders

**Purpose**: Remind friends about outstanding balances

### Set Reminder

**User Flow**:
1. From Friend/Group Detail, tap "Reminder" button
2. Choose reminder type:
   - One-time reminder
   - Recurring reminder
3. Select date/time
4. Add optional message
5. Tap "Set Reminder"

**Features**:
- Push notifications (future)
- In-app notifications
- Email reminders (future)
- Customizable messages

**Implementation**:
- Likely uses WorkManager for scheduled reminders
- Stored in Firestore (reminders subcollection or field)

---

## 10. Export

**Purpose**: Export expense data for record-keeping

### Export Data

**User Flow**:
1. From Group/Friend Detail, tap "Export" button
2. Choose export format:
   - CSV
   - PDF (future)
   - Excel (future)
3. Choose date range
4. Tap "Export"
5. File saved to device or shared

**Data Included**:
- All expenses in selected range
- Participants
- Amounts
- Categories
- Balances

**Implementation**:
- CSV generation from expense list
- File saved to Downloads folder
- Share via Android ShareSheet

---

## Future Feature Ideas

1. **Recurring Expenses**: Automatically create expenses on schedule (rent, utilities)
2. **Bill Splitting Calculator**: One-time calculator without saving
3. **Currency Conversion**: Support for multiple currencies
4. **Receipt OCR**: Automatically extract amount from receipt photos
5. **Integration with Payment Apps**: Link to Venmo, PayPal, etc.
6. **Expense Templates**: Save common expenses for quick reuse
7. **Budget Limits**: Set spending limits for categories or groups
8. **Notifications**: Push notifications for new expenses, payments, reminders
9. **Web App**: Access SplitPay from web browser
10. **Multi-language Support**: Localization for different languages

---

## Feature Implementation Priority

**Priority 1 (Core Features)**:
- ✅ User Authentication
- ✅ Friend Management
- ✅ Group Management
- ✅ Expense Management
- ✅ Settlement/Payments
- ✅ Activity Feed

**Priority 2 (Enhanced UX)**:
- ✅ Charts & Analytics
- ✅ Reminders
- ✅ Export
- Profile customization
- Search and filters

**Priority 3 (Future Enhancements)**:
- Recurring expenses
- Receipt OCR
- Currency conversion
- Web app
- Third-party integrations

---

For implementation details, see:
- [ARCHITECTURE.md](ARCHITECTURE.md) - Technical architecture
- [DATA_MODELS.md](DATA_MODELS.md) - Data structures
- [FIREBASE_SERVICES.md](FIREBASE_SERVICES.md) - Backend services
