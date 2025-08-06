package com.example.splitpay.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun HomeScreen(
    onLogout: () -> Unit
){
    //get current user info from Firebase
    var user by remember { mutableStateOf<FirebaseUser?>(null) }

    LaunchedEffect(Unit){
        user = FirebaseAuth.getInstance().currentUser
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        val currentUser = user
        if(currentUser != null){
            Text(
                text = "Welcome, ${currentUser.displayName ?: "User"}!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ){
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ){
                    Text("Full Name: ${currentUser.displayName ?: "Not Set"}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: ${currentUser.email ?: "Not Set"}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("User ID: ${currentUser.uid}", fontSize = 16.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = Color(0xFF1D6985)
                )
            ){
                Text("Log Out")
            }
        } else {
            CircularProgressIndicator()
        }
    }
}