package com.stiven.sos.api

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Si ya tiene Authorization header, no lo modificamos
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        // Obtener token de Firebase
        val token = try {
            runBlocking {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            }
        } catch (e: Exception) {
            null
        }

        // Si no hay token, proceder sin él
        if (token == null) {
            return chain.proceed(originalRequest)
        }

        // Añadir token al header
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}