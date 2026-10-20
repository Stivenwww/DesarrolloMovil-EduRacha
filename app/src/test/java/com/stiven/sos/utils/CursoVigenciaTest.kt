package com.stiven.sos.utils

import org.junit.Assert.*
import org.junit.Test

class CursoVigenciaTest {
    @Test fun `calculo 30 dias`() {
        val inicio = 1_700_000_000_000L
        assertEquals(inicio + 30 * 86_400_000L, CursoVigencia.calcularFechaFin(inicio, 30))
    }

    @Test fun `duraciones 1 2 7 90`() {
        val inicio = 1_700_000_000_000L
        listOf(1, 2, 7, 30, 90).forEach {
            assertEquals(inicio + it * 86_400_000L, CursoVigencia.calcularFechaFin(inicio, it))
        }
    }

    @Test fun `limite inclusivo`() {
        val i = 1_700_000_000_000L
        val f = CursoVigencia.calcularFechaFin(i, 7)
        assertTrue(CursoVigencia.estaVigente(i, f, i))
        assertTrue(CursoVigencia.estaVigente(i, f, f))
        assertFalse(CursoVigencia.estaVigente(i, f, f + 1))
    }

    @Test fun `rango invalido`() {
        assertNotNull(CursoVigencia.validar(0))
        assertNotNull(CursoVigencia.validar(366))
        assertNull(CursoVigencia.validar(30))
    }
}
