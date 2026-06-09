package com.example.ecommerceandroid

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ecommerceandroid.network.AddToCartRequest
import com.example.ecommerceandroid.network.CartItem
import com.example.ecommerceandroid.network.LoginRequest
import com.example.ecommerceandroid.network.ProductDto
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
    var currentScreen by remember { mutableStateOf("home") }

    when (currentScreen) {
        "cart" -> {
            CartScreen(
                modifier = modifier,
                token = token,
                onBack = {
                    currentScreen = "home"
                }
            )
        }

        "products" -> {
            ProductsScreen(
                modifier = modifier,
                token = token,
                onBack = {
                    currentScreen = "home"
                }
            )
        }

        else -> {
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
                        currentScreen = "products"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Products")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        currentScreen = "cart"
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
}

@Composable
fun ProductsScreen(
    modifier: Modifier = Modifier,
    token: String,
    onBack: () -> Unit
) {
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }

    if (selectedProduct != null) {
        ProductDetailsScreen(
            product = selectedProduct!!,
            token = token,
            onBack = {
                selectedProduct = null
            }
        )
        return
    }

    LaunchedEffect(Unit) {
        isLoading = true
        message = ""

        try {
            val response = RetrofitClient.authApi.getProducts()
            products = response.products
        } catch (e: Exception) {
            message = "Failed to load products: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Products",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (message.isNotBlank()) {
            Text(message)
        } else if (products.isEmpty()) {
            Text("No products found")
        } else {
            products.forEach { product ->
                ProductRow(
                    product = product,
                    onClick = {
                        selectedProduct = product
                    }
                )
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

@Composable
fun ProductRow(
    product: ProductDto,
    onClick: () -> Unit
) {
    val imageModel = rememberImageModel(product.image)
    val bitmap = rememberBase64Bitmap(product.image)
    val priceToShow = product.discountedPrice ?: product.price

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductImage(
            bitmap = bitmap,
            imageModel = imageModel,
            contentDescription = product.name
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(product.name)
            Text(product.category)
            Text("${priceToShow} ₪")

            if (product.countInStock > 0) {
                Text("In stock: ${product.countInStock}")
            } else {
                Text("Out of stock")
            }
        }
    }
}

@Composable
fun ProductDetailsScreen(
    product: ProductDto,
    token: String,
    onBack: () -> Unit
) {
    val imageModel = rememberImageModel(product.image)
    val bitmap = rememberBase64Bitmap(product.image)
    val priceToShow = product.discountedPrice ?: product.price

    var addMessage by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProductImage(
            bitmap = bitmap,
            imageModel = imageModel,
            contentDescription = product.name
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(product.category)

        Spacer(modifier = Modifier.height(8.dp))

        Text("${priceToShow} ₪")

        Spacer(modifier = Modifier.height(16.dp))

        Text(product.description)

        Spacer(modifier = Modifier.height(16.dp))

        if (product.countInStock > 0) {
            Text("In stock: ${product.countInStock}")
        } else {
            Text("Out of stock")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isAdding = true
                    addMessage = ""

                    try {
                        RetrofitClient.authApi.addToCart(
                            token = "Bearer $token",
                            request = AddToCartRequest(
                                productId = product._id,
                                quantity = 1
                            )
                        )

                        addMessage = "Added to cart"
                    } catch (e: Exception) {
                        addMessage = "Failed to add: ${e.message}"
                    } finally {
                        isAdding = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = product.countInStock > 0 && !isAdding
        ) {
            if (isAdding) {
                Text("Adding...")
            } else {
                Text("Add to Cart")
            }
        }

        if (addMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(addMessage)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Products")
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
                CartItemRow(item = item)
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

@Composable
fun CartItemRow(item: CartItem) {
    val image = item.image.ifBlank {
        item.product?.image ?: ""
    }

    val imageModel = rememberImageModel(image)
    val bitmap = rememberBase64Bitmap(image)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductImage(
            bitmap = bitmap,
            imageModel = imageModel,
            contentDescription = item.name
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(item.name)
            Text("x${item.quantity}")
            Text("${item.price} ₪")
        }
    }
}

@Composable
fun ProductImage(
    bitmap: android.graphics.Bitmap?,
    imageModel: String?,
    contentDescription: String
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        AsyncImage(
            model = imageModel,
            contentDescription = contentDescription,
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Crop
        )
    }
}

fun normalizeRawImage(image: String): String {
    return image
        .replace("\n", "")
        .replace("\r", "")
        .trim()
}

fun rememberImageModel(image: String): String? {
    val rawImage = normalizeRawImage(image)

    return when {
        rawImage.isBlank() -> null
        rawImage.startsWith("http://") || rawImage.startsWith("https://") -> rawImage
        rawImage.startsWith("/") -> "http://10.69.0.140:5000$rawImage"
        else -> null
    }
}

@Composable
fun rememberBase64Bitmap(image: String): android.graphics.Bitmap? {
    val rawImage = normalizeRawImage(image)

    val isUrlImage = rawImage.startsWith("http://") ||
            rawImage.startsWith("https://") ||
            rawImage.startsWith("/")

    return remember(rawImage) {
        if (rawImage.isNotBlank() && !isUrlImage) {
            try {
                val base64Text = rawImage.substringAfter("base64,", rawImage)
                val imageBytes = Base64.decode(base64Text, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}