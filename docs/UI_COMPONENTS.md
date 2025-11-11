# UI Components Documentation

Guide to reusable UI components in SplitPay built with Jetpack Compose.

---

## Overview

**Location**: `app/src/main/java/com/example/splitpay/ui/common/`

SplitPay uses **Jetpack Compose** for declarative UI and **Material Design 3** for theming and components.

---

## Theme System

### Location
`app/src/main/java/com/example/splitpay/ui/theme/`

### Files
- **Theme.kt**: Main theme setup
- **Color.kt**: Color definitions
- **Type.kt**: Typography definitions

### Material 3 Color Scheme

The app uses Material 3's dynamic color system:

```kotlin
@Composable
fun SplitPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Key Colors

**Primary**: Main brand color (used for buttons, app bar)
**Secondary**: Accent color
**Tertiary**: Additional accent
**Error**: Error states and destructive actions
**Surface**: Background surfaces
**OnPrimary/OnSurface**: Text colors for contrast

### Usage

```kotlin
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    )
) {
    Text("Click Me")
}
```

---

## Common Components

### 1. Buttons

#### Primary Button

Standard call-to-action button.

```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
```

**Usage**:
```kotlin
PrimaryButton(
    text = "Add Expense",
    onClick = { viewModel.addExpense() }
)
```

#### Outlined Button

Secondary action button with border.

```kotlin
@Composable
fun OutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text)
    }
}
```

#### Icon Button

Button with icon only.

```kotlin
IconButton(onClick = { }) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings"
    )
}
```

---

### 2. Text Fields

#### Standard Text Field

```kotlin
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp)
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
```

**Usage**:
```kotlin
CustomTextField(
    value = email,
    onValueChange = { email = it },
    label = "Email",
    placeholder = "Enter your email",
    keyboardType = KeyboardType.Email,
    isError = emailError.isNotEmpty(),
    errorMessage = emailError
)
```

---

### 3. Dialogs

#### Confirmation Dialog

```kotlin
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
```

**Usage**:
```kotlin
if (showDeleteDialog) {
    ConfirmationDialog(
        title = "Delete Expense",
        message = "Are you sure you want to delete this expense?",
        onConfirm = {
            viewModel.deleteExpense()
            showDeleteDialog = false
        },
        onDismiss = { showDeleteDialog = false },
        confirmText = "Delete"
    )
}
```

#### Loading Dialog

```kotlin
@Composable
fun LoadingDialog(
    message: String = "Loading..."
) {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text(message)
            }
        }
    }
}
```

---

### 4. Cards

#### Expense Card

```kotlin
@Composable
fun ExpenseCard(
    expense: Expense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatDate(expense.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatCurrency(expense.totalAmount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

---

### 5. List Items

#### Friend List Item

```kotlin
@Composable
fun FriendListItem(
    friend: FriendWithBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Profile picture
            AsyncImage(
                model = friend.user.profilePictureUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                placeholder = painterResource(R.drawable.default_avatar),
                error = painterResource(R.drawable.default_avatar)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Name
            Text(
                text = friend.user.fullName,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Balance
        Text(
            text = formatCurrency(friend.balance),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                friend.balance > 0 -> Color.Green
                friend.balance < 0 -> Color.Red
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
```

---

### 6. Top App Bar

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    title: String,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
```

**Usage**:
```kotlin
CustomTopAppBar(
    title = "Expense Detail",
    onNavigationClick = { navController.navigateUp() },
    actions = {
        IconButton(onClick = { showEditDialog = true }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
)
```

---

### 7. Bottom Navigation

```kotlin
@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.People, contentDescription = "Friends") },
            label = { Text("Friends") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Group, contentDescription = "Groups") },
            label = { Text("Groups") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Activity") },
            label = { Text("Activity") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
    }
}
```

---

### 8. Empty State

```kotlin
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onActionClick) {
                Text(actionText)
            }
        }
    }
}
```

**Usage**:
```kotlin
if (expenses.isEmpty()) {
    EmptyState(
        icon = Icons.Default.Receipt,
        title = "No Expenses Yet",
        message = "Add your first expense to get started tracking shared costs.",
        actionText = "Add Expense",
        onActionClick = { navController.navigate(Screen.AddExpense) }
    )
}
```

---

### 9. Loading Indicator

```kotlin
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
```

---

### 10. Error State

```kotlin
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
```

---

### 11. Snackbar

```kotlin
@Composable
fun showSnackbar(
    scaffoldState: SnackbarHostState,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    LaunchedEffect(message) {
        val result = scaffoldState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && onAction != null) {
            onAction()
        }
    }
}
```

---

### 12. Date Picker

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let(onDateSelected)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
```

---

### 13. Image Picker

```kotlin
@Composable
fun ImagePickerButton(
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onImageSelected)
    }

    Button(
        onClick = { launcher.launch("image/*") },
        modifier = modifier
    ) {
        Icon(Icons.Default.Image, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Choose Image")
    }
}
```

---

### 14. Pull to Refresh

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
    ) {
        content()

        if (pullRefreshState.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
```

---

## Utility Functions

### Currency Formatting

```kotlin
fun formatCurrency(amount: Double): String {
    return String.format("$%.2f", abs(amount))
}

fun formatCurrencyWithSign(amount: Double): String {
    return when {
        amount > 0 -> "+$%.2f".format(amount)
        amount < 0 -> "-$%.2f".format(abs(amount))
        else -> "$0.00"
    }
}
```

### Date Formatting

```kotlin
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
```

---

## Best Practices

### 1. Composition over Inheritance
- Build complex UI from small, reusable composables
- Avoid deeply nested hierarchies

### 2. State Hoisting
- Hoist state to the lowest common ancestor
- Make composables stateless when possible

```kotlin
// Good: Stateless composable
@Composable
fun TextField(value: String, onValueChange: (String) -> Unit) { }

// Bad: Stateful composable
@Composable
fun TextField() {
    var value by remember { mutableStateOf("") }
}
```

### 3. Modifiers
- Always accept a `modifier` parameter
- Apply it to the root composable

```kotlin
@Composable
fun CustomCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier) { }
}
```

### 4. Preview
- Add `@Preview` annotations for UI development

```kotlin
@Preview(showBackground = true)
@Composable
fun PreviewExpenseCard() {
    SplitPayTheme {
        ExpenseCard(
            expense = Expense(description = "Lunch", totalAmount = 25.0),
            onClick = {}
        )
    }
}
```

### 5. Accessibility
- Provide meaningful `contentDescription` for images and icons
- Use semantic properties

```kotlin
Icon(
    imageVector = Icons.Default.Delete,
    contentDescription = "Delete expense"  // Important for screen readers
)
```

---

## Performance Tips

1. **Remember expensive calculations**:
```kotlin
val sortedExpenses = remember(expenses) {
    expenses.sortedByDescending { it.date }
}
```

2. **Use `LazyColumn` for lists**:
```kotlin
LazyColumn {
    items(expenses) { expense ->
        ExpenseCard(expense = expense, onClick = {})
    }
}
```

3. **Avoid unnecessary recomposition**:
```kotlin
@Composable
fun ExpensiveComposable(data: List<Expense>) {
    // Only recomposes when data changes
}
```

---

## References

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Compose Layouts](https://developer.android.com/jetpack/compose/layouts)
- [Compose State](https://developer.android.com/jetpack/compose/state)
