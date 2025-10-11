package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
// CORRECCIÓN: Añadir importaciones para los iconos extendidos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.stiven.desarrollomovil.ui.theme.EduRachaColors
import com.stiven.desarrollomovil.ui.theme.EduRachaTheme

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            EduRachaTheme {
                val user = auth.currentUser
                val (firstName, username) = remember(user) {
                    extractUserInfo(user?.displayName)
                }

                StudentDashboardScreen(
                    firstName = firstName,
                    username = username,
                    onLogoutClick = {
                        auth.signOut()
                        val intent = Intent(this, WelcomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }

    private fun extractUserInfo(displayName: String?): Pair<String, String> {
        if (displayName.isNullOrEmpty()) {
            return "Estudiante" to "ESTUDIANTE"
        }
        val name = displayName.substringBefore("(")
        return name.trim() to name.replace(" ", "_").uppercase()
    }
}

@Composable
fun StudentDashboardScreen(
    firstName: String,
    username: String,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var showQuizIntroDialog by remember { mutableStateOf(false) }
    var showQuizDialog by remember { mutableStateOf(false) }
    var showQuizResultDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var quizScore by remember { mutableStateOf(0) }

    var showRankingDialog by remember { mutableStateOf(false) }
    var showRankingProfileDialog by remember { mutableStateOf(false) }
    var selectedRankingUser by remember { mutableStateOf<RankingUser?>(null) }

    val subjects = remember { getSampleSubjects() }

    Scaffold(
        containerColor = EduRachaColors.Background,
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Header(firstName = firstName, onSettingsClick = onSettingsClick)
                Spacer(Modifier.height(16.dp))
                StreakCard(streakDays = 10)
                Spacer(Modifier.height(20.dp))
                SearchField()
                Spacer(Modifier.height(20.dp))
                RankingCard(username = username, onClick = { showRankingDialog = true })
                Spacer(Modifier.height(24.dp))
                CoursesSection(subjects) { subject ->
                    selectedSubject = subject
                    showQuizIntroDialog = true
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    )

    if (showQuizIntroDialog && selectedSubject != null) {
        QuizIntroDialog(
            subjectName = selectedSubject!!.name,
            onDismiss = { showQuizIntroDialog = false },
            onStart = {
                showQuizIntroDialog = false
                showQuizDialog = true
            }
        )
    }

    if (showQuizDialog && selectedSubject != null) {
        QuizScreen(
            subject = selectedSubject!!,
            onQuizFinished = { score ->
                quizScore = score
                showQuizDialog = false
                showQuizResultDialog = true
            }
        )
    }

    if (showQuizResultDialog) {
        QuizResultDialog(
            score = quizScore,
            totalQuestions = getSampleQuestions().size,
            streakDays = 11, // Simulado
            onDismiss = { showQuizResultDialog = false }
        )
    }

    if (showRankingDialog) {
        RankingDialog(
            currentUserUsername = username,
            onDismiss = { showRankingDialog = false },
            onUserClick = { user ->
                selectedRankingUser = user
                showRankingProfileDialog = true
            }
        )
    }

    if (showRankingProfileDialog && selectedRankingUser != null) {
        StudentProfileDialog(user = selectedRankingUser!!) {
            showRankingProfileDialog = false
        }
    }
}

// --- Componentes del Dashboard Corregidos ---

@Composable
fun Header(firstName: String, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hola, $firstName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )
            Text(
                text = "¿Qué deseas aprender hoy?",
                style = MaterialTheme.typography.bodyMedium,
                color = EduRachaColors.TextSecondary
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configuración",
                tint = EduRachaColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StreakCard(streakDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EduRachaColors.Surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Racha",
                tint = EduRachaColors.StreakFire,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Racha actual", fontWeight = FontWeight.Bold, color = EduRachaColors.TextPrimary)
                Text("¡Sigue así!", style = MaterialTheme.typography.bodySmall, color = EduRachaColors.TextSecondary)
            }
            Text(
                text = streakDays.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.StreakFire
            )
            Text(
                text = "días",
                style = MaterialTheme.typography.bodyMedium,
                color = EduRachaColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchField() {
    TextField(
        value = "",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar por asignatura...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = EduRachaColors.Surface,
            unfocusedContainerColor = EduRachaColors.Surface,
            disabledContainerColor = EduRachaColors.Surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        )
    )
}

@Composable
fun RankingCard(username: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EduRachaColors.Primary),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents, // CORRECCIÓN
                contentDescription = "Ranking",
                tint = EduRachaColors.RankingGold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ranking de Estudiantes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "¡Compite y gana puntos!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Ver ranking", tint = Color.White) // CORRECCIÓN
        }
    }
}

