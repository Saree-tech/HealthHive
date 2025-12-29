package com.example.healthhive.data

import com.google.gson.annotations.SerializedName

// This represents the full object returned by the API
data class NewsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("articles") val articles: List<HealthArticle>
)

// This represents an individual health article
data class HealthArticle(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String,
    @SerializedName("urlToImage") val imageUrl: String?,
    @SerializedName("source") val source: ArticleSource
)

// This extracts the name of the news provider (e.g., "Mayo Clinic")
data class ArticleSource(
    @SerializedName("name") val name: String
)