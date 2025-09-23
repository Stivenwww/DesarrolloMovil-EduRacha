package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.stiven.desarrollomovil.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private var userType: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userType = intent.getStringExtra("user_type") ?: "student"

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        if (userType == "teacher") {
            binding.tvTitle.text = "Registro de Docente"
            binding.btnRegister.text = "Registrarse como Docente"
        } else {
            binding.tvTitle.text = "Registro de Estudiante"
            binding.btnRegister.text = "Registrarse como Estudiante"
        }
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.btnGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("user_type", userType)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        val fullName = binding.etFullName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        // Validaciones
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Ingresa tu nombre completo"
            return
        }

        if (username.isEmpty()) {
            binding.etUsername.error = "Ingresa un nombre de usuario"
            return
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Ingresa tu correo electrónico"
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Ingresa una contraseña"
            return
        }

        if (password.length < 6) {
            binding.etPassword.error = "La contraseña debe tener mínimo 6 caracteres"
            return
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Las contraseñas no coinciden"
            return
        }

        // Crear usuario en Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Actualizar el perfil del usuario con el nombre completo
                    val user = auth.currentUser
                    val displayName = "$fullName ($username) - ${if (userType == "teacher") "Docente" else "Estudiante"}"

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                goToMain()
                            } else {
                                Toast.makeText(this, "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                                goToMain() // Continuar aunque falle la actualización del perfil
                            }
                        }
                } else {
                    Toast.makeText(this, "Error al registrar: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_type", userType)
        startActivity(intent)
        finish()
    }
}