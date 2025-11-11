# Firebase Services Documentation

Complete guide to Firebase integration in SplitPay.

---

## Overview

SplitPay uses the following Firebase services:

1. **Firebase Authentication** - User authentication and account management
2. **Cloud Firestore** - Real-time NoSQL database
3. **Firebase Storage** - File and image storage
4. **Firebase Analytics** - Usage tracking and insights

---

## Firebase Configuration

### Setup File

**File**: `app/google-services.json`

This file contains Firebase project configuration including:
- Project ID
- API keys
- Storage bucket URL
- Database URL

**Security**: This file is gitignored and should never be committed to version control.

### Gradle Configuration

**File**: `app/build.gradle.kts`

```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BOM (Bill of Materials) - manages versions
    implementation(platform(libs.firebase.bom))

    // Firebase services
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Coroutines support for Firebase
    implementation(libs.kotlinx.coroutines.play.services)
}
```

**Firebase BOM Version**: Managed in `gradle/libs.versions.toml`

---

## 1. Firebase Authentication

### Purpose
- Secure user authentication
- Email/password sign-in
- User session management
- Account creation and deletion

### Implementation

**Repository**: `data/repository/UserRepository.kt`

```kotlin
class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
)
```

### Key Operations

#### Sign Up

```kotlin
suspend fun signUp(
    fullName: String,
    username: String,
    email: String,
    phoneNumber: String,
    password: String
): Result<Unit> {
    // 1. Create Firebase Auth account
    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
    val user = authResult.user

    // 2. Create user document in Firestore
    val userDoc = User(
        uid = user.uid,
        fullName = fullName,
        username = username,
        email = email,
        phoneNumber = phoneNumber
    )
    usersCollection.document(user.uid).set(userDoc).await()

    return Result.success(Unit)
}
```

#### Login

```kotlin
suspend fun login(email: String, password: String): Result<FirebaseUser> {
    val authResult = auth.signInWithEmailAndPassword(email, password).await()
    val user = authResult.user ?: return Result.failure(Exception("Login failed"))
    return Result.success(user)
}
```

#### Logout

```kotlin
fun logout() {
    auth.signOut()
}
```

#### Get Current User

```kotlin
fun getCurrentUser(): FirebaseUser? {
    return auth.currentUser
}
```

#### Check Authentication State

```kotlin
val isUserLoggedIn: Boolean
    get() = auth.currentUser != null
```

### Authentication Flow

1. User enters credentials
2. ViewModel calls repository method
3. Repository calls Firebase Auth API
4. Auth state listener updates UI
5. On success: redirect to Home
6. On failure: show error message

### Session Management

- Firebase handles session persistence automatically
- User stays logged in across app restarts
- Session expires after 1 hour of inactivity (configurable)
- Refresh tokens managed by Firebase SDK

### Security Features

- Password hashing (handled by Firebase)
- Email verification (optional, can be enabled)
- Password reset via email
- Rate limiting on login attempts
- Secure token-based authentication

---

## 2. Cloud Firestore

### Purpose
- Store user data, groups, expenses, activities
- Real-time synchronization
- Offline support
- Structured NoSQL database

### Implementation

**Repositories**:
- `UserRepository.kt`
- `GroupsRepository.kt`
- `ExpenseRepository.kt`
- `ActivityRepository.kt`

### Database Structure

See [FIRESTORE_SCHEMA.md](FIRESTORE_SCHEMA.md) for complete schema documentation.

### Key Operations

#### Create Document

```kotlin
suspend fun addExpense(expense: Expense): Result<String> {
    val docRef = expensesCollection.document()
    val expenseWithId = expense.copy(id = docRef.id)
    docRef.set(expenseWithId).await()
    return Result.success(docRef.id)
}
```

#### Read Document

```kotlin
suspend fun getExpense(expenseId: String): Result<Expense> {
    val snapshot = expensesCollection.document(expenseId).get().await()
    val expense = snapshot.toObject(Expense::class.java)
    return if (expense != null) {
        Result.success(expense)
    } else {
        Result.failure(Exception("Expense not found"))
    }
}
```

#### Update Document

```kotlin
suspend fun updateExpense(expense: Expense): Result<Unit> {
    expensesCollection.document(expense.id).set(expense).await()
    return Result.success(Unit)
}
```

#### Delete Document

```kotlin
suspend fun deleteExpense(expenseId: String): Result<Unit> {
    expensesCollection.document(expenseId).delete().await()
    return Result.success(Unit)
}
```

#### Query Documents

```kotlin
suspend fun getGroupExpenses(groupId: String): Result<List<Expense>> {
    val snapshot = expensesCollection
        .whereEqualTo("groupId", groupId)
        .orderBy("date", Query.Direction.DESCENDING)
        .get()
        .await()
    val expenses = snapshot.toObjects(Expense::class.java)
    return Result.success(expenses)
}
```

#### Real-time Listeners

```kotlin
fun observeGroupExpenses(groupId: String): Flow<List<Expense>> = callbackFlow {
    val listener = expensesCollection
        .whereEqualTo("groupId", groupId)
        .orderBy("date", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
            trySend(expenses)
        }

    awaitClose { listener.remove() }
}
```

