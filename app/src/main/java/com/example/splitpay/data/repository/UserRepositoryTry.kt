package com.example.splitpay.data.repository

import com.example.splitpay.data.model.User
import com.google.firebase.auth.FirebaseAuth
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
}