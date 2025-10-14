package com.example.splitpay.data.repository

import android.util.Log
import com.example.splitpay.data.model.User
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepositoryTry(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
){
    suspend fun signUp2(
        fullName: String,
        username: String,
        email: String,
        password: String,
        ): Result<Unit> {
        return try {

            logD("Attempting sign-up for user: $email")
            //create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("User creation failed."))

            logI("User created in Firebase Auth: ${user.uid}")

            // Set the Firebase User's display name using the fullName
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            user.updateProfile(profileUpdates).await() // <-- ADDED: Updates displayName
            logD("Firebase User profile updated with displayName: $fullName") // Logger: Profile updated


            //build user data for firestore
            val userData = User(
                uid = user.uid,
                fullName = fullName,
                username = username,
                email = email
            )

            //store in Firestore
            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            logI("User profile saved to Firestore.")
            Result.success(Unit)
        } catch (e: Exception){
            logE("Sign-up failed for $email: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signIn2(email: String, password: String): FirebaseUser? {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user

        } catch (e: Exception) {
            throw e
        }
    }
    //return FirebaseUser if successful, caught exception if not.

    fun signOut2() {
        try {
            logD("Attempting sign out for ${auth.currentUser?.email}")
            Log.d("signOut2", "Successfully logged out")
            auth.signOut()
            logI("Successfully signed out")
        } catch (e: Exception) {
            logE("Sign out failed: ${e.message}")
            throw e
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun getUserProfile(uid: String): User? {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            logE("Failed to fetch user profile for UID: $uid. Error: ${e.message}")
            null
        }
    }


}