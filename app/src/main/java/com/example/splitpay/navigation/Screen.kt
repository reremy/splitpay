package com.example.splitpay.navigation

object Screen {
    const val Welcome = "welcome"
    const val SignUp = "signup"
    const val Login = "login"
    const val Home = "Home"
    const val CreateGroup = "create_group"
    const val GroupDetail = "group_detail/{groupId}"
    const val NonGroupDetail = "non_group_detail" // Special route for non-group expenses
    const val GroupSettings = "group_settings/{groupId}"
    const val AddGroupMembers = "add_group_members/{groupId}"
    const val AddExpenseWithGroup = "add_expense/{groupId}" // Route for adding with prefilled group
    const val AddExpenseNoGroup = "add_expense_no_group" // Route for adding without prefilled group
    const val AddFriend = "add_friend"
    const val FriendProfilePreview = "friend_profile_preview" // Base path
    // Route with arguments for preview screen
    const val FriendProfilePreviewRoute = "$FriendProfilePreview/{userId}?username={username}"
    const val FriendDetail = "friend_detail" // Base path
    const val FriendDetailRoute = "$FriendDetail/{friendId}"
}