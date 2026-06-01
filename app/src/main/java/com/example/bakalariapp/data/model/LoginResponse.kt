package com.example.bakalariapp.data.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("bak:ApiVersion") val apiVersion: String?,
    @SerializedName("bak:AppVersion") val appVersion: String?,
    @SerializedName("bak:UserId") val userId: String?
)

data class LoginRequest(
    val clientId: String = "ANDR",
    val grantType: String,
    val username: String? = null,
    val password: String? = null,
    val refreshToken: String? = null
)

data class LoginError(
    val error: String,
    @SerializedName("error_description") val errorDescription: String
)
