package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.stiven.desarrollomovil.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private var userType: String = "student"
    private var selectedFilter: String = "theory"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userType = intent.getStringExtra("user_type") ?: "student"

        setupViews()
        loadUserData()
        setupClickListeners()
        setupSearchFunctionality()
        setupFilterButtons()
    }

    private fun setupViews() {
        // Configurar la racha inicial
        updateStreakDisplay(10) // Ejemplo: 10 días de racha

        // Seleccionar filtro por defecto (Teórico)
        selectFilter("theory")
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            val displayName = user.displayName
            val email = user.email

            // Extraer información del displayName
            var firstName = "Estudiante"
            var username = ""

            if (!displayName.isNullOrEmpty()) {
                when {
                    displayName.contains(" - ") -> {
                        val parts = displayName.split(" - ")
                        val nameAndUsername = parts[0]

                        if (nameAndUsername.contains("(") && nameAndUsername.contains(")")) {
                            val nameParts = nameAndUsername.split("(")
                            firstName = nameParts[0].trim().split(" ")[0] // Solo el primer nombre
                            username = nameParts[1].replace(")", "").trim()
                        } else {
                            firstName = nameAndUsername.split(" ")[0] // Solo el primer nombre
                        }
                    }
                    else -> {
                        firstName = displayName.split(" ")[0] // Solo el primer nombre
                    }
                }
            }

            // Actualizar UI
            binding.tvGreeting.text = "Hola, $firstName"

            // Actualizar el nombre en el ranking (simulado)
            if (username.isNotEmpty()) {
                binding.tvUserRanking.text = username.uppercase()
            } else {
                binding.tvUserRanking.text = firstName.uppercase()
            }

            // Configurar título de la actividad
            title = "EduRacha - Estudiante"
        }
    }

    private fun updateStreakDisplay(days: Int) {
        binding.tvStreak.text = days.toString()
    }

    private fun setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                // Aquí puedes implementar la lógica de búsqueda
                filterCourses(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterCourses(query: String) {
        // Implementar filtrado de cursos basado en la búsqueda
        // Por ahora solo un placeholder
        if (query.isNotEmpty()) {
            // Lógica de filtrado
        }
    }

    private fun setupFilterButtons() {
        binding.btnFilterAll.setOnClickListener { selectFilter("all") }
        binding.btnFilterTheory.setOnClickListener { selectFilter("theory") }
        binding.btnFilterPractical.setOnClickListener { selectFilter("practical") }
        binding.btnFilterTheoryPractical.setOnClickListener { selectFilter("theory_practical") }
    }

    private fun selectFilter(filterType: String) {
        selectedFilter = filterType

        // Resetear todos los botones
        resetFilterButtons()

        // Aplicar estilo al botón seleccionado
        when (filterType) {
            "all" -> {
                binding.btnFilterAll.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.knowledge_blue)
                binding.btnFilterAll.setTextColor(
                    ContextCompat.getColor(this, R.color.on_primary)
                )
            }
            "theory" -> {
                binding.btnFilterTheory.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.knowledge_blue)
                binding.btnFilterTheory.setTextColor(
                    ContextCompat.getColor(this, R.color.on_primary)
                )
            }
            "practical" -> {
                binding.btnFilterPractical.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.knowledge_blue)
                binding.btnFilterPractical.setTextColor(
                    ContextCompat.getColor(this, R.color.on_primary)
                )
            }
            "theory_practical" -> {
                binding.btnFilterTheoryPractical.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.knowledge_blue)
                binding.btnFilterTheoryPractical.setTextColor(
                    ContextCompat.getColor(this, R.color.on_primary)
                )
            }
        }

        // Aplicar filtro a los cursos
        applyCourseFilter(filterType)
    }

    private fun resetFilterButtons() {
        val buttons = listOf(
            binding.btnFilterAll,
            binding.btnFilterTheory,
            binding.btnFilterPractical,
            binding.btnFilterTheoryPractical
        )

        buttons.forEach { button ->
            button.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.surface_variant)
            button.setTextColor(
                ContextCompat.getColor(this, R.color.primaryText)
            )
        }
    }

    private fun applyCourseFilter(filterType: String) {
        // Implementar lógica de filtrado de cursos
        // Por ahora solo un placeholder para mostrar/ocultar cursos
        when (filterType) {
            "all" -> {
                // Mostrar todos los cursos
            }
            "theory" -> {
                // Mostrar solo cursos teóricos
            }
            "practical" -> {
                // Mostrar solo cursos prácticos
            }
            "theory_practical" -> {
                // Mostrar solo cursos teórico-prácticos
            }
        }
    }

    private fun setupClickListeners() {
        // Logout
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Configuración/Settings - NUEVA FUNCIONALIDAD
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Card de ranking - NUEVA FUNCIONALIDAD
        binding.cardRanking.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            startActivity(intent)
        }

        // Ver todos los cursos - REDIRIGE AL RANKING
        binding.btnViewAll.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            startActivity(intent)
        }

        // Click en la racha para ver detalles - FUNCIONALIDAD MEJORADA
        binding.cardStreak.setOnClickListener {
            // Mostrar información de la racha con animación
            showStreakDetails()
        }
    }

    private fun showStreakDetails() {
        // Crear un diálogo simple para mostrar detalles de la racha
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("🔥 Racha de Estudio")
        builder.setMessage("¡Excelente trabajo!\n\nHas mantenido tu racha durante ${binding.tvStreak.text} días consecutivos.\n\n¡Sigue así para seguir subiendo en el ranking!")
        builder.setPositiveButton("¡Continúa estudiando!") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setNeutralButton("Ver Ranking") { dialog, _ ->
            val intent = Intent(this, RankingActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }
        builder.show()
    }

    // Método para actualizar la racha (puede ser llamado desde otras partes de la app)
    fun updateStreak(newStreakDays: Int) {
        updateStreakDisplay(newStreakDays)

        // Aquí puedes agregar animaciones o efectos visuales
        // cuando la racha se actualiza
        if (newStreakDays > 0) {
            // Mostrar una pequeña animación o notificación
            showStreakIncrementNotification(newStreakDays)
        }
    }

    private fun showStreakIncrementNotification(days: Int) {
        // Crear una pequeña notificación cuando la racha aumenta
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("🔥 ¡Racha Aumentada!")
        builder.setMessage("¡Felicidades! Tu racha ahora es de $days días.")
        builder.setPositiveButton("¡Genial!") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // Método para simular completar una actividad y aumentar la racha
    private fun completeStudyActivity() {
        val currentStreak = binding.tvStreak.text.toString().toIntOrNull() ?: 0
        updateStreak(currentStreak + 1)
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos del usuario cuando se vuelve a la actividad
        loadUserData()
    }
}