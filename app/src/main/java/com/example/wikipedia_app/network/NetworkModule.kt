package com.example.wikipedia_app.network

import android.util.Log
import com.example.wikipedia_app.network.WikipediaApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val TAG = "RetrofitInstance"
    private const val TIMEOUT_SECONDS = 30L

    private var retrofit: Retrofit? = null
    private var apiService: WikipediaApiService? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "WikiVoyage/1.0 (Android; educational project)")
                    .build()
            )
        }
        .build()

    fun recreateRetrofit() {
        Log.d(TAG, "Recreating Retrofit instance with base URL: ${ApiConfig.WIKIPEDIA_BASE_URL}")
        retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.WIKIPEDIA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit?.create(WikipediaApiService::class.java)
        Log.d(TAG, "Retrofit instance recreated successfully")
    }

    val api: WikipediaApiService
        get() {
            if (apiService == null) {
                recreateRetrofit()
            }
            return apiService!!
        }
}