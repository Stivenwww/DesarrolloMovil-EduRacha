package com.stiven.desarrollomovil

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stiven.desarrollomovil.databinding.ActivityCrearAsignaturaBinding
import com.stiven.desarrollomovil.databinding.ActivityPanelDocenteBinding

class CrearAsignatura : AppCompatActivity() {


    private lateinit var binding : ActivityCrearAsignaturaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_asignatura)

        binding = ActivityCrearAsignaturaBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnBack.setOnClickListener {

            val intent = Intent(this, PanelDocenteActivity::class.java)
            startActivity(intent)
        }

        //Aqui ya se pueden referenciar los componentes mediante binding




    }
}