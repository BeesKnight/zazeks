package com.example.minitasker.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.minitasker.ui.components.ErrorText
import com.example.minitasker.ui.components.LabeledTextField
import com.example.minitasker.ui.theme.MiniTaskerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AuthViewModel, onAuthenticated: () -> Unit) {
    val state by viewModel.state.collectAsState()

    if (state.isAuthenticated) {
        onAuthenticated()
    }

    AuthScreenContent(
        state = state,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onFullNameChange = viewModel::updateFullName,
        onSubmit = viewModel::submit,
        onToggleMode = viewModel::toggleMode
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScreenContent(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
) {
    val title = if (state.isRegister) "Регистрация" else "Вход"
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(title = { Text(title) })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LabeledTextField(value = state.email, onValueChange = onEmailChange, label = "Email")
                    LabeledTextField(value = state.password, onValueChange = onPasswordChange, label = "Пароль", isPassword = true)
                    if (state.isRegister) {
                        LabeledTextField(value = state.fullName, onValueChange = onFullNameChange, label = "Имя")
                    }
                    ErrorText(message = state.error)
                    Button(
                        onClick = onSubmit,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = if (state.isRegister) "Создать аккаунт" else "Войти")
                    }
                    FilledTonalButton(
                        onClick = onToggleMode,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = if (state.isRegister) "У меня уже есть аккаунт" else "Регистрация")
                    }
                    if (state.loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    MiniTaskerTheme {
        AuthScreenContent(
            state = AuthUiState(email = "user@mail.com", password = "123456"),
            onEmailChange = {},
            onPasswordChange = {},
            onFullNameChange = {},
            onSubmit = {},
            onToggleMode = {}
        )
    }
}
