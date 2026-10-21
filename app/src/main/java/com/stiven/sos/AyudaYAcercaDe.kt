package com.stiven.sos

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stiven.sos.ui.theme.*

/** Correo de soporte de EduRacha. */
const val CORREO_SOPORTE = "brayner.trochez.o@uniautonoma.edu.co"

private data class PreguntaAyuda(val icono: ImageVector, val pregunta: String, val respuesta: String)

private val AYUDA_GENERAL = listOf(
    PreguntaAyuda(Icons.Outlined.Lightbulb, "¿Qué es EduRacha?",
        "Una app de aprendizaje gamificado: el estudiante avanza por los temas de su curso como si fueran islas, presenta quizzes, gana XP, cuida sus vidas y mantiene su racha de días seguidos. El docente organiza el curso y la inteligencia artificial le ayuda a crear explicaciones y preguntas, que él revisa antes de publicarlas."),
    PreguntaAyuda(Icons.Outlined.DarkMode, "¿Puedo usar el modo oscuro?",
        "Sí. En Configuración → Apariencia elige Sistema, Claro u Oscuro. Tu elección se guarda en tu perfil."),
    PreguntaAyuda(Icons.Outlined.Wifi, "¿Necesito internet?",
        "Sí, durante los quizzes y para ver tu progreso. Si se cae la conexión, vuelve a intentarlo cuando regrese.")
)

private val AYUDA_ESTUDIANTE = listOf(
    PreguntaAyuda(Icons.Outlined.PersonAdd, "¿Cómo entro a un curso?",
        "En Cursos disponibles escribe el código que te dio tu docente y envía la solicitud. Cuando el docente la acepte te llegará un aviso y el curso aparecerá en Mis cursos."),
    PreguntaAyuda(Icons.Outlined.AutoStories, "¿Cómo avanzo por los temas?",
        "Cada tema es una isla. La primera vez lees la explicación tarjeta por tarjeta; al terminarla se desbloquea el quiz. Si apruebas con 80% o más, el tema queda conquistado y se abre el siguiente."),
    PreguntaAyuda(Icons.Outlined.Quiz, "¿Qué modos de quiz hay?",
        "Oficial: cuenta para tu progreso y tu XP. Práctica: para repasar sin perder nada. Final: al terminar todos los temas, para completar el curso."),
    PreguntaAyuda(Icons.Outlined.Favorite, "¿Qué pasa si me quedo sin vidas?",
        "Cada respuesta incorrecta en un quiz oficial te quita una vida. Si te quedas sin vidas, se regeneran solas: una cada 30 minutos."),
    PreguntaAyuda(Icons.Outlined.LocalFireDepartment, "¿Cómo funciona la racha?",
        "Cuenta los días seguidos en los que presentas al menos un quiz. Si un día no estudias, la racha vuelve a empezar."),
    PreguntaAyuda(Icons.Outlined.EmojiEvents, "¿Cómo subo en el ranking?",
        "El ranking del curso ordena a los estudiantes por XP, por racha o por vidas. Ganas más XP si respondes rápido, si es la primera vez que apruebas el tema o si aciertas todo."),
    PreguntaAyuda(Icons.Outlined.ErrorOutline, "Me dice que no estoy inscrito",
        "Cierra sesión desde Configuración y vuelve a entrar con tu cuenta de estudiante. Si sigue igual, revisa en Mis solicitudes que el docente haya aceptado tu ingreso.")
)

