package com.stiven.sos.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Modelo que representa la respuesta del backend para el ranking
 * DEBE coincidir exactamente con RankingItem del backend
 */
@Serializable
data class RankingEstudianteAPI(
    @SerialName("estudianteId")
    val estudianteId: String,

    @SerialName("nombre")
    val nombre: String? = null,

    @SerialName("experiencia")
    val experiencia: Int = 0,

    @SerialName("rachaDias")
    val rachaDias: Int = 0,

    @SerialName("vidas")
    val vidas: Int = 0,

    @SerialName("cursoId")
    val cursoId: String? = null
)

/**
 * Modelo para la UI
 */
data class RankingEstudiante(
    val id: String,
    val nombre: String,
    val experiencia: Int,
    val diasConsecutivos: Int,
    val vidas: Int = 0,
    val cursoId: String? = null
)

/**
 * Función de conversión mejorada
 */
fun RankingEstudianteAPI.toUIModel() = RankingEstudiante(
    id = estudianteId,
    nombre = nombre ?: "Estudiante",
    experiencia = experiencia,
    diasConsecutivos = rachaDias,
    vidas = vidas,
    cursoId = cursoId
)