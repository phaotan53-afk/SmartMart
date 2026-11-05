package com.example.smartmart.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ProductApiClient {
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8080/"

    /** baseUrl must end with '/', e.g. http://10.0.2.2:8080/ for an emulator. */
    fun create(baseUrl: String = EMULATOR_BASE_URL): ProductApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build(),
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ProductApiService::class.java)
}