@Composable
fun CoursesSection(subjects: List<Subject>, onSubjectClick: (Subject) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Cursos Inscritos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.TextPrimary
            )
            Text(
                text = "Ver todos",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = EduRachaColors.Primary,
                modifier = Modifier.clickable { /* TODO */ }
            )
        }
        Spacer(Modifier.height(16.dp))
        subjects.forEach { subject ->
            SubjectCard(subject = subject, onClick = { onSubjectClick(subject) })
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SubjectCard(subject: Subject, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EduRachaColors.Surface),
        border = BorderStroke(1.dp, EduRachaColors.SurfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(subject.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(subject.icon, contentDescription = null, tint = subject.color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.name, fontWeight = FontWeight.Bold, color = EduRachaColors.TextPrimary)
                Text("Prof: ${subject.teacher}", style = MaterialTheme.typography.bodySmall, color = EduRachaColors.TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Iniciar", tint = EduRachaColors.TextSecondary)
        }
    }
}

// --- Diálogos Corregidos ---

@Composable
fun QuizIntroDialog(subjectName: String, onDismiss: () -> Unit, onStart: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = EduRachaColors.Surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Quiz, null, tint = EduRachaColors.Primary, modifier = Modifier.size(60.dp)) // CORRECCIÓN
                Spacer(Modifier.height(16.dp))
                Text("Quiz de IA: $subjectName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Estás a punto de comenzar un cuestionario generado por IA para poner a prueba tus conocimientos. ¡Mucha suerte!", textAlign = TextAlign.Center, color = EduRachaColors.TextSecondary)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("¡Vamos a empezar!") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(subject: Subject, onQuizFinished: (Int) -> Unit) {
    val questions = remember { getSampleQuestions().shuffled() }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var correctAnswers by remember { mutableStateOf(0) }
    val progress by animateFloatAsState((currentQuestionIndex + 1) / questions.size.toFloat(), label = "quizProgress")

    Dialog(onDismissRequest = { /* No se cierra */ }) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Pregunta ${currentQuestionIndex + 1} de ${questions.size}", style = MaterialTheme.typography.bodySmall, color = EduRachaColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress, Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = EduRachaColors.Primary, trackColor = EduRachaColors.Primary.copy(alpha = 0.2f))
                Spacer(Modifier.height(24.dp))

                val question = questions[currentQuestionIndex]
                Text(question.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, minLines = 3)
                Spacer(Modifier.height(24.dp))
                question.options.forEachIndexed { index, option ->
                    OptionCard(option, selectedOption == index) { selectedOption = index }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (selectedOption == question.correctAnswerIndex) { correctAnswers++ }
                        if (currentQuestionIndex < questions.size - 1) {
                            currentQuestionIndex++
                            selectedOption = null
                        } else {
                            onQuizFinished(correctAnswers)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = selectedOption != null
                ) {
                    Text(if (currentQuestionIndex < questions.size - 1) "Siguiente" else "Finalizar")
                }
            }
        }
    }
}

@Composable
fun OptionCard(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, if (isSelected) EduRachaColors.Primary else EduRachaColors.SurfaceVariant),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) EduRachaColors.Primary.copy(alpha = 0.1f) else EduRachaColors.Surface)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Seleccionado", tint = EduRachaColors.Primary)
            }
        }
    }
}

@Composable
fun QuizResultDialog(score: Int, totalQuestions: Int, streakDays: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = EduRachaColors.Surface)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.EmojiEvents, null, tint = EduRachaColors.RankingGold, modifier = Modifier.size(80.dp)) // CORRECCIÓN
                Spacer(Modifier.height(16.dp))
                Text("¡Felicidades!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EduRachaColors.Primary)
                Spacer(Modifier.height(8.dp))
                Text("Obtuviste $score de $totalQuestions puntos", textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, "Racha", tint = EduRachaColors.StreakFire)
                    Spacer(Modifier.width(8.dp))
                    Text("¡Tu racha aumentó a $streakDays días!", fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Genial") }
            }
        }
    }
}

