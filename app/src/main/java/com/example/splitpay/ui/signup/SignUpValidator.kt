

// SignUpValidator.kt
package com.example.splitpay.ui.signup

import android.util.Patterns

object SignUpValidator {

    fun validateFullName(fullName: String): String? =
        if (fullName.isBlank()) "Full name cannot be empty" else null

    fun validateUsername(username: String): String? =
        if (username.isBlank()) "Username cannot be empty" else null

    fun validateEmail(email: String): String? =
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid Email"
        else null

    fun validatePassword(password: String): String? =
        if (password.length < 6) "Password must be at least 6 characters" else null

    fun validateRetypePassword(password: String, retypePassword: String): String? =
        if (password != retypePassword) "Passwords do not match" else null

    fun validateAll(
        fullName: String,
        username: String,
        email: String,
        password: String,
        retypePassword: String
    ): Map<String, String?> {
        return mapOf(
            "fullNameError" to validateFullName(fullName),
            "usernameError" to validateUsername(username),
            "emailError" to validateEmail(email),
            "passwordError" to validatePassword(password),
            "retypePasswordError" to validateRetypePassword(password, retypePassword)
        )
    }
}