private val AYUDA_DOCENTE = listOf(
    PreguntaAyuda(Icons.Outlined.AddCircleOutline, "¿Cómo creo un curso?",
        "Panel docente → Crear curso. Agrega los temas: puedes escribir la explicación, pedirla a la IA o subir un PDF para que la IA la escriba solo con el contenido del documento. Comparte el código del curso con tus estudiantes."),
    PreguntaAyuda(Icons.Outlined.RateReview, "¿Por qué debo aprobar las explicaciones?",
        "Nada llega a los estudiantes sin tu revisión. En Explicaciones puedes leerlas por secciones, editarlas, regenerarlas con IA, aprobarlas o rechazarlas. Las preguntas con IA se basan en la explicación aprobada."),
    PreguntaAyuda(Icons.Outlined.AutoAwesome, "¿Cómo genero preguntas con IA?",
        "En Crear preguntas elige curso, tema y \"Con IA\". Puedes pedir hasta 30; se generan en lotes de 10 con pausas cortas para no saturar la IA. Quedan pendientes de revisión."),
    PreguntaAyuda(Icons.Outlined.FactCheck, "¿Puedo aprobar todas las preguntas de una vez?",
        "Sí. En Validar preguntas elige el curso, el tema (o todos) y toca \"Aprobar todas\"."),
    PreguntaAyuda(Icons.Outlined.HourglassTop, "¿Dónde veo lo que tengo pendiente?",
        "Toca \"Pendientes\" en el panel: verás cuántas preguntas, explicaciones, temas sin explicación y aspirantes tienes por revisar, por curso, con acceso directo a cada uno."),
    PreguntaAyuda(Icons.Outlined.Groups, "¿Cómo acepto estudiantes?",
        "En Gestión de grupos entra al curso. Verás los aspirantes con su mensaje: acéptalos uno a uno o todos a la vez. Si rechazas, puedes elegir un motivo."),
    PreguntaAyuda(Icons.Outlined.Insights, "¿Cómo veo el desempeño del curso?",
        "En Ranking y estadísticas elige el curso. En Estadísticas verás la actividad, los puntajes, los temas que más cuestan y quién necesita apoyo. Toca un gráfico para verlo en grande."),
    PreguntaAyuda(Icons.Outlined.TableChart, "¿Cómo descargo los reportes?",
        "En Reportes elige el tipo (diario, por tema, general o por rango), mira la vista previa y descarga el Excel. Queda en Descargas y puedes abrirlo o compartirlo desde la app.")
)