@Composable
fun RankingDialog(currentUserUsername: String, onDismiss: () -> Unit, onUserClick: (RankingUser) -> Unit) {
    val rankingUsers = remember { getSampleRanking(currentUserUsername) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ranking de la Clase", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(rankingUsers) { user ->
                        RankingItem(user = user, onUserClick = onUserClick)
                    }
                }
            }
        }
    }
}

@Composable
fun RankingItem(user: RankingUser, onUserClick: (RankingUser) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onUserClick(user) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${user.rank}.", fontWeight = FontWeight.Bold, fontSize = 16.sp,
            color = when (user.rank) {
                1 -> EduRachaColors.RankingGold
                2 -> EduRachaColors.RankingSilver
                3 -> EduRachaColors.RankingBronze
                else -> EduRachaColors.TextSecondary
            },
            modifier = Modifier.width(30.dp)
        )
        Icon(Icons.Default.Person, contentDescription = "Avatar", modifier = Modifier.size(40.dp).clip(CircleShape).border(2.dp, if (user.isCurrentUser) EduRachaColors.Primary else Color.Transparent, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(text = user.username, modifier = Modifier.weight(1f), fontWeight = if (user.isCurrentUser) FontWeight.Bold else FontWeight.Normal)
        Text("${user.points} pts", fontWeight = FontWeight.Bold, color = EduRachaColors.Primary)
    }
}

@Composable
fun StudentProfileDialog(user: RankingUser, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Person, "Avatar", modifier = Modifier.size(90.dp).clip(CircleShape).background(EduRachaColors.SurfaceVariant, CircleShape).padding(20.dp), tint = EduRachaColors.TextSecondary)
                Spacer(Modifier.height(16.dp))
                Text(user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("@${user.username.lowercase()}", style = MaterialTheme.typography.bodyMedium, color = EduRachaColors.TextSecondary)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceAround) {
                    ProfileStat(Icons.Default.Star, "${user.points}", "Puntos")
                    ProfileStat(Icons.Default.LocalFireDepartment, "${user.streakDays}", "Racha")
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
    }
}

@Composable
fun ProfileStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = EduRachaColors.Primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = EduRachaColors.TextSecondary)
    }
}

// --- Clases de datos y funciones de ejemplo (sin cambios) ---
data class Subject(val name: String, val teacher: String, val icon: ImageVector, val color: Color)
data class RankingUser(val rank: Int, val username: String, val points: Int, val streakDays: Int, val isCurrentUser: Boolean = false)
data class QuizQuestion(val text: String, val options: List<String>, val correctAnswerIndex: Int)

fun getSampleSubjects(): List<Subject> {
    return listOf(
        Subject("Teoría de la computación", "Prof. Zúñiga", Icons.Default.Computer, Color(0xFF1976D2)),
        Subject("Desarrollo Móvil", "Prof. Castillo", Icons.Default.PhoneAndroid, Color(0xFFF57C00)),
    )
}

fun getSampleRanking(currentUserUsername: String): List<RankingUser> {
    return listOf(
        RankingUser(1, "JUANP", 1250, 25),
        RankingUser(2, currentUserUsername, 1100, 10, isCurrentUser = true),
        RankingUser(3, "MARIA_G", 980, 18),
        RankingUser(4, "CARLOS_V", 850, 8),
        RankingUser(5, "SOFIA_R", 720, 12)
    )
}

fun getSampleQuestions(): List<QuizQuestion> {
    return listOf(
        QuizQuestion("¿Cuál es el propósito de un 'Intent' en Android?", listOf("Definir la UI", "Manejar eventos de click", "Iniciar una Activity o pasar datos", "Almacenar datos localmente"), 2),
        QuizQuestion("¿Qué layout organiza sus hijos en una única fila o columna?", listOf("ConstraintLayout", "FrameLayout", "TableLayout", "LinearLayout"), 3),
        QuizQuestion("¿Cuál es el archivo principal de manifiesto de una app de Android?", listOf("build.gradle", "AndroidManifest.xml", "styles.xml", "strings.xml"), 1)
    )
}
