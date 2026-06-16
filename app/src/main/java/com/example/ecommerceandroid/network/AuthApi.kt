package com.example.ecommerceandroid.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import com.example.ecommerceandroid.OrderDto
import com.example.ecommerceandroid.UpdateCartQuantityRequest

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

    @POST("cart")
    suspend fun addToCart(
        @Header("Authorization") token: String,
        @Body request: AddToCartRequest
    ): CartResponse

    @GET("products")
    suspend fun getProducts(): ProductsResponse



    @GET("orders/my")
    suspend fun getMyOrders(
        @Header("Authorization") token: String
    ): List<OrderDto>

    @PUT("cart/{productId}")
    suspend fun updateCartItem(
        @Header("Authorization") token: String,
        @Path("productId") productId: String,
        @Body request: UpdateCartQuantityRequest
    ): CartResponse


    @DELETE("cart/{productId}")
    suspend fun removeCartItem(
        @Header("Authorization") token: String,
        @Path("productId") productId: String
    ): CartResponse
}