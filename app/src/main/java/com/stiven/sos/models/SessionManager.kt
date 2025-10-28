package com.stiven.sos.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Administrador de sesión para guardar y obtener datos del usuario logueado
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        // Usar el mismo nombre de SharedPreferences que ya existe
        private const val PREFS_NAME = "EduRachaUserPrefs"
        // Usar las mismas keys que ya existen
        private const val KEY_USER_ID = "user_uid"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROL = "user_rol"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Guardar datos de sesión del usuario
     */
    fun saveUserSession(
        userId: String,
        userName: String,
        userEmail: String,
        userRol: String
    ) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_ROL, userRol)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Obtener ID del usuario logueado (docente o estudiante)
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Obtener nombre del usuario logueado
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Obtener email del usuario logueado
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Obtener rol del usuario (docente o estudiante)
     */
    fun getUserRol(): String? {
        return prefs.getString(KEY_USER_ROL, null)
    }

    /**
     * Verificar si hay una sesión activa
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Cerrar sesión y limpiar datos
     */
    fun logout() {
        prefs.edit().clear().apply()
    }

    /**
     * Verificar si el usuario es docente
     */
    fun isDocente(): Boolean {
        return getUserRol()?.equals("docente", ignoreCase = true) == true
    }

    /**
     * Verificar si el usuario es estudiante
     */
    fun isEstudiante(): Boolean {
        return getUserRol()?.equals("estudiante", ignoreCase = true) == true
    }
}