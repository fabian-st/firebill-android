package com.firebill.android.data.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREFS_NAME = "firebill_prefs"
    private const val KEY_SERVER_URL = "server_url"
    const val DEFAULT_URL = "http://localhost:5000/"

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getService(context: Context): FirebillApiService {
        val url = getServerUrl(context)
        if (retrofit == null || currentBaseUrl != url) {
            currentBaseUrl = url
            retrofit = buildRetrofit(url)
        }
        return retrofit!!.create(FirebillApiService::class.java)
    }

    fun invalidate() {
        retrofit = null
        currentBaseUrl = null
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getServerUrl(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun saveServerUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_SERVER_URL, url).apply()
        invalidate()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
