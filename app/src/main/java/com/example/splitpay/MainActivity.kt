package com.example.splitpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import com.example.splitpay.navigation.Navigation
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.theme.SplitPayTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class MainActivity : ComponentActivity() {


    private lateinit var auth: FirebaseAuth

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        initializeFcmToken()

        val navigateTo = intent.getStringExtra("navigate_to")

        setContent {
            SplitPayTheme {
                val navController = rememberNavController()

                LaunchedEffect(navigateTo){
                    if (navigateTo == "activity"){
                        navController.navigate("${Screen.Home}?tab=activity"){
                            popUpTo(navController.graph.startDestinationId){
                                inclusive = true
                            }
                        }
                    }
                }

                Surface(Modifier.fillMaxSize()) {
                    Navigation(
                        navController = navController,
                    )
                }
            }
        }
    }

    private fun initializeFcmToken() {
        mainScope.launch{
            try{
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                logI("FCM token retrieved: ${token.take(20)}...")

                val result = userRepository.updateFcmToken(token)
                if (result.isSuccess){
                    logI("FCM token updated successfully")
                } else {
                    logE("Failed to update FCM token: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                logE("Error initializing FCM token ${e.message}")
            }
        }
    }
}




