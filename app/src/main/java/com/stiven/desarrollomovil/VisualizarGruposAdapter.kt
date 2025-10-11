package com.stiven.desarrollomovil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stiven.desarrollomovil.databinding.ItemGrupoVisualizarBinding

class VisualizarGruposAdapter(
    private val grupos: Map<String, List<Estudiante>>
) : RecyclerView.Adapter<VisualizarGruposAdapter.ViewHolder>() {

    private val asignaturas = grupos.keys.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGrupoVisualizarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asignatura = asignaturas[position]
        val estudiantes = grupos[asignatura] ?: emptyList()
        holder.bind(asignatura, estudiantes)
    }

    override fun getItemCount(): Int = asignaturas.size

    class ViewHolder(private val binding: ItemGrupoVisualizarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(asignatura: String, estudiantes: List<Estudiante>) {
            binding.txtNombreAsignatura.text = asignatura
            binding.txtNumEstudiantes.text = "${estudiantes.size} estudiantes"

            // Mostrar/ocultar lista de estudiantes
            var isExpanded = false

            binding.btnExpandir.setOnClickListener {
                isExpanded = !isExpanded
                if (isExpanded) {
                    binding.layoutEstudiantes.visibility = View.VISIBLE
                    binding.btnExpandir.rotation = 180f
                } else {
                    binding.layoutEstudiantes.visibility = View.GONE
                    binding.btnExpandir.rotation = 0f
                }
            }

            // Limpiar el layout antes de agregar
            binding.layoutEstudiantes.removeAllViews()

            // Agregar cada estudiante
            estudiantes.forEach { estudiante ->
                val estudianteView = LayoutInflater.from(binding.root.context)
                    .inflate(R.layout.item_estudiante_simple, binding.layoutEstudiantes, false)

                val txtNombre = estudianteView.findViewById<android.widget.TextView>(R.id.txt_nombre_estudiante_simple)
                val txtEmail = estudianteView.findViewById<android.widget.TextView>(R.id.txt_email_estudiante_simple)

                txtNombre.text = "${estudiante.nombre} ${estudiante.apellido}"
                txtEmail.text = estudiante.email

                binding.layoutEstudiantes.addView(estudianteView)
            }
        }
    }
}