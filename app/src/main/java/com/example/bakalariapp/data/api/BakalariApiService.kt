package com.example.bakalariapp.data.api

import com.example.bakalariapp.data.model.AbsenceResponse
import com.example.bakalariapp.data.model.LoginResponse
import com.example.bakalariapp.data.model.MarksResponse
import com.example.bakalariapp.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface BakalariApiService {
    
    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(
        @Field("client_id") clientId: String = "ANDR",
        @Field("grant_type") grantType: String = "password",
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>
    
    @FormUrlEncoded
    @POST("api/login")
    suspend fun refreshToken(
        @Field("client_id") clientId: String = "ANDR",
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String
    ): Response<LoginResponse>
    
    @GET("api/3/marks")
    suspend fun getMarks(
        @Header("Authorization") authorization: String
    ): Response<MarksResponse>
    
    @GET("api/3/user")
    suspend fun getUser(
        @Header("Authorization") authorization: String
    ): Response<UserResponse>
    
    @GET("api/3/absence/student")
    suspend fun getAbsence(
        @Header("Authorization") authorization: String
    ): Response<AbsenceResponse>
}
