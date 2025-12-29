// File: com/example/healthhive/data/NewsRepository.kt
package com.example.healthhive.data

import com.example.healthhive.BuildConfig
import com.example.healthhive.data.model.HealthArticle
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository responsible for fetching global health insights.
 */
class NewsRepository {

    // 1. Create a custom OkHttpClient
    // NewsAPI blocks mobile requests that don't have a browser-like User-Agent.
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            chain.proceed(request)
        }
        .build()

    // 2. Initialize Retrofit with the custom client
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://newsapi.org/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 3. Create the API Service implementation
    private val apiService: NewsApiService = retrofit.create(NewsApiService::class.java)

    /**
     * Fetches global health articles in English.
     */
    suspend fun getLatestHealthInsights(): List<HealthArticle> {
        return try {
            // Pulls the API Key from your build.gradle / local.properties
            val apiKey = BuildConfig.NEWS_API_KEY

            // We omit the 'country' parameter to get news from all over the world
            val response = apiService.getHealthNews(
                category = "health",
                language = "en",
                apiKey = apiKey
            )

            if (response.status == "ok") {
                // Filter out articles that have been removed or have broken titles
                response.articles.filter { it.title != "[Removed]" && it.title.isNotBlank() }
            } else {
                // Log the status or error message if needed
                emptyList()
            }
        } catch (e: Exception) {
            // Prints network or parsing errors to the console
            e.printStackTrace()
            emptyList()
        }
    }
}