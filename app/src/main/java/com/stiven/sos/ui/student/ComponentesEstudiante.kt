package com.stiven.sos.ui.student

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.EduRachaDS

/**
 * Fase 6 — componentes del estudiante.
 * Aprendizaje activo: cada tarjeta prioriza 1 concepto + ejemplo + micro-pregunta,
 * con explicación revelable. Sin dependencia de audio para la información.
 */

@Composable
fun ConceptoCard(
    titulo: String,
    explicacion: String,
    ejemplo: String? = null,
    preguntaRapida: String? = null,
    respuestaCorta: String? = null,
    modifier: Modifier = Modifier
) {
    var revelado by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = EduRachaDS.CardShape,
        colors = EduRachaDS.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lightbulb, contentDescription = "Concepto", tint = EduRachaDS.Secondary)
                Spacer(Modifier.width(8.dp))
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EduRachaDS.TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(explicacion, fontSize = 14.sp, color = EduRachaDS.TextPrimary, lineHeight = 20.sp)
            if (!ejemplo.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(color = EduRachaDS.Secondary.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                    Text("Ejemplo: $ejemplo", Modifier.padding(10.dp), fontSize = 13.sp)
                }
            }
            if (!preguntaRapida.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { revelado = !revelado },
                    modifier = Modifier.semantics { contentDescription = "Revelar explicación" }
                ) { Text(if (revelado) "Ocultar comprobación" else "Comprueba: $preguntaRapida") }
                if (revelado && !respuestaCorta.isNullOrBlank()) {
                    Text(respuestaCorta, fontSize = 13.sp, color = EduRachaDS.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ProgresoHeader(
    saludo: String,
    siguientePaso: String,
    progreso: Float,
    racha: Int,
    puntos: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth(), shape = EduRachaDS.CardShape, colors = CardDefaults.cardColors(containerColor = EduRachaDS.Primary)) {
        Column(Modifier.padding(16.dp)) {
            Text(saludo, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(siguientePaso, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progreso.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).semantics { contentDescription = "Progreso ${(progreso * 100).toInt()} por ciento" },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Racha $racha", color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp)
                Text("XP $puntos", color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun QuizOpcionCard(
    texto: String,
    seleccionada: Boolean,
    correcta: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = when (correcta) {
        true -> EduRachaDS.Success
        false -> EduRachaDS.Error
        null -> if (seleccionada) EduRachaDS.Primary else EduRachaDS.TextSecondary.copy(alpha = 0.3f)
    }
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
            .semantics { contentDescription = "Opción $texto" },
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, border),
        colors = CardDefaults.cardColors(containerColor = EduRachaDS.Surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (correcta == true) Icon(Icons.Filled.CheckCircle, null, tint = EduRachaDS.Success)
            if (correcta == false) Icon(Icons.Filled.CheckCircle, null, tint = EduRachaDS.Error)
            Text(texto, Modifier.padding(start = 8.dp), fontSize = 15.sp)
        }
    }
}

@Composable
fun LogroBanner(texto: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = EduRachaDS.Secondary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EmojiEvents, null, tint = EduRachaDS.Secondary)
            Spacer(Modifier.width(8.dp))
            Text(texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

/**
 * Sonidos opcionales, persistentes y nunca automáticos invasivos.
 * Usa tonos del sistema (sin assets) y respeta preferencia explícita.
 */
object SonidoAprendizaje {
    private const val PREFS = "EduRachaUserPrefs"
    private const val KEY = "sonido_habilitado"

    fun habilitado(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun guardar(ctx: Context, valor: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, valor).apply()
    }

    fun emitir(ctx: Context, tipo: Tipo = Tipo.ACIERTO) {
        if (!habilitado(ctx)) return
        try {
            val tono = when (tipo) {
                Tipo.ACIERTO -> ToneGenerator.TONE_PROP_ACK
                Tipo.ERROR -> ToneGenerator.TONE_PROP_NACK
                Tipo.LOGRO -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            }
            ToneGenerator(AudioManager.STREAM_MUSIC, 60).apply {
                startTone(tono, 150)
                release()
            }
        } catch (_: Exception) { }
    }

    enum class Tipo { ACIERTO, ERROR, LOGRO }
}
