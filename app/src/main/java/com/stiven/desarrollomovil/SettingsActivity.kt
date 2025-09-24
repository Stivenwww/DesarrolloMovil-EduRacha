package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.stiven.desarrollomovil.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        loadUserData()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            val displayName = user.displayName
            val email = user.email

            // Extraer información del displayName
            var firstName = "Estudiante"
            var lastName = ""
            var username = ""

            if (!displayName.isNullOrEmpty()) {
                when {
                    displayName.contains(" - ") -> {
                        val parts = displayName.split(" - ")
                        val nameAndUsername = parts[0]

                        if (nameAndUsername.contains("(") && nameAndUsername.contains(")")) {
                            val nameParts = nameAndUsername.split("(")
                            val fullName = nameParts[0].trim()
                            val nameWords = fullName.split(" ")
                            firstName = nameWords[0]
                            if (nameWords.size > 1) {
                                lastName = nameWords.drop(1).joinToString(" ")
                            }
                            username = nameParts[1].replace(")", "").trim()
                        } else {
                            val nameWords = nameAndUsername.split(" ")
                            firstName = nameWords[0]
                            if (nameWords.size > 1) {
                                lastName = nameWords.drop(1).joinToString(" ")
                            }
                        }
                    }
                    else -> {
                        val nameWords = displayName.split(" ")
                        firstName = nameWords[0]
                        if (nameWords.size > 1) {
                            lastName = nameWords.drop(1).joinToString(" ")
                        }
                    }
                }
            }

            // Actualizar UI
            binding.tvUserName.text = "$firstName $lastName".trim()
            binding.tvUserEmail.text = email
            binding.tvUserExperience.text = "15800"
            binding.tvUserStreak.text = "10"

            if (username.isNotEmpty()) {
                binding.tvUserName.text = "$firstName $lastName ($username)".trim()
            }
        }
    }

    private fun setupClickListeners() {
        // Perfil
        binding.cardProfile.setOnClickListener {
            // Aquí puedes abrir una actividad de edición de perfil
            // val intent = Intent(this, EditProfileActivity::class.java)
            // startActivity(intent)
        }

        // Cursos
        binding.cardCourses.setOnClickListener {
            // Navegar a vista de todos los cursos
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Notificaciones
        binding.cardNotifications.setOnClickListener {
            // Configurar notificaciones
            // val intent = Intent(this, NotificationSettingsActivity::class.java)
            // startActivity(intent)
        }

        // Privacidad
        binding.cardPrivacy.setOnClickListener {
            // Configurar privacidad
            // val intent = Intent(this, PrivacySettingsActivity::class.java)
            // startActivity(intent)
        }

        // Ayuda
        binding.cardHelp.setOnClickListener {
            // Mostrar ayuda
            // val intent = Intent(this, HelpActivity::class.java)
            // startActivity(intent)
        }

        // Cerrar sesión
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}