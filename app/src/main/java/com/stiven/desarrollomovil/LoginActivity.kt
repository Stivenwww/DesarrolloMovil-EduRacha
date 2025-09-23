package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.stiven.desarrollomovil.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private var userType: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        userType = intent.getStringExtra("user_type") ?: "student"

        setupUI()
        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun setupUI() {
        if (userType == "teacher") {
            binding.tvTitle.text = "Inicio de Sesión - Docente"
            binding.btnLogin.text = "Ingresar como Docente"
            binding.btnGoToRegister.text = "Registrarse como Docente"
        } else {
            binding.tvTitle.text = "Inicio de Sesión - Estudiante"
            binding.btnLogin.text = "Ingresar como Estudiante"
            binding.btnGoToRegister.text = "Registrarse como Estudiante"
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        // Login con email y contraseña
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty()) {
                binding.etEmail.error = "Ingresa tu correo electrónico"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Ingresa tu contraseña"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    goToMain()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Ir a registro
        binding.btnGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("user_type", userType)
            startActivity(intent)
            finish()
        }

        // Login con Google
        binding.btnGoogle.setOnClickListener {
            signInGoogle()
        }
    }

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // Actualizar el displayName para incluir el tipo de usuario
                    val user = auth.currentUser
                    val currentDisplayName = user?.displayName ?: account.displayName ?: "Usuario"

                    // Solo actualizar si no contiene ya información del tipo de usuario
                    if (!currentDisplayName.contains("Docente") && !currentDisplayName.contains("Estudiante")) {
                        val newDisplayName = "$currentDisplayName - ${if (userType == "teacher") "Docente" else "Estudiante"}"

                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(newDisplayName)
                            .build()

                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener {
                                goToMain()
                            }
                    } else {
                        goToMain()
                    }
                } else {
                    Toast.makeText(this, "Error con Google: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInGoogle() {
        val signInIntent = googleClient.signInIntent
        googleLauncher.launch(signInIntent)
    }

    private fun goToMain() {
        val intent = if (userType == "teacher") {
            Intent(this, PanelDocenteActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        intent.putExtra("user_type", userType)
        startActivity(intent)
        finish()
    }
}