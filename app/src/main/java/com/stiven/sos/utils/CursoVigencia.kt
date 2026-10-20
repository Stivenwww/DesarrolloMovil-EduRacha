package com.stiven.sos.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Lógica canónica de vigencia. Espejo del backend (UTC epoch-millis). */
object CursoVigencia {
    const val MILLIS_POR_DIA = 86_400_000L
    const val MIN_DIAS = 1
    const val MAX_DIAS = 365

    fun validar(dias: Int): String? = when {
        dias < MIN_DIAS -> "La duración mínima es 1 día"
        dias > MAX_DIAS -> "La duración máxima es 365 días"
        else -> null
    }

    fun calcularFechaFin(fechaInicio: Long, dias: Int): Long {
        require(validar(dias) == null) { validar(dias) ?: "Duración inválida" }
        require(fechaInicio > 0) { "fechaInicio inválido" }
        return fechaInicio + TimeUnit.DAYS.toMillis(dias.toLong())
    }

    fun formatearFecha(millis: Long): String = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(millis))
    } catch (_: Exception) { "—" }

    fun estaVigente(inicio: Long, fin: Long, ahora: Long = System.currentTimeMillis()): Boolean =
        ahora in inicio..fin
}
