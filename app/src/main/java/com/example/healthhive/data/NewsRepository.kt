package com.example.healthhive.data

// MANUAL IMPORT: Replace with your actual namespace if it's different
import com.example.healthhive.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewsRepository {

    // 1. Initialize Retrofit
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://newsapi.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 2. Create the API Service implementation
    private val apiService: NewsApiService = retrofit.create(NewsApiService::class.java)

    // 3. The function that fetches the articles
    suspend fun getLatestHealthInsights(): List<HealthArticle> {
        return try {
            // Using NEWS_API_KEY which we defined in build.gradle
            val apiKey = BuildConfig.NEWS_API_KEY

            // Log for debugging (Remove in production)
            println("Fetching news with key: ${apiKey.take(4)}****")

            val response = apiService.getHealthNews(apiKey = apiKey)

            if (response.status == "ok") {
                response.articles
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}