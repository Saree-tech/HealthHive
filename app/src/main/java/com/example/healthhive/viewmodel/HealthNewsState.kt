package com.example.healthhive.viewmodel

import com.example.healthhive.data.HealthArticle

data class HealthNewsState(
    val isLoading: Boolean = false,
    val articles: List<HealthArticle> = emptyList(),
    val errorMessage: String? = null
)