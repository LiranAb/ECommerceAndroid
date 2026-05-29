package com.example.ecommerceandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ecommerceandroid.network.CartItem
import com.example.ecommerceandroid.network.LoginRequest
import com.example.ecommerceandroid.network.RetrofitClient
import com.example.ecommerceandroid.ui.theme.ECommerceAndroidTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ECommerceAndroidTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                var userName by remember { mutableStateOf("") }
                var token by remember { mutableStateOf("") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoggedIn) {
                        HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            userName = userName,
                            token = token,
                            onLogout = {
                                userName = ""
                                token = ""
                                isLoggedIn = false
                            }
                        )
                    } else {
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoginSuccess = { name, userToken ->
                                userName = name
                                token = userToken
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    message = ""

                    try {
                        val response = RetrofitClient.authApi.login(
                            LoginRequest(
                                email = email,
                                password = password
                            )
                        )

                        message = "Welcome ${response.user.name}"
                        onLoginSuccess(response.user.name, response.token)
                    } catch (e: Exception) {
                        message = "Login failed: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userName: String,
    token: String,
    onLogout: () -> Unit
) {
    var showCart by remember { mutableStateOf(false) }

    if (showCart) {
        CartScreen(
            modifier = modifier,
            token = token,
            onBack = {
                showCart = false
            }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome $userName",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    showCart = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Cart")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun CartScreen(
    modifier: Modifier = Modifier,
    token: String,
    onBack: () -> Unit
) {
    var items by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(token) {
        isLoading = true
        message = ""

        try {
            val cart = RetrofitClient.authApi.getCart("Bearer $token")
            items = cart.items
        } catch (e: Exception) {
            message = "Failed to load cart: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Shopping Cart",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (message.isNotBlank()) {
            Text(message)
        } else if (items.isEmpty()) {
            Text("Your cart is empty")
        } else {
            items.forEach { item ->
                Text("${item.name} x${item.quantity} -   ${item.price} ₪")
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")
        }
    }
}