### Offline Support

Firestore provides built-in offline persistence:

**Enabled by default** in Firebase SDK

**Features**:
- Local cache of accessed data
- Writes queued when offline
- Automatic sync when back online
- Optimistic UI updates

**Configuration** (optional):
```kotlin
val settings = FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .build()
firestore.firestoreSettings = settings
```

### Indexing

Firestore automatically creates single-field indexes. Composite indexes must be created manually.

**Required Composite Indexes**:

See [FIRESTORE_SCHEMA.md](FIRESTORE_SCHEMA.md) for full list.

**Create Indexes**:
1. Run query in app
2. Check Logcat for index creation link
3. Click link to auto-create index in Firebase Console
4. OR manually create in Console: Firestore → Indexes

### Security Rules

**Development Rules** (Test Mode):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**Production Rules** (Recommended):

See [FIRESTORE_SCHEMA.md](FIRESTORE_SCHEMA.md) for detailed security rules.

**Key Principles**:
- Users can only access their own data
- Group members can access group data
- Expense participants can access expense data
- Activity feed filtered by `involvedUids`

### Error Handling

```kotlin
suspend fun getUser(uid: String): Result<User> = withContext(Dispatchers.IO) {
    try {
        val snapshot = usersCollection.document(uid).get().await()
        val user = snapshot.toObject(User::class.java)
        Result.success(user ?: throw Exception("User not found"))
    } catch (e: FirebaseFirestoreException) {
        logE("Firestore error: ${e.message}")
        Result.failure(e)
    } catch (e: Exception) {
        logE("Unexpected error: ${e.message}")
        Result.failure(e)
    }
}
```

---

## 3. Firebase Storage

### Purpose
- Store profile pictures
- Store QR code images
- Store group photos
- Store expense receipt images

### Implementation

**Repository**: `data/repository/FileStorageRepository.kt`

```kotlin
class FileStorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
)
```

### Storage Structure

```
firebase-storage-bucket/
├── profile_pictures/
│   └── {userId}/
│       └── profile.jpg
├── qr_codes/
│   └── {userId}/
│       └── qrcode.png
├── group_photos/
│   └── {groupId}/
│       └── photo.jpg
└── expense_images/
    └── {expenseId}/
        └── receipt.jpg
```

### Key Operations

#### Upload Image

```kotlin
suspend fun uploadProfilePicture(
    userId: String,
    imageUri: Uri
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val storageRef = storage.reference
            .child("profile_pictures")
            .child(userId)
            .child("profile.jpg")

        // Upload file
        storageRef.putFile(imageUri).await()

        // Get download URL
        val downloadUrl = storageRef.downloadUrl.await().toString()

        Result.success(downloadUrl)
    } catch (e: Exception) {
        logE("Upload failed: ${e.message}")
        Result.failure(e)
    }
}
```

#### Download Image URL

