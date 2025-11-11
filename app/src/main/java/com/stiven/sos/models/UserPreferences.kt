package com.stiven.sos.models

import android.content.Context
import android.content.SharedPreferences

/**
 * Clase para guardar y recuperar datos del usuario usando SharedPreferences
 *  Usa commit() en lugar de apply() para garantizar escritura síncrona
 */
object UserPreferences {
    private const val PREF_NAME = "EduRachaUserPrefs"
    private const val KEY_USER_UID = "user_uid"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_NICKNAME = "user_nickname"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Guarda los datos del usuario después del registro o login
     *  USA commit() que es SÍNCRONO en lugar de apply() que es ASÍNCRONO
     */
    fun saveUserData(
        context: Context,
        uid: String,
        nombreCompleto: String,
        apodo: String,
        correo: String,
        rol: String
    ) {
        android.util.Log.d("UserPreferences", "=== GUARDANDO EN SHAREDPREFERENCES ===")
        android.util.Log.d("UserPreferences", "UID a guardar: $uid")
        android.util.Log.d("UserPreferences", "Nombre a guardar: $nombreCompleto")
        android.util.Log.d("UserPreferences", "Apodo a guardar: $apodo")
        android.util.Log.d("UserPreferences", "Correo a guardar: $correo")
        android.util.Log.d("UserPreferences", "Rol a guardar: $rol")

        val prefs = getPrefs(context)
        val success = prefs.edit().apply {
            putString(KEY_USER_UID, uid)
            putString(KEY_USER_NAME, nombreCompleto)
            putString(KEY_USER_NICKNAME, apodo)
            putString(KEY_USER_EMAIL, correo)
            putString(KEY_USER_ROLE, rol)
            putBoolean(KEY_IS_LOGGED_IN, true)
        }.commit() // commit() espera a que se complete el guardado (SÍNCRONO)

        android.util.Log.d("UserPreferences", "Guardado exitoso: $success")

        // Verificar INMEDIATAMENTE con una nueva instancia
        val verificar = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        android.util.Log.d("UserPreferences", "=== VERIFICACIÓN INMEDIATA ===")
        android.util.Log.d("UserPreferences", "Nombre guardado: ${verificar.getString(KEY_USER_NAME, "NO_ENCONTRADO")}")
        android.util.Log.d("UserPreferences", "Rol guardado: ${verificar.getString(KEY_USER_ROLE, "NO_ENCONTRADO")}")
        android.util.Log.d("UserPreferences", "Email guardado: ${verificar.getString(KEY_USER_EMAIL, "NO_ENCONTRADO")}")
        android.util.Log.d("UserPreferences", "UID guardado: ${verificar.getString(KEY_USER_UID, "NO_ENCONTRADO")}")
    }

    /**
     * Obtiene el UID del usuario
     */
    fun getUserUid(context: Context): String? {
        val uid = getPrefs(context).getString(KEY_USER_UID, null)
        android.util.Log.d("UserPreferences", "Recuperando UID: $uid")
        return uid
    }

    /**
     * Obtiene el nombre completo del usuario
     */
    fun getUserName(context: Context): String {
        val nombre = getPrefs(context).getString(KEY_USER_NAME, null)
        android.util.Log.d("UserPreferences", "Recuperando nombre completo: $nombre (clave: $KEY_USER_NAME)")
        return nombre ?: "Usuario"
    }

    /**
     * Obtiene el apodo del usuario
     */
    fun getUserNickname(context: Context): String {
        val apodo = getPrefs(context).getString(KEY_USER_NICKNAME, "")
        android.util.Log.d("UserPreferences", "Recuperando apodo: $apodo")
        return apodo ?: ""
    }

    /**
     * Obtiene el email del usuario
     */
    fun getUserEmail(context: Context): String {
        val email = getPrefs(context).getString(KEY_USER_EMAIL, "")
        android.util.Log.d("UserPreferences", "Recuperando email: $email")
        return email ?: ""
    }

    /**
     * Obtiene el rol del usuario (estudiante o docente)
     */
    fun getUserRole(context: Context): String {
        val rol = getPrefs(context).getString(KEY_USER_ROLE, null)
        android.util.Log.d("UserPreferences", "Recuperando rol: $rol (clave: $KEY_USER_ROLE)")
        return rol ?: "estudiante"
    }

    /**
     * Verifica si el usuario está logueado
     */
    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Limpia todos los datos del usuario (para logout)
     *  También usa commit() para asegurar limpieza inmediata
     */
    fun clearUserData(context: Context) {
        android.util.Log.d("UserPreferences", "=== LIMPIANDO TODOS LOS DATOS ===")
        val success = getPrefs(context).edit().clear().commit()
        android.util.Log.d("UserPreferences", "Limpieza exitosa: $success")
    }

    /**
     * Obtiene el nombre para mostrar (primer nombre)
     */
    fun getDisplayName(context: Context): String {
        val fullName = getUserName(context)
        val displayName = fullName.split(" ").firstOrNull() ?: "Usuario"
        android.util.Log.d("UserPreferences", "Display name generado: $displayName de $fullName")
        return displayName
    }
}