// File: com/example/healthhive/data/NewsApiService.kt
package com.example.healthhive.data

import com.example.healthhive.data.model.HealthArticle
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface for NewsAPI.org
 * To get global news: We omit the 'country' parameter and only use 'category'.
 */
interface NewsApiService {

    @GET("v2/top-headlines")
    suspend fun getHealthNews(
        @Query("category") category: String = "health",
        @Query("language") language: String = "en", // Ensures global results are in English
        @Query("apiKey") apiKey: String
    ): NewsApiResponse
}

/**
 * Root response object from NewsAPI
 */
data class NewsApiResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<HealthArticle>
)