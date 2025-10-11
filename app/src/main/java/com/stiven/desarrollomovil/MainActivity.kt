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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
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
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Uniautonoma.Primary)) {
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
        val nameParts = displayName.split(" ")
        val firstName = nameParts.firstOrNull() ?: "Estudiante"
        val username = displayName.replace(" ", "_").uppercase()
        return firstName to username
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
        containerColor = Uniautonoma.Background,
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
            streakDays = 11,
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

@Composable
fun Header(firstName: String, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hola, $firstName",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Uniautonoma.TextPrimary
            )
            Text(
                text = "¿Qué deseas aprender el día de hoy?",
                fontSize = 14.sp,
                color = Uniautonoma.TextSecondary
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settingss),
                contentDescription = "Configuración",
                tint = Uniautonoma.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StreakCard(streakDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Uniautonoma.Surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_fire),
                contentDescription = "Racha",
                tint = Uniautonoma.Warning,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Racha actual", fontSize = 14.sp, color = Uniautonoma.TextSecondary)
                Text("¡Sigue así!", fontSize = 12.sp, color = Uniautonoma.TextSecondary.copy(alpha = 0.7f))
            }
            Text(
                text = streakDays.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Uniautonoma.Warning
            )
            Text(
                text = "días",
                fontSize = 14.sp,
                color = Uniautonoma.TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchField() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar por asignatura") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Uniautonoma.Primary,
            unfocusedBorderColor = Color.LightGray
        )
    )
}

@Composable
fun RankingCard(username: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Uniautonoma.Primary),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ranking de estudiantes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = username,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Uniautonoma.Secondary, shape = RoundedCornerShape(2.dp))
            )
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
                text = "Cursos inscritos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Uniautonoma.TextPrimary
            )
            Text(
                text = "VER TODOS >",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Uniautonoma.Primary,
                modifier = Modifier.clickable { /* TODO: Navigate to all courses */ }
            )
        }
        Spacer(Modifier.height(16.dp))
        // Aquí irían las tarjetas de los cursos.
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Uniautonoma.Surface),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(subject.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(subject.icon, contentDescription = null, tint = subject.color)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.name, fontWeight = FontWeight.Bold, color = Uniautonoma.TextPrimary)
                Text("Prof: ${subject.teacher}", fontSize = 12.sp, color = Uniautonoma.TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Iniciar", tint = Uniautonoma.TextSecondary)
        }
    }
}

@Composable
fun QuizIntroDialog(subjectName: String, onDismiss: () -> Unit, onStart: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Uniautonoma.Surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = Uniautonoma.Primary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Quiz de IA: $subjectName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Estás a punto de comenzar un cuestionario generado por IA para poner a prueba tus conocimientos. ¡Mucha suerte!",
                    textAlign = TextAlign.Center,
                    color = Uniautonoma.TextSecondary
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("¡Vamos a empezar!")
                }
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

    Dialog(onDismissRequest = { /* No se puede cerrar a mitad del quiz */ }) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Pregunta ${currentQuestionIndex + 1} de ${questions.size}", fontSize = 14.sp, color = Uniautonoma.TextSecondary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Uniautonoma.Primary,
                    trackColor = Uniautonoma.Primary.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(24.dp))

                val question = questions[currentQuestionIndex]
                Text(question.text, fontSize = 18.sp, fontWeight = FontWeight.Medium, minLines = 3)
                Spacer(Modifier.height(24.dp))
                question.options.forEachIndexed { index, option ->
                    OptionCard(
                        text = option,
                        isSelected = selectedOption == index,
                        onClick = { selectedOption = index }
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (selectedOption == question.correctAnswerIndex) {
                            correctAnswers++
                        }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, if (isSelected) Uniautonoma.Primary else Color.LightGray.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Uniautonoma.Primary.copy(alpha = 0.1f) else Uniautonoma.Surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = Uniautonoma.Primary
                )
            }
        }
    }
}
@Composable
fun QuizResultDialog(score: Int, totalQuestions: Int, streakDays: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Uniautonoma.Surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("¡Felicidades!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Uniautonoma.Primary)
                Spacer(Modifier.height(8.dp))
                Text("Obtuviste $score de $totalQuestions puntos", textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = "Racha", tint = Uniautonoma.Warning)
                    Spacer(Modifier.width(8.dp))
                    Text("¡Tu racha aumentó a $streakDays días!", fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Genial")
                }
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
                Text("Ranking de la Clase", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick(user) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${user.rank}.",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = when (user.rank) {
                1 -> Uniautonoma.Secondary
                2, 3 -> Uniautonoma.Secondary.copy(alpha = 0.7f)
                else -> Uniautonoma.TextSecondary
            },
            modifier = Modifier.width(30.dp)
        )
        Image(
            painter = painterResource(id = user.avatarResId),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, if (user.isCurrentUser) Uniautonoma.Primary else Color.Transparent, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = user.username,
            modifier = Modifier.weight(1f),
            fontWeight = if (user.isCurrentUser) FontWeight.Bold else FontWeight.Normal
        )
        Text("${user.points} pts", fontWeight = FontWeight.Bold, color = Uniautonoma.Primary)
    }
}

