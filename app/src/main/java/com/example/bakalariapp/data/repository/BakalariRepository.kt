package com.example.bakalariapp.data.repository

import com.example.bakalariapp.data.api.BakalariApiService
import com.example.bakalariapp.data.model.AbsenceResponse
import com.example.bakalariapp.data.model.LoginResponse
import com.example.bakalariapp.data.model.MarksResponse
import com.example.bakalariapp.data.model.Subject
import com.example.bakalariapp.data.model.UserResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

class BakalariRepository(private val apiService: BakalariApiService) {
    
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(username = username, password = password)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response body")
            } else {
                val errorBody = response.errorBody()?.string()
                Result.Error(errorBody ?: "Login failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
    
    suspend fun getMarks(accessToken: String): Result<MarksResponse> {
        return try {
            val auth = "Bearer $accessToken"
            val response = apiService.getMarks(auth)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response body")
            } else {
                Result.Error("Failed to load marks: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
    
    suspend fun getUser(accessToken: String): Result<UserResponse> {
        return try {
            val auth = "Bearer $accessToken"
            val response = apiService.getUser(auth)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response body")
            } else {
                Result.Error("Failed to load user: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
    
    suspend fun getAbsence(accessToken: String): Result<AbsenceResponse> {
        return try {
            val auth = "Bearer $accessToken"
            val response = apiService.getAbsence(auth)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
                } ?: Result.Error("Empty response body")
            } else {
                Result.Error("Failed to load absence: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
    
    fun calculateWeightedAverage(marks: List<com.example.bakalariapp.data.model.Mark>): Double {
        var totalWeight = 0
        var weightedSum = 0.0
        
        marks.forEach { mark ->
            if (!mark.isPoints) {
                val gradeValue = parseGrade(mark.markText)
                val weight = mark.weight ?: 1
                if (gradeValue != null) {
                    weightedSum += gradeValue * weight
                    totalWeight += weight
                }
            }
        }
        
        return if (totalWeight > 0) weightedSum / totalWeight else 0.0
    }
    
    private fun parseGrade(markText: String): Double? {
        return when (markText.trim()) {
            "1" -> 1.0
            "1-" -> 1.5
            "1+" -> 1.0
            "2" -> 2.0
            "2-" -> 2.5
            "2+" -> 1.5
            "3" -> 3.0
            "3-" -> 3.5
            "3+" -> 2.5
            "4" -> 4.0
            "4-" -> 4.5
            "4+" -> 3.5
            "5" -> 5.0
            "5-" -> 5.0
            "5+" -> 4.5
            else -> markText.replace(",", ".").toDoubleOrNull()
        }
    }
}
