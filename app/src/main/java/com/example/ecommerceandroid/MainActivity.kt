package com.example.ecommerceandroid

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ecommerceandroid.network.AddToCartRequest
import com.example.ecommerceandroid.network.CartItem
import com.example.ecommerceandroid.network.LoginRequest
import com.example.ecommerceandroid.network.ProductDto
import com.example.ecommerceandroid.network.RetrofitClient
import com.example.ecommerceandroid.ui.theme.ECommerceAndroidTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(false) }

            ECommerceAndroidTheme(darkTheme = isDarkMode) {
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                var userName by rememberSaveable { mutableStateOf("") }
                var token by rememberSaveable { mutableStateOf("") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoggedIn) {
                        HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            userName = userName,
                            token = token,
                            isDarkMode = isDarkMode,
                            onDarkModeChange = { isDarkMode = it },
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
            .verticalScroll(rememberScrollState())
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
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var currentScreen by rememberSaveable { mutableStateOf("home") }
    var productFilter by rememberSaveable { mutableStateOf("") }

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
            FilteredProductsScreen(
                modifier = modifier,
                token = token,
                initialFilter = productFilter,
                onBack = {
                    productFilter = ""
                    currentScreen = "home"
                }
            )
        }

        else -> {
            var searchTerm by rememberSaveable { mutableStateOf("") }
            var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
            var isLoading by remember { mutableStateOf(false) }
            var loadMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                isLoading = true
                loadMessage = ""

                try {
                    products = RetrofitClient.authApi.getProducts().products
                } catch (e: Exception) {
                    loadMessage = "Could not load featured products"
                } finally {
                    isLoading = false
                }
            }

            val trendingProducts = products.take(4)
            val categories = products
                .map { it.category }
                .filter { it.isNotBlank() }
                .distinct()

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "E-Commerce",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Welcome, $userName",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isDarkMode) "Day" else "Night")
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onDarkModeChange
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Fresh technology,\nfast picks.",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Discover phones, laptops and accessories selected for you.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        OutlinedTextField(
                            value = searchTerm,
                            onValueChange = { searchTerm = it },
                            label = { Text("Search products") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    productFilter = searchTerm.trim()
                                    currentScreen = "products"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Search")
                            }
                            OutlinedButton(
                                onClick = { currentScreen = "cart" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cart")
                            }
                        }
                    }
                }

                HomeSectionTitle(
                    title = "Trending Now",
                    subtitle = "Popular products customers are checking out.",
                    onViewAll = {
                        productFilter = ""
                        currentScreen = "products"
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (loadMessage.isNotBlank()) {
                    Text(
                        text = loadMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    trendingProducts.forEach { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    productFilter = product.name
                                    currentScreen = "products"
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            ProductRow(product = product, onClick = {
                                productFilter = product.name
                                currentScreen = "products"
                            })
                        }
                    }
                }

                Text(
                    text = "Shop by Category",
                    style = MaterialTheme.typography.headlineSmall
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categories.forEach { category ->
                        OutlinedButton(
                            onClick = {
                                productFilter = category
                                currentScreen = "products"
                            }
                        ) {
                            Text(category)
                        }
                    }
                }

                Text(
                    text = "Why Shop With Us",
                    style = MaterialTheme.typography.headlineSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TrustCard("24h", "Fast Delivery", Modifier.weight(1f))
                    TrustCard("SSL", "Secure Checkout", Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TrustCard("30d", "Easy Returns", Modifier.weight(1f))
                    TrustCard("VIP", "Helpful Support", Modifier.weight(1f))
                }

                OutlinedButton(
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
fun HomeSectionTitle(
    title: String,
    subtitle: String,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onViewAll) {
            Text("View All")
        }
    }
}

@Composable
fun TrustCard(mark: String, title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(mark, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun OldHomeScreen(
    modifier: Modifier = Modifier,
    userName: String,
    token: String,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var currentScreen by rememberSaveable { mutableStateOf("home") }

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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome $userName",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!isDarkMode) "Day Mode" else "Night Mode"
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange
                    )
                }

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
fun FilteredProductsScreen(
    modifier: Modifier = Modifier,
    token: String,
    initialFilter: String = "",
    onBack: () -> Unit
) {
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }

    val visibleProducts = products.filter { product ->
        initialFilter.isBlank() ||
                product.name.contains(initialFilter, ignoreCase = true) ||
                product.category.contains(initialFilter, ignoreCase = true)
    }

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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (initialFilter.isBlank()) "Products" else "Products: $initialFilter",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (message.isNotBlank()) {
            Text(message)
        } else if (visibleProducts.isEmpty()) {
            Text("No products found")
        } else {
            visibleProducts.forEach { product ->
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
            .verticalScroll(rememberScrollState())
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
    var quantity by rememberSaveable(product._id) { mutableStateOf(1) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

        if (product.countInStock > 0) {
            Text(
                text = "Quantity",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (quantity > 1) {
                            quantity--
                        }
                    },
                    enabled = quantity > 1 && !isAdding
                ) {
                    Text("-")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (quantity < product.countInStock) {
                            quantity++
                        }
                    },
                    enabled = quantity < product.countInStock && !isAdding
                ) {
                    Text("+")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

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
                                quantity = quantity
                            )
                        )

                        addMessage = "Added $quantity to cart"
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
                Text("Add $quantity to Cart")
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

    val totalPrice = items.sumOf { item ->
        item.price * item.quantity
    }

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
            .verticalScroll(rememberScrollState())
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total: ${"%.2f".format(totalPrice)} ₪",
                style = MaterialTheme.typography.titleMedium
            )
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