@Composable
fun StudentProfileDialog(user: RankingUser, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = user.avatarResId),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(90.dp).clip(CircleShape)
                )
                Spacer(Modifier.height(16.dp))
                Text(user.username, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("@${user.username.lowercase()}", fontSize = 14.sp, color = Uniautonoma.TextSecondary)
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProfileStat(icon = Icons.Default.Star, value = "${user.points}", label = "Puntos")
                    ProfileStat(icon = Icons.Default.LocalFireDepartment, value = "${user.streakDays}", label = "Racha")
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
fun ProfileStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = Uniautonoma.Primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = Uniautonoma.TextSecondary)
    }
}

data class Subject(val name: String, val teacher: String, val icon: ImageVector, val color: Color)
data class RankingUser(val rank: Int, val username: String, val points: Int, val avatarResId: Int, val streakDays: Int, val isCurrentUser: Boolean = false)
data class QuizQuestion(val text: String, val options: List<String>, val correctAnswerIndex: Int)

fun getSampleSubjects(): List<Subject> {
    return listOf(
        Subject("Teoría de la computación", "Prof. Zúñiga", Icons.Default.Computer, Color(0xFF1976D2)),
        Subject("Desarrollo Móvil", "Prof. Castillo", Icons.Default.PhoneAndroid, Color(0xFFF57C00)),
        // Añade más asignaturas aquí
    )
}

fun getSampleRanking(currentUserUsername: String): List<RankingUser> {
    return listOf(
        RankingUser(1, "JUANP", 1250, R.drawable.ic_person, 25),
        RankingUser(2, currentUserUsername, 1100, R.drawable.ic_person, 10, isCurrentUser = true),
        RankingUser(3, "MARIA_G", 980, R.drawable.ic_person, 18),
        RankingUser(4, "CARLOS_V", 850, R.drawable.ic_person, 8),
        RankingUser(5, "SOFIA_R", 720, R.drawable.ic_person, 12)
    )
}

fun getSampleQuestions(): List<QuizQuestion> {
    return listOf(
        QuizQuestion("¿Cuál es el propósito de un 'Intent' en Android?", listOf("Definir la UI", "Manejar eventos de click", "Iniciar una Activity o pasar datos", "Almacenar datos localmente"), 2),
        QuizQuestion("¿Qué layout organiza sus hijos en una única fila o columna?", listOf("ConstraintLayout", "FrameLayout", "TableLayout", "LinearLayout"), 3),
        QuizQuestion("¿Cuál es el archivo principal de manifiesto de una app de Android?", listOf("build.gradle", "AndroidManifest.xml", "styles.xml", "strings.xml"), 1)
    )
}
