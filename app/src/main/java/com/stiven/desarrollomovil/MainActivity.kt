package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.stiven.desarrollomovil.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private var userType: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userType = intent.getStringExtra("user_type") ?: "student"

        loadUserData()
        setupClickListeners()
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            val displayName = user.displayName
            val email = user.email

            // Extraer información del displayName si está disponible
            var fullName = "Usuario"
            var username = ""
            var roleText = if (userType == "teacher") "Docente" else "Estudiante"

            if (!displayName.isNullOrEmpty()) {
                // Si el displayName contiene información completa (nombre (username) - rol)
                if (displayName.contains(" - ")) {
                    val parts = displayName.split(" - ")
                    val nameAndUsername = parts[0]
                    roleText = parts[1]

                    if (nameAndUsername.contains("(") && nameAndUsername.contains(")")) {
                        val nameParts = nameAndUsername.split("(")
                        fullName = nameParts[0].trim()
                        username = nameParts[1].replace(")", "").trim()
                    } else {
                        fullName = nameAndUsername
                    }
                } else {
                    fullName = displayName
                }
            }

            binding.tvWelcome.text = "¡Bienvenido, $fullName!"

            var userInfo = "Perfil: $roleText\nCorreo: $email"
            if (username.isNotEmpty()) {
                userInfo = "Perfil: $roleText\nUsuario: @$username\nCorreo: $email"
            }

            binding.tvUserInfo.text = userInfo
            title = "$roleText - $email"
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}