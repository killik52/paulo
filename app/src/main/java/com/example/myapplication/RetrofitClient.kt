package com.example.myapplication.api

// A importação para BuildConfig DEVE corresponder ao 'namespace' definido em app/build.gradle.kts
import com.example.myapplication.BuildConfig

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://www.receitaws.com.br/v1/"

    private val okHttpClient = OkHttpClient.Builder().apply {
        // Adiciona o interceptor de logging apenas em builds de debug
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            addInterceptor(loggingInterceptor)
        }
    }.build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val cnpjApiService: CnpjApiService by lazy {
        retrofit.create(CnpjApiService::class.java)
    }
}