/** Ayuda (cómo funciona la app) y soporte por correo. */
@Composable
fun PantallaAyudaSoporte(rol: String, onVolver: () -> Unit) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val esDocente = rol.equals("docente", true) || rol.equals("admin", true)
    var abierta by remember { mutableStateOf<String?>(null) }

    fun escribirASoporte() {
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0"
        val cuerpo = """
            Hola equipo de EduRacha,

            Describe aquí tu problema o sugerencia:


            —
            Rol: ${rol.ifBlank { "sin rol" }}
            Versión de la app: $version
            Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})
        """.trimIndent()
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(CORREO_SOPORTE))
            putExtra(Intent.EXTRA_SUBJECT, "Soporte EduRacha")
            putExtra(Intent.EXTRA_TEXT, cuerpo)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Escribir a soporte"))
        } catch (e: Exception) {
            Avisos.mostrar(context, "No hay una app de correo. Escríbenos a $CORREO_SOPORTE", TipoAviso.ADVERTENCIA)
        }
    }

    PantallaSuperpuestaNexo(onAtras = onVolver) {
        FondoNexo(conCuadricula = false) {
            Column(Modifier.fillMaxSize()) {
                EncabezadoNexo(titulo = "Ayuda y soporte", subtitulo = "Centro de ayuda", onVolver = onVolver, tono = TonoEncabezado.MARCA)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ContenidoCentradoNexo(Modifier.padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GloboRobi(
                                titulo = "HOLA",
                                mensaje = if (esDocente) "Aquí tienes una guía rápida de tus herramientas como docente. Si algo no funciona, escríbenos."
                                else "Aquí te explico cómo funciona EduRacha. Si tienes un problema, escríbenos y te ayudamos.",
                                expresion = ExpresionRobi.SALUDA,
                                tamanoRobi = 64.dp
                            )
                            TituloSeccionNexo(if (esDocente) "Para docentes" else "Para estudiantes", icono = if (esDocente) Icons.Outlined.School else Icons.Outlined.Backpack)
                            (if (esDocente) AYUDA_DOCENTE else AYUDA_ESTUDIANTE).forEach { q ->
                                FilaPreguntaAyuda(q, abierta == q.pregunta) { abierta = if (abierta == q.pregunta) null else q.pregunta }
                            }
                            TituloSeccionNexo("General", icono = Icons.Outlined.Info)
                            AYUDA_GENERAL.forEach { q ->
                                FilaPreguntaAyuda(q, abierta == q.pregunta) { abierta = if (abierta == q.pregunta) null else q.pregunta }
                            }

                            TituloSeccionNexo("Soporte", icono = Icons.Outlined.SupportAgent)
                            TarjetaNexo(Modifier.fillMaxWidth(), acento = p.marca) {
                                Text("¿Necesitas ayuda personal?", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Escríbenos y cuéntanos qué pasó. El correo ya incluye la versión de la app y tu dispositivo para ayudarte más rápido.",
                                    color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(CORREO_SOPORTE, color = p.marcaTexto, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                BotonNexo("Enviar correo a soporte", { escribirASoporte() }, tipo = TipoBoton.MARCA,
                                    icono = Icons.Outlined.Email, modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaPreguntaAyuda(q: PreguntaAyuda, abierta: Boolean, onClick: () -> Unit) {
    val p = LocalNexo.current
    TarjetaNexo(Modifier.fillMaxWidth().clickable(onClick = onClick), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaNexo(q.icono, p.marca, tamano = 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(q.pregunta, color = p.texto, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, if (abierta) "Ocultar" else "Ver respuesta", tint = p.textoSuave)
        }
        AnimatedVisibility(abierta) {
            Text(q.respuesta, color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 21.sp,
                modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** Acerca de EduRacha: qué es, cómo funciona, creadores e institución. */
@Composable
fun PantallaAcercaDe(onVolver: () -> Unit) {
    val p = LocalNexo.current
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0" }
    PantallaSuperpuestaNexo(onAtras = onVolver) {
        FondoNexo {
            Column(Modifier.fillMaxSize()) {
                EncabezadoNexo(titulo = "Acerca de EduRacha", subtitulo = "Versión $version", onVolver = onVolver, tono = TonoEncabezado.IA)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ContenidoCentradoNexo(Modifier.padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            RobiNeo(tamano = 120.dp, expresion = ExpresionRobi.FELIZ)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("edu", fontSize = 34.sp, fontWeight = FontWeight.Black, color = p.texto)
                                Text("racha", fontSize = 34.sp, fontWeight = FontWeight.Black, color = p.marcaTexto)
                            }
                            Text(
                                "Estudiar se vuelve un hábito y enseñar se vuelve más fácil",
                                color = p.textoSuave, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center
                            )

                            TarjetaNexo(Modifier.fillMaxWidth()) {
                                Text("¿Qué es?", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.semantics { heading() })
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "EduRacha es una plataforma de aprendizaje gamificado con inteligencia artificial. Convierte el estudio en un juego: " +
                                        "el estudiante avanza por los temas de su curso, presenta quizzes, gana XP, cuida sus vidas, mantiene su racha de días " +
                                        "seguidos y compite en el ranking con sus compañeros.",
                                    color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 21.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Para el docente es un aliado que le ahorra trabajo: la IA le propone explicaciones y preguntas para cada tema, " +
                                        "él las revisa y las aprueba antes de que lleguen a los estudiantes, y sigue el avance de su curso con estadísticas " +
                                        "y reportes en Excel.",
                                    color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 21.sp
                                )
                            }

                            TarjetaNexo(Modifier.fillMaxWidth()) {
                                Text("Cómo funciona", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.semantics { heading() })
                                Spacer(Modifier.height(8.dp))
                                listOf(
                                    Icons.Outlined.School to "El docente crea su curso, lo organiza por temas y define las fechas de los quizzes.",
                                    Icons.Outlined.AutoAwesome to "La IA propone explicaciones y preguntas; nada se publica sin la aprobación del docente.",
                                    Icons.Outlined.Quiz to "El estudiante lee cada explicación y presenta quizzes en modo oficial, práctica y final.",
                                    Icons.Outlined.Bolt to "Cada resultado suma XP, actualiza vidas y racha, y mueve el ranking del curso al instante.",
                                    Icons.Outlined.Insights to "El docente consulta estadísticas, ve quién necesita apoyo y descarga reportes en Excel."
                                ).forEachIndexed { i, (icono, texto) ->
                                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                                        InsigniaNexo(icono, if (i % 2 == 0) p.marca else p.ia, tamano = 32.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Text(texto, color = p.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            TarjetaNexo(Modifier.fillMaxWidth(), acento = p.ia) {
                                Text("Creado por", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.semantics { heading() })
                                Spacer(Modifier.height(10.dp))
                                listOf("Laura Valentina Solarte Muñoz", "Brayner Stiven Trochez Ordóñez").forEach { n ->
                                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AvatarNexo(n, p.ia, tamano = 44.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(n, color = p.texto, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                            Text("Ingeniería de Software y Computación", color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                NotaNexo(
                                    "Programa de Ingeniería de Software y Computación\nCorporación Universitaria Autónoma del Cauca · Popayán, Colombia · 2026",
                                    p.marca, Icons.Outlined.AccountBalance
                                )
                            }

                            TarjetaNexo(Modifier.fillMaxWidth()) {
                                Text("Tecnología", color = p.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "App nativa para Android (Kotlin y Jetpack Compose), servidor propio en Ktor, Firebase para datos y avisos en tiempo real, " +
                                        "e inteligencia artificial con las APIs de Groq y Anthropic. Arquitectura por capas para crecer sin dañar lo que ya funciona.",
                                    color = p.textoSuave, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 21.sp
                                )
                            }
                            Text("¿Dudas o sugerencias? $CORREO_SOPORTE", color = p.textoTenue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
