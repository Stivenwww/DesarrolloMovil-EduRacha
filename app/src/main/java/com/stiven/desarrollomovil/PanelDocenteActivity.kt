package com.stiven.desarrollomovil

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stiven.desarrollomovil.databinding.ActivityPanelDocenteBinding

class PanelDocenteActivity : AppCompatActivity() {


    private lateinit var binding : ActivityPanelDocenteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_panel_docente)

        binding = ActivityPanelDocenteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajuste de insets para status bar y navigation
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //desde aqui ya se pueden referenciar los items con los ids y programarlos
        // 🔹 Ejemplos de uso de binding con tus vistas:
        binding.txtNombreDocente.text = "Hola, Profesor 👋"
        binding.txtBienvenida.text = "Buenas tardes"
        binding.txtNumNotificaciones.text = "7"

        // Eventos en las cards
        binding.cardCrearAsignatura.setOnClickListener {
            // TODO: abrir actividad o mostrar un toast
        }

        binding.cardValidacionIa.setOnClickListener {
            // TODO: acción para validación IA
        }

        binding.cardReportes.setOnClickListener {
            // TODO: abrir reportes
        }

        binding.cardAsignarGrupos.setOnClickListener {
            // TODO: gestión de grupos
        }


    }
}