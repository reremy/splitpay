package com.example.splitpay.ui.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel



@Composable
private fun HandleSignUpEvents(
    uiEvent: SharedFlow<SignUpUiEvent>,
    onNavigateToHome: () -> Unit,
    onNavigateToBack: () -> Unit
){
    if (!LocalInspectionMode.current){
        LaunchedEffect(Unit) {
            uiEvent.collect { event ->
                when (event) {
                    SignUpUiEvent.NavigateToHome -> onNavigateToHome()
                    SignUpUiEvent.NavigateToBack -> onNavigateToBack()
                }
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
    isError: Boolean = false,
    supportingText: String? = null,
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
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                errorContainerColor = Color(0xFF2D2D2D),
                errorIndicatorColor = Color.Transparent
            ),
            isError = isError,
            supportingText = {
                if (!supportingText.isNullOrEmpty()) {
                    Text(text = supportingText, color = Color.Red)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

    }
}

@Composable
    fun SignUpScreen(
        viewModel: SignUpViewModel = viewModel(),
        onNavigateToHome: () -> Unit,
        onNavigateToBack: () -> Unit
    ){

        HandleSignUpEvents(
            uiEvent = viewModel.uiEvent,
            onNavigateToHome = onNavigateToHome,
            onNavigateToBack = onNavigateToBack
        )

        val uiState = viewModel.uiState.collectAsState().value

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFF1E1E1E))
                .padding(16.dp),
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InputField(
                    label = "Full Name",
                    placeholder = "John Doe",
                    value = uiState.fullName,
                    onValueChange = { viewModel.onFullNameChange(it) },
                    isError = !uiState.fullNameError.isNullOrEmpty(),
                    supportingText = uiState.fullNameError,
                    modifier = Modifier
                        .testTag("fullNameTextField")

                )

                InputField(
                    label = "Username",
                    placeholder = "username",
                    value = uiState.username,
                    onValueChange = { viewModel.onUsernameChange(it) },
                    isError = !uiState.usernameError.isNullOrEmpty(),
                    supportingText = uiState.usernameError,
                    modifier = Modifier
                        .testTag("usernameTextField")
                )

                InputField(
                    label = "Email",
                    placeholder = "example@email.com",
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    keyboardType = KeyboardType.Email,
                    isError = !uiState.emailError.isNullOrEmpty(),
                    supportingText = uiState.emailError,
                    modifier = Modifier
                        .testTag("emailTextField")
                )

                InputField(
                    label = "Password",
                    placeholder = "password",
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = !uiState.passwordError.isNullOrEmpty(),
                    supportingText = uiState.passwordError,
                    modifier = Modifier
                        .testTag("passwordTextField")
                )

                InputField(
                    label = "Re-type Password",
                    placeholder = "Re-type password",
                    value = uiState.retypePassword,
                    onValueChange = { viewModel.onRetypePasswordChange(it) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = !uiState.retypePasswordError.isNullOrEmpty(),
                    supportingText = uiState.retypePasswordError,
                    modifier = Modifier
                        .testTag("retypePasswordTextField")
                )

                Spacer(modifier = Modifier.height(20.dp))


                Button(
                    onClick = { viewModel.onSignUpClick() },
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
                        .testTag("signUpButton"),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading){
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Sign Up",
                            fontSize = 20.sp
                        )
                    }
                }
            }
            if (uiState.errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
}