```kotlin
suspend fun getProfilePictureUrl(userId: String): Result<String> {
    return try {
        val storageRef = storage.reference
            .child("profile_pictures")
            .child(userId)
            .child("profile.jpg")

        val url = storageRef.downloadUrl.await().toString()
        Result.success(url)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

#### Delete Image

```kotlin
suspend fun deleteProfilePicture(userId: String): Result<Unit> {
    return try {
        val storageRef = storage.reference
            .child("profile_pictures")
            .child(userId)
            .child("profile.jpg")

        storageRef.delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Image Upload Flow

1. User selects image from gallery or camera
2. Image cropped to desired aspect ratio (1:1 for profile pictures)
3. Image compressed to reduce file size
4. Image uploaded to Firebase Storage
5. Download URL retrieved
6. URL saved to Firestore user document

### Image Loading

**Library**: Coil (io.coil-kt:coil-compose:2.5.0)

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(profilePictureUrl)
        .crossfade(true)
        .build(),
    contentDescription = "Profile Picture",
    modifier = Modifier.size(100.dp),
    placeholder = painterResource(R.drawable.default_avatar),
    error = painterResource(R.drawable.default_avatar)
)
```

### Security Rules

**Development Rules**:
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**Production Rules**:

See [FIREBASE_STORAGE_SETUP.md](../FIREBASE_STORAGE_SETUP.md) for detailed security rules.

**Key Principles**:
- Users can only upload to their own folders
- File size limits (e.g., 5MB for profile pictures)
- File type restrictions (images only)
- Read access for all authenticated users

### Performance Optimization

- **Compression**: Compress images before upload (e.g., JPEG quality 80%)
- **Caching**: Coil handles memory and disk caching automatically
- **Thumbnails**: Generate thumbnails for faster loading (future enhancement)
- **Lazy Loading**: Load images on demand using LazyColumn

---

## 4. Firebase Analytics

### Purpose
- Track user behavior and app usage
- Monitor feature adoption
- Identify performance issues
- Inform product decisions

### Implementation

Firebase Analytics is automatically initialized when `google-services.json` is present.

**File**: `app/build.gradle.kts`

```kotlin
implementation(libs.firebase.analytics)
```

### Default Events Tracked

Firebase automatically tracks:
- App opens (`app_open`)
- First app open (`first_open`)
- Sessions (`session_start`)
- Screen views (`screen_view`)
- User engagement (`user_engagement`)

### Custom Events (Future Enhancement)

**Example**:
```kotlin
val analytics = FirebaseAnalytics.getInstance(context)

// Log expense creation
analytics.logEvent("expense_created") {
    param("expense_amount", expense.totalAmount)
    param("split_type", expense.splitType)
    param("category", expense.category)
}

// Log settle up
analytics.logEvent("payment_recorded") {
    param("payment_amount", payment.totalAmount)
}
```

### User Properties (Future Enhancement)

```kotlin
analytics.setUserProperty("user_type", "premium") // or "free"
analytics.setUserProperty("friend_count", friendCount.toString())
```

### Viewing Analytics

**Firebase Console** → Analytics → Dashboard

**Metrics Available**:
- Active users (daily, weekly, monthly)
- Session duration
- Screen views
- Events
- User demographics (if enabled)
- Device info
- App version distribution

---

## Coroutines Integration

### Firebase Tasks to Coroutines

All Firebase asynchronous operations use coroutines via the `await()` extension function.

**Dependency**: `kotlinx-coroutines-play-services`

**Example**:
```kotlin
// Without coroutines (callback-based)
auth.signInWithEmailAndPassword(email, password)
    .addOnSuccessListener { result ->
        // Handle success
    }
    .addOnFailureListener { exception ->
        // Handle error
    }

// With coroutines (suspend function)
suspend fun login(email: String, password: String): Result<FirebaseUser> {
    return try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Real-time Listeners with Flow

```kotlin
fun observeUser(uid: String): Flow<User?> = callbackFlow {
    val listener = usersCollection.document(uid)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val user = snapshot?.toObject(User::class.java)
            trySend(user)
        }

    awaitClose { listener.remove() }
}
```

---

## Error Handling Best Practices

### Repository Layer

```kotlin
suspend fun operation(): Result<T> = withContext(Dispatchers.IO) {
    try {
        // Firebase operation
        val result = firebaseCall().await()
        Result.success(result)
    } catch (e: FirebaseAuthException) {
        logE("Auth error: ${e.message}")
        Result.failure(e)
    } catch (e: FirebaseFirestoreException) {
        logE("Firestore error: ${e.message}")
        Result.failure(e)
    } catch (e: FirebaseStorageException) {
        logE("Storage error: ${e.message}")
        Result.failure(e)
    } catch (e: Exception) {
        logE("Unexpected error: ${e.message}")
        Result.failure(e)
    }
}
```

### ViewModel Layer

```kotlin
viewModelScope.launch {
    _uiState.value = UiState.Loading

    userRepository.signUp(email, password).fold(
        onSuccess = {
            _uiState.value = UiState.Success
            navigateToHome()
        },
        onFailure = { exception ->
            _uiState.value = UiState.Error(exception.message ?: "Unknown error")
        }
    )
}
```

---

## Testing with Firebase

### Firebase Emulator Suite

For local testing without affecting production data:

1. Install Firebase CLI: `npm install -g firebase-tools`
2. Initialize emulators: `firebase init emulators`
3. Start emulators: `firebase emulators:start`
4. Connect app to emulators:

```kotlin
// In Application class or before Firebase usage
if (BuildConfig.DEBUG) {
    Firebase.auth.useEmulator("10.0.2.2", 9099)
    Firebase.firestore.useEmulator("10.0.2.2", 8080)
    Firebase.storage.useEmulator("10.0.2.2", 9199)
}
```

### Unit Testing

Mock Firebase services in unit tests:

```kotlin
@Test
fun testLogin() = runTest {
    val mockAuth = mock<FirebaseAuth>()
    val repository = UserRepository(auth = mockAuth)

    // Test logic
}
```

---

## Performance Monitoring (Future Enhancement)

**Firebase Performance Monitoring**: Track app performance metrics

**Setup**:
```kotlin
implementation(libs.firebase.perf)
```

**Metrics**:
- App startup time
- Network request latency
- Screen rendering time
- Custom traces

---

## Firebase Costs

### Free Tier (Spark Plan)

- **Authentication**: Unlimited
- **Firestore**: 1GB storage, 50K reads/day, 20K writes/day
- **Storage**: 5GB storage, 1GB/day downloads
- **Analytics**: Unlimited

### Paid Tier (Blaze Plan)

- Pay-as-you-go beyond free tier
- Costs scale with usage
- Set budget alerts to avoid unexpected charges

**Monitoring**: Firebase Console → Usage and Billing

---

## References

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firebase Auth Guide](https://firebase.google.com/docs/auth)
- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Firebase Storage Guide](https://firebase.google.com/docs/storage)
- [Firebase Analytics](https://firebase.google.com/docs/analytics)
- [Kotlin Coroutines + Firebase](https://firebase.google.com/docs/android/kotlin-coroutines)
