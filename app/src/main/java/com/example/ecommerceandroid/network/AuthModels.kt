package com.example.ecommerceandroid.network


data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class AuthResponse(
    val message: String,
    val token: String,
    val user: UserDto
)

data class CartResponse(
    val items: List<CartItem>
)

data class CartItem(
    val product: ProductInCart?,
    val name: String,
    val image: String,
    val price: Double,
    val quantity: Int
)

data class ProductInCart(
    val _id: String,
    val name: String,
    val image: String,
    val price: Double,
    val countInStock: Int
)