// File: com/example/healthhive/data/model/HealthArticle.kt
package com.example.healthhive.data.model

import com.google.gson.annotations.SerializedName

/**
 * Combined data file containing all news-related models.
 * Keeping these in the 'model' package prevents type mismatch errors in the UI.
 */

data class NewsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("articles") val articles: List<HealthArticle>
)

data class HealthArticle(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String,
    @SerializedName("urlToImage") val imageUrl: String?, // Maps to the UI property
    @SerializedName("source") val source: ArticleSource
)

data class ArticleSource(
    @SerializedName("name") val name: String
)