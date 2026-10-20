package com.stiven.sos.models

import org.junit.Assert.assertEquals
import org.junit.Test

/** Contrato compartido con GET /api/preguntas?estado= (backend FiltroPreguntas). */
class PreguntaEstadoTest {
    @Test fun `estados coinciden con backend`() {
        assertEquals("pendiente_revision", EstadoPregunta.PENDIENTE_REVISION)
        assertEquals("aprobada", EstadoPregunta.APROBADA)
        assertEquals("rechazada", EstadoPregunta.RECHAZADA)
    }
}
