package com.stiven.sos.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    // Logger para ver las peticiones en Logcat
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = AuthInterceptor()

    // Se configura el cliente OkHttp para usar ambos interceptores
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor) //  Se añade el interceptor de autenticación
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true) // Se mantiene la configuración de reintentos
        .build()

    // Se mantiene la inicialización 'lazy' para Retrofit, usando el nuevo okHttpClient
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Se usa el cliente OkHttp ya configurado con ambos interceptores
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Se mantiene la inicialización 'lazy' para ApiService
    val apiService: ApiService by lazy {
        instance.create(ApiService::class.java)
    }
}
