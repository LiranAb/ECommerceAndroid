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