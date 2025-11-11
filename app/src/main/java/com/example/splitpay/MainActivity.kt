package com.example.splitpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.splitpay.navigation.Navigation
import com.example.splitpay.ui.theme.SplitPayTheme
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            SplitPayTheme {
                val navController = rememberNavController()

                Surface(Modifier.fillMaxSize()) {
                    Navigation(
                        navController = navController,
                    )
                }
            }
        }
    }
}




