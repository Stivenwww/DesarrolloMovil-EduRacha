package com.stiven.desarrollomovil

data class Estudiante(
    val id: String,
    val nombre: String,
    val apellido: String,
    val email: String,
    val fotoPerfil: String? = null,

    // Datos de racha
    var rachaActual: Int = 0,
    var rachaMejor: Int = 0,
    var ultimaActividad: Long = System.currentTimeMillis(),

    // Datos de rendimiento
    var puntosTotal: Int = 0,
    var preguntasRespondidas: Int = 0,
    var preguntasCorrectas: Int = 0,
    var asignaturasInscritas: MutableList<String> = mutableListOf(),

    // Ranking
    var posicionRanking: Int = 0
) {

    // Calcula el porcentaje de aciertos
    fun getPorcentajeAciertos(): Double {
        return if (preguntasRespondidas > 0) {
            (preguntasCorrectas.toDouble() / preguntasRespondidas.toDouble()) * 100
        } else {
            0.0
        }
    }

    // Obtiene el nivel basado en puntos
    fun getNivel(): Int {
        return (puntosTotal / 100) + 1
    }

    // Calcula puntos para siguiente nivel
    fun getPuntosParaSiguienteNivel(): Int {
        val nivelActual = getNivel()
        val puntosNecesarios = nivelActual * 100
        return puntosNecesarios - (puntosTotal % 100)
    }

    // Verifica si la racha está activa (última actividad dentro de 24 horas)
    fun isRachaActiva(): Boolean {
        val ahora = System.currentTimeMillis()
        val diferencia = ahora - ultimaActividad
        val unDiaEnMillis = 24 * 60 * 60 * 1000
        return diferencia < unDiaEnMillis
    }

    companion object {
        // Datos de ejemplo para pruebas
        fun obtenerEstudiantesEjemplo(): MutableList<Estudiante> {
            return mutableListOf(
                Estudiante(
                    id = "1",
                    nombre = "Ana",
                    apellido = "García",
                    email = "ana.garcia@ejemplo.com",
                    rachaActual = 15,
                    rachaMejor = 20,
                    puntosTotal = 450,
                    preguntasRespondidas = 120,
                    preguntasCorrectas = 98,
                    asignaturasInscritas = mutableListOf("Matemáticas I", "Física"),
                    posicionRanking = 1
                ),
                Estudiante(
                    id = "2",
                    nombre = "Carlos",
                    apellido = "Rodríguez",
                    email = "carlos.rodriguez@ejemplo.com",
                    rachaActual = 12,
                    rachaMejor = 15,
                    puntosTotal = 380,
                    preguntasRespondidas = 100,
                    preguntasCorrectas = 75,
                    asignaturasInscritas = mutableListOf("Matemáticas I"),
                    posicionRanking = 2
                ),
                Estudiante(
                    id = "3",
                    nombre = "María",
                    apellido = "López",
                    email = "maria.lopez@ejemplo.com",
                    rachaActual = 8,
                    rachaMejor = 12,
                    puntosTotal = 320,
                    preguntasRespondidas = 85,
                    preguntasCorrectas = 70,
                    asignaturasInscritas = mutableListOf("Física", "Química"),
                    posicionRanking = 3
                ),
                Estudiante(
                    id = "4",
                    nombre = "Juan",
                    apellido = "Martínez",
                    email = "juan.martinez@ejemplo.com",
                    rachaActual = 5,
                    rachaMejor = 10,
                    puntosTotal = 280,
                    preguntasRespondidas = 75,
                    preguntasCorrectas = 55,
                    asignaturasInscritas = mutableListOf("Matemáticas I", "Química"),
                    posicionRanking = 4
                ),
                Estudiante(
                    id = "5",
                    nombre = "Laura",
                    apellido = "Fernández",
                    email = "laura.fernandez@ejemplo.com",
                    rachaActual = 3,
                    rachaMejor = 8,
                    puntosTotal = 220,
                    preguntasRespondidas = 60,
                    preguntasCorrectas = 45,
                    asignaturasInscritas = mutableListOf("Física"),
                    posicionRanking = 5
                )
            )
        }
    }
}