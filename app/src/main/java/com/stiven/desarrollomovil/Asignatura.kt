package com.stiven.desarrollomovil

data class Asignatura(
    val id: String = System.currentTimeMillis().toString(),
    val nombre: String,
    val codigo: String,
    val semestre: Int,
    val modalidad: String,
    val planAula: String,
    val imagenUrl: String = ""
)