package com.example.splitpay.ui.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.ui.common.ButtonSizeModifier
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.BorderGray


//@Composable
//private fun HandleWelcomeEvents(
//    uiEvent: SharedFlow<WelcomeUiEvent>,
//    onNavigateToSignUp: () -> Unit,
//    onNavigateToLogIn: () -> Unit
//){
//    val isPreview = isInPreview()
//
//    if (!isPreview){
//        LaunchedEffect(Unit) {
//            uiEvent.collect { event ->
//                when (event) {
//                    WelcomeUiEvent.NavigateToSignUp -> onNavigateToSignUp()
//                    WelcomeUiEvent.NavigateToLogIn -> onNavigateToLogIn()
//                }
//            }
//        }
//    }
//}

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = viewModel(),
    onNavigateToSignUp: () -> Unit,
    onNavigateToLogIn: () -> Unit
) {

    if (!LocalInspectionMode.current) {
        UiEventHandler(viewModel.uiEvent) { event ->
            when (event) {
                WelcomeUiEvent.NavigateToSignUp -> onNavigateToSignUp()
                WelcomeUiEvent.NavigateToLogIn -> onNavigateToLogIn()
            }
        }
    }



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
            onClick = { viewModel.onSignUpClick() },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color(0xFF1D6985)
            ),
            border = BorderStroke(width = 1.dp, color = Color(0xFF747474)),
            modifier = ButtonSizeModifier
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
            onClick = { viewModel.onLogInClick() },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color(0xFF1E1E1E)
            ),
            border = BorderStroke(width = 1.dp, color = BorderGray),
            modifier = ButtonSizeModifier
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

@Preview(showBackground = true, name = "Welcome Screen")
@Composable
fun WelcomeScreenPreview() {
    // Wrap in a dark theme
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            background = Color(0xFF1E1E1E),
            primary = Color(0xFF1D6985)
        )
    ) {
        WelcomeScreen(
            viewModel = viewModel(),
            onNavigateToSignUp = {},
            onNavigateToLogIn = {}
        )
    }
}