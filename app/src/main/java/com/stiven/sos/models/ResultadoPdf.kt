package com.stiven.sos.models

/** Respuesta de POST api/pdf/extraer: texto real extraído del PDF por el servidor. */
data class ResultadoPdf(
    val nombre: String = "",
    val paginas: Int = 0,
    val caracteres: Int = 0,
    val texto: String = "",
    val escaneado: Boolean = false,
    val truncado: Boolean = false,
    val error: String? = null
)
