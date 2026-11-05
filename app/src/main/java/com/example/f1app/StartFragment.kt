package com.example.f1app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.f1app.ui.theme.F1appTheme

@Composable
fun StartScreen(
    onRegistrationClick: () -> Unit,
    onLoginClick: (String, String) -> Unit,
    initialLogin: String = "",
    userName: String = "",
    registeredLogin: String = ""
) {
    var login by rememberSaveable(initialLogin) {
        mutableStateOf(initialLogin)
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var loginError by rememberSaveable {
        mutableStateOf(false)
    }

    var passwordError by rememberSaveable {
        mutableStateOf(false)
    }

    // Получаем логин после возвращения с регистрации.
    LaunchedEffect(registeredLogin) {
        if (registeredLogin.isNotBlank()) {
            login = registeredLogin
            password = ""
            loginError = false
            passwordError = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.start_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (userName.isNotBlank()) {
            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(
                    R.string.welcome_user,
                    userName
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = login,
            onValueChange = {
                login = it
                loginError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(stringResource(R.string.login))
            },
            isError = loginError,
            supportingText = if (loginError) {
                {
                    Text(stringResource(R.string.login_required))
                }
            } else {
                null
            }
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(stringResource(R.string.password))
            },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError,
            supportingText = if (passwordError) {
                {
                    Text(stringResource(R.string.password_required))
                }
            } else {
                null
            }
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                loginError = login.isBlank()
                passwordError = password.isBlank()

                if (!loginError && !passwordError) {
                    onLoginClick(login.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.login_button))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onRegistrationClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.registration))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    F1appTheme(darkTheme = true) {
        StartScreen(
            onRegistrationClick = {},
            onLoginClick = { _, _ -> }
        )
    }
}