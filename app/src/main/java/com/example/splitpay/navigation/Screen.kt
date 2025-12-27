package com.example.splitpay.navigation

object Screen {
    const val Welcome = "welcome"
    const val SignUp = "signup"
    const val Login = "login"
    const val Home = "Home"
    const val CreateGroup = "create_group"
    const val EditGroup = "edit_group/{groupId}"
    const val GroupDetail = "group_detail/{groupId}"
    const val NonGroupDetail = "non_group_detail"
    const val GroupSettings = "group_settings/{groupId}"
    const val AddGroupMembers = "add_group_members/{groupId}"
    const val AddExpense = "add_expense" // Base route
    // Define the full route pattern with optional query parameters
    const val AddExpenseRoute = "$AddExpense?groupId={groupId}&expenseId={expenseId}"
    const val AddFriend = "add_friend"
    const val FriendProfilePreview = "friend_profile_preview"
    const val FriendProfilePreviewRoute = "$FriendProfilePreview/{userId}?username={username}"
    const val FriendDetail = "friend_detail"
    const val FriendDetailRoute = "$FriendDetail/{friendId}"
    const val FriendSettings = "friend_settings"
    const val FriendSettingsRoute = "$FriendSettings/{friendId}"

    const val SettleUpFriend = "settle_up_friend"
    const val SettleUpFriendRoute = "$SettleUpFriend/{friendId}"
    const val SelectBalanceToSettle = "select_balance_to_settle"
    const val SelectBalanceToSettleRoute = "$SelectBalanceToSettle/{friendId}"
    const val SelectPayerFriend = "select_payer_friend"
    const val SelectPayerFriendRoute = "$SelectPayerFriend/{friendId}"

    // --- NEW: Settle Up Flow Routes ---
    const val SettleUp = "settle_up"
    const val SettleUpRoute = "$SettleUp/{groupId}"
    const val RecordPayment = "record_payment"
    const val RecordPaymentRoute = "$RecordPayment/{groupId}?memberUid={memberUid}&balance={balance}&paymentId={paymentId}&payerUid={payerUid}&recipientUid={recipientUid}" // Pass balance as a float string, paymentId for edit mode, or custom payer/recipient for manual selection
    const val MoreOptions = "more_options"
    const val MoreOptionsRoute = "$MoreOptions/{groupId}"

    // --- Activity Detail ---
    const val ActivityDetail = "activity_detail"
    const val ActivityDetailRoute = "$ActivityDetail?activityId={activityId}&expenseId={expenseId}"

    // --- Expense Detail ---
    const val ExpenseDetail = "expense_detail"
    const val ExpenseDetailRoute = "$ExpenseDetail/{expenseId}"

    // --- Payment Detail ---
    const val PaymentDetail = "payment_detail"
    const val PaymentDetailRoute = "$PaymentDetail/{paymentId}"

    // --- Edit Profile ---
    const val EditProfile = "edit_profile"

    // --- Blocked Users ---
    const val BlockedUsers = "blocked_users"

    // --- Receipt Scanner ---
    const val ReceiptScanner = "receipt_scanner"
    const val ReceiptScannerRoute = "$ReceiptScanner/{groupId}"

    const val ReceiptReview = "receipt_review"
    const val ReceiptReviewRoute = "$ReceiptReview/{groupId}"

}
