package com.example.bakalariapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bakalariapp.data.api.RetrofitClient
import com.example.bakalariapp.data.model.AbsencePerSubject
import com.example.bakalariapp.data.model.Mark
import com.example.bakalariapp.data.model.MarksResponse
import com.example.bakalariapp.data.model.Subject
import com.example.bakalariapp.data.preferences.MarksCacheDataStore
import com.example.bakalariapp.data.preferences.TokenDataStore
import com.example.bakalariapp.data.repository.BakalariRepository
import com.example.bakalariapp.data.repository.Result
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MarksViewModel(
    private val tokenDataStore: TokenDataStore,
    private val marksCacheDataStore: MarksCacheDataStore
) : ViewModel() {
    
    var uiState by mutableStateOf(MarksUiState())
        private set
    
    init {
        // Delay initial load to allow token to be saved after login
        viewModelScope.launch {
            delay(500)
            loadMarks()
        }
    }
    
    fun loadMarks(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val accessToken = tokenDataStore.accessToken.first()
            val baseUrl = tokenDataStore.baseUrl.first()
            
            if (accessToken == null || baseUrl == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Not logged in"
                )
                return@launch
            }
            
            // Clear cache if forcing refresh
            if (forceRefresh) {
                marksCacheDataStore.clearCache()
            }
            
            // First, try to load from cache immediately (only if not forcing refresh)
            val cachedMarks = marksCacheDataStore.cachedMarks.first()
            if (!forceRefresh && cachedMarks != null && uiState.subjects.isEmpty()) {
                val subjects = processSubjects(cachedMarks.subjects)
                uiState = uiState.copy(
                    isLoading = true, // Still loading from API
                    subjects = subjects,
                    error = null
                )
            } else {
                // Always show loading when fetching
                uiState = uiState.copy(isLoading = true, error = null)
            }
            
            val fullUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            
            var absencesPerSubject = emptyList<AbsencePerSubject>()
            
            try {
                val apiService = RetrofitClient.create(fullUrl)
                val repository = BakalariRepository(apiService)
                
                // Fetch both marks and absences
                val marksResult = repository.getMarks(accessToken)
                val absenceResult = repository.getAbsence(accessToken)
                
                // Process absences with logging
                absencesPerSubject = when (absenceResult) {
                    is Result.Success -> {
                        val absences = absenceResult.data.absencesPerSubject
                        android.util.Log.d("MarksViewModel", "Loaded ${absences.size} absence records")
                        absences
                    }
                    is Result.Error -> {
                        android.util.Log.e("MarksViewModel", "Failed to load absences: ${absenceResult.message}")
                        emptyList()
                    }
                    else -> emptyList()
                }
                
                when (marksResult) {
                    is Result.Success -> {
                        // Save to cache
                        marksCacheDataStore.saveMarks(marksResult.data)
                        
                        val subjects = processSubjects(marksResult.data.subjects)
                        uiState = uiState.copy(
                            isLoading = false,
                            subjects = subjects,
                            absencesPerSubject = absencesPerSubject,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        // If we have cached data, don't show error, just keep cached data
                        if (cachedMarks != null) {
                            uiState = uiState.copy(
                                isLoading = false,
                                absencesPerSubject = absencesPerSubject
                            )
                        } else {
                            uiState = uiState.copy(
                                isLoading = false,
                                error = marksResult.message,
                                absencesPerSubject = absencesPerSubject
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                // If we have cached data, don't show error
                if (cachedMarks != null) {
                    uiState = uiState.copy(
                        isLoading = false,
                        absencesPerSubject = absencesPerSubject
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Error: ${e.message}",
                        absencesPerSubject = absencesPerSubject
                    )
                }
            }
        }
    }
    
    private fun processSubjects(subjects: List<Subject>): List<Subject> {
        return subjects.map { subject ->
            // Calculate average if API doesn't provide it
            if (subject.averageText.isNullOrBlank() && subject.marks.isNotEmpty()) {
                val calculatedAvg = calculateAverage(subject.marks)
                subject.copy(averageText = calculatedAvg)
            } else {
                subject
            }
        }
    }
    
    fun selectSubject(subject: Subject?) {
        uiState = uiState.copy(selectedSubject = subject)
    }
    
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            tokenDataStore.clearTokens()
            marksCacheDataStore.clearCache()
            uiState = MarksUiState() // Reset UI state
            onLoggedOut()
        }
    }
}

data class MarksUiState(
    val isLoading: Boolean = false,
    val subjects: List<Subject> = emptyList(),
    val selectedSubject: Subject? = null,
    val absencesPerSubject: List<AbsencePerSubject> = emptyList(),
    val error: String? = null
)

private fun calculateAverage(marks: List<Mark>): String {
    // Calculate weighted average using only numeric marks (not points)
    val numericMarks = marks.filter { !it.isPoints }
    
    if (numericMarks.isEmpty()) return "0,00"
    
    var totalWeight = 0
    var weightedSum = 0.0
    
    for (mark in numericMarks) {
        val value = parseMarkValue(mark.markText)
        if (value != null) {
            val weight = mark.weight ?: 1
            totalWeight += weight
            weightedSum += value * weight
        }
    }
    
    return if (totalWeight > 0) {
        val avg = weightedSum / totalWeight
        // Format with Czech decimal comma
        String.format(Locale.US, "%.2f", avg).replace(".", ",")
    } else {
        "0,00"
    }
}

private fun parseMarkValue(markText: String): Double? {
    return when (markText.trim()) {
        "1" -> 1.0
        "1+" -> 1.3
        "1-" -> 1.7
        "2" -> 2.0
        "2+" -> 2.3
        "2-" -> 2.7
        "3" -> 3.0
        "3+" -> 3.3
        "3-" -> 3.7
        "4" -> 4.0
        "4+" -> 4.3
        "4-" -> 4.7
        "5" -> 5.0
        "5+" -> 5.3
        "5-" -> 5.7
        else -> markText.replace(",", ".").toDoubleOrNull()
    }
}

class MarksViewModelFactory(
    private val tokenDataStore: TokenDataStore,
    private val marksCacheDataStore: MarksCacheDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarksViewModel::class.java)) {
            return MarksViewModel(tokenDataStore, marksCacheDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
