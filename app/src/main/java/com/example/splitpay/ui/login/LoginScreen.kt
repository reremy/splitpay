package com.example.splitpay.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.ui.common.UiEventHandler


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

        Spacer(modifier = Modifier.height(5.dp))

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
                errorContainerColor = Color(0xFF2D2D2D),

                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,

                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                errorTextColor = Color.White
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
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToBack: () -> Unit
) {

//    LaunchedEffect(uiState.loginSuccess) {
//        if (uiState.loginSuccess) {
//            onNavigateToHome()
//        }
//    }

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            LoginUiEvent.NavigateToHome -> onNavigateToHome()
            LoginUiEvent.NavigateToBack -> onNavigateToBack()
        }
    }

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
            text = "Log In",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 100.dp, bottom = 48.dp)
                .testTag("logInText")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

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
                visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = !uiState.passwordError.isNullOrEmpty(),
                supportingText = uiState.passwordError,
                modifier = Modifier
                    .testTag("passwordTextField")
            )

            Spacer(modifier = Modifier.height(20.dp))


            Button(
                onClick = { viewModel.onLoginClick() },
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
                    .testTag("logInButton"),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Log In",
                        fontSize = 20.sp
                    )
                }
            }
        }
        if (!uiState.generalError.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.generalError,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

