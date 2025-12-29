package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HealthNewsViewModel : ViewModel() {

    private val repository = NewsRepository()

    // 1. Internal state (mutable)
    private val _uiState = MutableStateFlow(HealthNewsState())

    // 2. External state (read-only for the UI)
    val uiState: StateFlow<HealthNewsState> = _uiState.asStateFlow()

    init {
        fetchNews()
    }

    fun fetchNews() {
        viewModelScope.launch {
            // Start loading and clear previous errors
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
                    it.copy(isLoading = false, errorMessage = "Could not sync news. Check connection.")
                }
            }
        }
    }
}