package com.example.splitpay.navigation

object Screen {
    const val Welcome = "welcome"
    const val SignUp = "signup"
    const val Login = "login"
    const val Home = "Home"
    const val CreateGroup = "create_group"
    const val GroupDetail = "group_detail/{groupId}"
    const val NonGroupDetail = "non_group_detail"
    const val GroupSettings = "group_settings/{groupId}"
    const val AddGroupMembers = "add_group_members/{groupId}"
    const val AddExpense = "add_expense" // Base route
    // Define the full route pattern with an optional query parameter
    const val AddExpenseRoute = "$AddExpense?groupId={groupId}"
    const val AddFriend = "add_friend"
    const val FriendProfilePreview = "friend_profile_preview"
    const val FriendProfilePreviewRoute = "$FriendProfilePreview/{userId}?username={username}"
    const val FriendDetail = "friend_detail"
    const val FriendDetailRoute = "$FriendDetail/{friendId}"
}