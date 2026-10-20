package com.stiven.sos.api

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    private val TAG = "AuthInterceptor"
    @Volatile private var tokenCache: String? = null
    @Volatile private var tokenExpiraEn: Long = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // Si ya tiene Authorization header, no lo modificamos
        if (originalRequest.header("Authorization") != null) {
            Log.d(TAG, "Request ya tiene Authorization header")
            return chain.proceed(originalRequest)
        }

        // Obtener token de Firebase con caché (evita getIdToken(true) en cada request)
        val ahora = System.currentTimeMillis()
        val cacheValido = tokenCache != null && ahora < tokenExpiraEn
        val firebaseToken = if (cacheValido) {
            tokenCache
        } else try {
            runBlocking {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.e(TAG, "Usuario no autenticado en Firebase")
                    return@runBlocking null
                }

                // Solo forzar refresh si el caché expiró (50 min)
                val tokenResult = currentUser.getIdToken(false).await()

                if (tokenResult.token.isNullOrEmpty()) {
                    Log.e(TAG, "Token obtenido esta vacio")
                    null
                } else {
                    Log.d(TAG, "Token Firebase obtenido correctamente")
                    tokenCache = tokenResult.token
                    tokenExpiraEn = System.currentTimeMillis() + 50 * 60 * 1000L
                    tokenResult.token
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo token: ${e.message}", e)
            // Ante fallo, invalida caché para reintentar la próxima vez
            tokenCache = null
            tokenExpiraEn = 0L
            null
        }

        // Si no hay token, proceder sin el (fallara en el backend)
        if (firebaseToken == null) {
            Log.w(TAG, "No se pudo obtener token de Firebase")
            return chain.proceed(originalRequest)
        }

        // Agregar token a TODOS los requests
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $firebaseToken")
            .build()

        Log.d(TAG, "Request autenticada: ${newRequest.method} ${newRequest.url}")

        return chain.proceed(newRequest)
    }
}