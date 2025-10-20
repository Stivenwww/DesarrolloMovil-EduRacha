package com.stiven.sos.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // IMPORTANTE: Cambia esta URL según tu caso:
    // - Para emulador Android Studio: "http://10.0.2.2:8080/"
    // - Para dispositivo físico en misma red: "http://TU_IP_LOCAL:8080/" (ej: "http://192.168.1.5:8080/")
    // - Para servidor en la nube: "https://tu-dominio.com/"
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // Logger para ver las peticiones en Logcat
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true) // Reintentar en caso de fallo
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        instance.create(ApiService::class.java)
    }
}