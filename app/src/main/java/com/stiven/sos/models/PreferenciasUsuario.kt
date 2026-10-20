package com.stiven.sos.models

/** Preferencias visuales sincronizadas con el backend. tema: SISTEMA | CLARO | OSCURO */
data class PreferenciasUsuario(
    val tema: String = "SISTEMA",
    val sonidos: Boolean = true
)
