package com.example.minitasker.ui.screen.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.minitasker.ui.components.ErrorText
import com.example.minitasker.ui.components.LabeledTextField
import com.example.minitasker.ui.components.PrimaryButton

@Composable
fun AuthScreen(viewModel: AuthViewModel, onAuthenticated: () -> Unit) {
    val state by viewModel.state.collectAsState()

    if (state.isAuthenticated) {
        onAuthenticated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (state.isRegister) "Регистрация" else "Вход",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        LabeledTextField(value = state.email, onValueChange = viewModel::updateEmail, label = "Email")
        LabeledTextField(value = state.password, onValueChange = viewModel::updatePassword, label = "Пароль", isPassword = true)
        if (state.isRegister) {
            LabeledTextField(value = state.fullName, onValueChange = viewModel::updateFullName, label = "Имя")
        }
        ErrorText(message = state.error)
        if (state.loading) {
            CircularProgressIndicator()
        } else {
            PrimaryButton(text = if (state.isRegister) "Создать аккаунт" else "Войти", onClick = viewModel::submit)
        }
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = if (state.isRegister) "У меня уже есть аккаунт" else "Регистрация",
            onClick = viewModel::toggleMode
        )
    }
}
