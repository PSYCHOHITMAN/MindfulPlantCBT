package com.mindfulplant.cbt.data.remote

import com.mindfulplant.cbt.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds a single, shared Retrofit instance pointed at the API base URL
 * defined in app/build.gradle.kts (BuildConfig.API_BASE_URL), so the URL
 * only needs to change in one place.
 */
object RetrofitClient {

    val apiService: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            // Render's free tier can take up to a minute to wake up after being idle.
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
