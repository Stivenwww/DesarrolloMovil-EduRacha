package com.stiven.sos.api

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    private val TAG = "AuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // Si ya tiene Authorization header, no lo modificamos
        if (originalRequest.header("Authorization") != null) {
            Log.d(TAG, "Request ya tiene Authorization header")
            return chain.proceed(originalRequest)
        }

        // Obtener token de Firebase SIEMPRE ACTUALIZADO
        val firebaseToken = try {
            runBlocking {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.e(TAG, "Usuario no autenticado en Firebase")
                    return@runBlocking null
                }

                Log.d(TAG, "Obteniendo token actualizado de Firebase")

                // IMPORTANTE: Siempre forzar actualizacion del token
                val tokenResult = currentUser.getIdToken(true).await()

                if (tokenResult.token.isNullOrEmpty()) {
                    Log.e(TAG, "Token obtenido esta vacio")
                    null
                } else {
                    // Verificar que el token contenga los claims necesarios
                    Log.d(TAG, "Token Firebase obtenido correctamente")
                    tokenResult.token
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo token: ${e.message}", e)
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