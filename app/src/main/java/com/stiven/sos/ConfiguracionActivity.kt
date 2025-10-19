package com.stiven.sos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.stiven.sos.databinding.ActivityConfiguracionBinding

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        // Editar perfil
        binding.btnEditarPerfil.setOnClickListener {
            Toast.makeText(this, "Editar perfil - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Cambiar contraseña
        binding.btnCambiarPassword.setOnClickListener {
            mostrarDialogoCambiarPassword()
        }

        // Notificaciones switch
        binding.switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                this,
                if (isChecked) "Notificaciones activadas" else "Notificaciones desactivadas",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Sonido switch
        binding.switchSonido.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                this,
                if (isChecked) "Sonido activado" else "Sonido desactivado",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Ayuda
        binding.btnAyuda.setOnClickListener {
            Toast.makeText(this, "Ayuda y soporte - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }
    }

    private fun mostrarDialogoCambiarPassword() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cambiar Contraseña")
            .setMessage("Se enviará un correo de recuperación a tu dirección de email registrada.")
            .setPositiveButton("Enviar") { dialog, _ ->
                enviarEmailRecuperacion()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarEmailRecuperacion() {
        val email = auth.currentUser?.email

        if (email.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "No se encontró un email asociado a tu cuenta",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Correo de recuperación enviado a $email",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al enviar correo: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarDialogoCerrarSesion() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Cerrar Sesión") { dialog, _ ->
                cerrarSesion()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        // Cerrar sesión de Firebase
        auth.signOut()

        // Redirigir a la pantalla de login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()
    }
}