package com.example.ecommerceandroid.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse

    @GET("auth/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): UserDto

    @GET("cart")
    suspend fun getCart(
        @Header("Authorization") token: String
    ): CartResponse

    @GET("products")
    suspend fun getProducts(): ProductsResponse
}