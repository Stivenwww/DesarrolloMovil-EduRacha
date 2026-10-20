package ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stiven.sos.ui.theme.EduRachaColors

/**
 * Sistema de diseño canónico EduRacha (Fase 5).
 *
 * Decisión: EduRachaColors (índigo #5B4CF0 + sol #FFB627 + llama #FF6A1F) es la
 * paleta canónica. La paleta V2 (#3C79F5) queda como alias de acento para
 * no romper pantallas existentes; las nuevas pantallas deben usar [EduRachaDS].
 *
 * Estudiante: colores vivos + gamificación moderada.
 * Admin: [AdminDS] sobrio, sin gamificación.
 */
object EduRachaDS {
    val Primary: Color get() = EduRachaColors.Primary
    val PrimaryDark: Color get() = EduRachaColors.PrimaryVariant
    val Secondary: Color get() = EduRachaColors.Secondary
    val Accent: Color get() = EduRachaColors.Accent
    val Success: Color get() = EduRachaColors.Success
    val Warning: Color get() = EduRachaColors.Warning
    val Error: Color get() = EduRachaColors.Error
    val Background: Color get() = EduRachaColors.Background
    val Surface: Color get() = EduRachaColors.Surface
    val TextPrimary: Color get() = EduRachaColors.TextPrimary
    val TextSecondary: Color get() = EduRachaColors.TextSecondary

    // Alias de compatibilidad V2 (no usar en código nuevo)
    val V2Primary = Color(0xFF2F6BFF)
    val V2Success = Color(0xFF1FC77E)
    val V2Accent = Color(0xFFFFC531)

    val RadiusS = 8.dp
    val RadiusM = 14.dp
    val RadiusL = 20.dp
    val ButtonH = 52.dp
    val TouchMin = 48.dp

    val SpaceXS = 4.dp
    val SpaceS = 8.dp
    val SpaceM = 16.dp
    val SpaceL = 24.dp
    val SpaceXL = 32.dp

    @Composable
    fun primaryButtonColors() = ButtonDefaults.buttonColors(containerColor = Primary)
    @Composable
    fun cardColors() = CardDefaults.cardColors(containerColor = Surface)
    val CardShape = RoundedCornerShape(RadiusM)
}

/**
 * Identidad del docente: misma marca que el estudiante (azul eléctrico + acentos),
 * pero con menos movimiento y sin mascota. Colorida y profesional.
 */
object AdminDS {
    private val p: com.stiven.sos.ui.theme.NexoPaleta get() = com.stiven.sos.ui.theme.TemaApp.paleta
    val Background: Color get() = p.fondo
    val Surface: Color get() = p.superficie
    val Border: Color get() = p.borde
    val TextPrimary: Color get() = p.texto
    val TextSecondary: Color get() = p.textoSuave
    val Brand: Color get() = p.marca
    val BrandDark: Color get() = p.marcaSombra
    val BrandSoft: Color get() = p.marca.copy(alpha = 0.14f)
    val Success: Color get() = p.accionTexto
    val Warning: Color get() = p.rachaTexto
    val Error: Color get() = p.errorTexto

    // Acentos por módulo
    val AcentoAzul: Color get() = p.marca
    val AcentoVerde: Color get() = p.accion
    val AcentoAmarillo: Color get() = p.racha
    val AcentoNaranja: Color get() = Color(0xFFFF7A2F)
    val AcentoTurquesa: Color get() = p.marcaSombra
    val AcentoMorado: Color get() = p.ia
    val AcentoRosa: Color get() = p.error

    val GradienteHeader: androidx.compose.ui.graphics.Brush
        get() = androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.fondo, p.superficie, p.superficieAlt))

    val Radius = 18.dp
    val RowH = 52.dp
}
