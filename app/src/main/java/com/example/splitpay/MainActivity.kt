package com.example.splitpay

import com.example.splitpay.navigation.Navigation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            val currentUser = auth.currentUser
            val startDestination = if (currentUser != null) "home" else "welcome"
            val navController = rememberNavController()

            Surface(Modifier.fillMaxSize()) {
                Navigation(
                    navController = navController,
                    startDestination = startDestination,
                    //onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}




@Composable
fun WelcomeScreenUI() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(
            text = "SplitPay",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,

            )
        Spacer(Modifier.height(72.dp))

        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                //contentColor = Color.White,
                containerColor = Color(0xFF1D6985)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFF747474)
            ),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 32.dp)
                .testTag("signUpButton")
        ) {
            Text(
                text = "Sign Up",
                fontSize = 20.sp
            )

        }
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White // Text/icon color
            ),
            border = BorderStroke(1.dp, Color(0xFF747474)),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 32.dp)
                .testTag("logInButton")
        ) {
            Text(text = "Log In", fontSize = 20.sp)
        }
    }
}

//@Preview
@Composable
fun OnboardingScreen() {
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
                .testTag("signUpButton"),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text(
                text = "Sign Up",
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
fun SignUpScreenUI() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xFF1E1E1E)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign Up",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 100.dp, bottom = 48.dp)
                .testTag("signUpText")
        )

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            //full name
            Text(
                text = "Full Name:",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("fullNameText")
            )
            Spacer(modifier = Modifier.height(5.dp))
            var fullName by remember { mutableStateOf("") } //to remember the text entered
            TextField(
                modifier = Modifier
                    .size(width = 300.dp, height = 60.dp)
                    .testTag("fullNameTextField"),
                value = fullName,
                placeholder = { Text(text = "John Doe") },
                onValueChange = { fullName = it },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(15.dp))

            //username
            Text(
                text = "Username:",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("usernameText")
            )
            Spacer(modifier = Modifier.height(5.dp))
            var username by remember { mutableStateOf("") } //to remember the text entered
            TextField(
                modifier = Modifier
                    .size(width = 300.dp, height = 60.dp)
                    .testTag("usernameTextField"),
                value = username,
                placeholder = { Text(text = "username") },
                onValueChange = { username = it },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(15.dp))

            //email
            Text(
                text = "Email:",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("emailText")
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

            //password
            Text(
                text = "Password:",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("passwordText")
            )
            Spacer(modifier = Modifier.height(5.dp))
            var password by remember { mutableStateOf("") } //to remember the text entered
            TextField(
                modifier = Modifier
                    .size(width = 300.dp, height = 60.dp)
                    .testTag("passwordTextField"),
                value = password,
                placeholder = { Text(text = "password") },
                onValueChange = { password = it },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(15.dp))

            //retype password
            Text(
                text = "Re-type Password:",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("retypePasswordText")
            )
            Spacer(modifier = Modifier.height(5.dp))
            var retypePassword by remember { mutableStateOf("") } //to remember the text entered
            TextField(
                modifier = Modifier
                    .size(width = 300.dp, height = 60.dp)
                    .testTag("retypePasswordTextField"),
                value = retypePassword,
                placeholder = { Text(text = "password") },
                onValueChange = { retypePassword = it },
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
                .testTag("signUpButton"),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text(
                text = "Sign Up",
                fontSize = 20.sp
            )
        }
    }
}

@Preview
@Composable
fun SignUpScreenUI2(){

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "Sign Up",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 100.dp, bottom = 48.dp)
                .testTag("signUpText")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            InputField(
                label = "Full Name",
                placeholder = "John Doe",
                value = fullName,
                onValueChange = {fullName = it},
                modifier = Modifier

                    .testTag("fullNameTextField")
            )

            InputField(
                label = "Username",
                placeholder = "username",
                value = username,
                onValueChange = {username = it},
                modifier = Modifier

                    .testTag("usernameTextField")
            )

            InputField(
                label = "Email",
                placeholder = "example@email.com",
                value = email,
                onValueChange = {email = it},
                keyboardType = KeyboardType.Email,
                modifier = Modifier

                    .testTag("emailTextField")
            )

            InputField(
                label = "Password",
                placeholder = "password",
                value = password,
                onValueChange = {password = it},
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier

                    .testTag("passwordTextField")
            )

            InputField(
                label = "Re-type Password",
                placeholder = "password",
                value = retypePassword,
                onValueChange = {retypePassword = it},
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    
                    .testTag("retypePasswordTextField")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = Color(0xFF1D6985)
                ),
                border = BorderStroke(width = 1.dp, color = Color(0xFF747474)),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("signUpButton")
            ) {
                Text(
                    text = "Sign Up",
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun InputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start

    ){
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFFAAAAAA)
                )
            },
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2D2D2D),
                unfocusedContainerColor = Color(0xFF2D2D2D),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                //textColor = Color.White,
                //placeholderColor = Color(0xFFAAAAAA)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
