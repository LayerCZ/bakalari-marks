package com.example.bakalariapp.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    
    fun create(baseUrl: String): BakalariApiService {
        // Logging disabled for performance - enable only for debugging
        // val loggingInterceptor = HttpLoggingInterceptor().apply {
        //     level = HttpLoggingInterceptor.Level.BODY
        // }
        
        val client = OkHttpClient.Builder()
            // .addInterceptor(loggingInterceptor)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(BakalariApiService::class.java)
    }
}
