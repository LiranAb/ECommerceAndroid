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

data class ProductsResponse(
    val products: List<ProductDto>,
    val page: Int,
    val pages: Int,
    val totalProducts: Int
)

data class ProductDto(
    val _id: String,
    val name: String,
    val description: String,
    val price: Double,
    val discountedPrice: Double?,
    val hasDiscount: Boolean = false,
    val discountPercentage: Double = 0.0,
    val image: String = "",
    val category: String,
    val countInStock: Int,
    val rating: Double = 0.0,
    val numReviews: Int = 0
)