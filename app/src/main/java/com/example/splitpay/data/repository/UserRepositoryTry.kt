package com.example.splitpay.data.repository

import android.util.Log
import com.example.splitpay.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
            //create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("User creation failed."))

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

            Result.success(Unit)
        } catch (e: Exception){
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
            Log.d("signOut2", "Successfully logged out")
            auth.signOut()
        } catch (e: Exception) {
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
            null
        }
    }


}