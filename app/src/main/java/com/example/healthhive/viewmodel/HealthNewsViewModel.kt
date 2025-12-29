// File: com/example/healthhive/viewmodel/HealthNewsViewModel.kt

package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.NewsRepository
// CRITICAL IMPORT: Points to the model sub-package
import com.example.healthhive.data.model.HealthArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the News section.
 * Using the HealthArticle from the .model package.
 */
data class HealthNewsState(
    val articles: List<HealthArticle> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HealthNewsViewModel : ViewModel() {

    private val repository = NewsRepository()

    private val _uiState = MutableStateFlow(HealthNewsState())
    val uiState: StateFlow<HealthNewsState> = _uiState.asStateFlow()

    init {
        fetchNews()
    }

    fun fetchNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val news = repository.getLatestHealthInsights()
                if (news.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "No recent health insights found.")
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, articles = news)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not sync news. Check connection: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}