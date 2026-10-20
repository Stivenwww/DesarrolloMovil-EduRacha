package com.stiven.sos.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // =====================================================================
    // URL DEL BACKEND: deja UNA sola línea activa y comenta las demás.
    // =====================================================================

    // 1) LOCAL + EMULADOR (ACTIVA AHORA): backend corriendo en tu PC (puerto 8080).
    //    10.0.2.2 es la forma en que el emulador de Android Studio llega al "localhost" de tu PC.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // 2) LOCAL + CELULAR FÍSICO: celular y PC en la misma red Wi-Fi.
    //    Cambia la IP por la de tu PC (en Windows: ipconfig -> "Dirección IPv4")
    //    y agrega esa misma IP en res/xml/network_security_config.xml.
    // private const val BASE_URL = "http://192.168.1.100:8080/"

    // 3) DESPLEGADO (Sevalla): úsala cuando el backend esté publicado.
    // private const val BASE_URL = "https://backend-eduracha-kotlin-35lqe.sevalla.app/"

    // En depuración se ven las peticiones en Logcat (filtro "okhttp"); en release no se registra nada.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (com.stiven.sos.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
    }

    private val authInterceptor = AuthInterceptor()


    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
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
