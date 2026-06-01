package com.example.bakalariapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bakalariapp.data.api.RetrofitClient
import com.example.bakalariapp.data.preferences.TokenDataStore
import com.example.bakalariapp.data.repository.BakalariRepository
import com.example.bakalariapp.data.repository.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginViewModel(private val tokenDataStore: TokenDataStore) : ViewModel() {
    
    var uiState by mutableStateOf(LoginUiState())
        private set
    
    init {
        viewModelScope.launch {
            val savedUrl = tokenDataStore.baseUrl.first()
            if (savedUrl != null) {
                uiState = uiState.copy(serverUrl = savedUrl)
            }
        }
    }
    
    fun onServerUrlChange(url: String) {
        uiState = uiState.copy(serverUrl = url)
    }
    
    fun onUsernameChange(username: String) {
        uiState = uiState.copy(username = username)
    }
    
    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password)
    }
    
    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            
            val url = uiState.serverUrl.trim()
            val username = uiState.username.trim()
            val password = uiState.password
            
            if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Vyplňte všechna pole / Please fill all fields"
                )
                return@launch
            }
            
            val baseUrl = if (url.endsWith("/")) url else "$url/"
            
            try {
                val apiService = RetrofitClient.create(baseUrl)
                val repository = BakalariRepository(apiService)
                
                when (val result = repository.login(username, password)) {
                    is Result.Success -> {
                        tokenDataStore.saveTokens(
                            result.data.accessToken,
                            result.data.refreshToken
                        )
                        tokenDataStore.saveBaseUrl(url)
                        uiState = uiState.copy(isLoading = false, error = null)
                        onSuccess()
                    }
                    is Result.Error -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Connection error: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
