package com.example.splitpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitpay.ui.theme.SplitpayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitpayTheme {
                LoginScreenUI()
            }
        }
    }
}


@Composable
fun UserRegistrationPage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xFF1E1E1E)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(
            text = "SplitPay",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = { /*TODO*/ },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color(0xFF1D6985)
            ),
            border = BorderStroke(width = 1.dp, color = Color(0xFF747474)),
            modifier = Modifier
                .size(width = 300.dp, height = 60.dp)
                .testTag("createAccountButton"),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text(
                text = "Create Account",
                fontSize = 20.sp
            )

        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { /*TODO*/ },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color(0xFF1E1E1E)
            ),
            border = BorderStroke(width = 1.dp, color = Color(0xFF747474)),
            modifier = Modifier
                .size(width = 300.dp, height = 60.dp)
                .testTag("logInButton"),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text(
                text = "Log In",
                fontSize = 20.sp
            )

        }
    }
}

@Preview
@Composable
fun LoginScreenUI() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xFF1E1E1E)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Log In",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 100.dp, bottom = 48.dp)
        )

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Email:",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(5.dp))
            var email by remember { mutableStateOf("") } //to remember the text entered
            TextField(
                modifier = Modifier
                    .size(width = 300.dp, height = 60.dp)
                    .testTag("emailTextField"),
                value = email,
                placeholder = { Text(text = "example@email.com") },
                onValueChange = { email = it },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Password:",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(5.dp))
            var password by remember { mutableStateOf("") } //to remember the text entered
            TextField(
                modifier = Modifier
                    .size(width = 300.dp, height = 60.dp)
                    .testTag("emailTextField"),
                value = password,
                placeholder = { Text(text = "password") },
                onValueChange = { password = it },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(50.dp))

        Button(
            onClick = { /*TODO*/ },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color(0xFF1D6985)
            ),
            border = BorderStroke(width = 1.dp, color = Color(0xFF747474)),
            modifier = Modifier
                .size(width = 300.dp, height = 60.dp)
                .testTag("logInButton"),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text(
                text = "Log In",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun RegistrationScreenUI(){
    Column(){

    }
}

