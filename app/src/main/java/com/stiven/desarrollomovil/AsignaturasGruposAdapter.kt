package com.stiven.desarrollomovil

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stiven.desarrollomovil.databinding.ItemAsignaturaGrupoBinding

class AsignaturasGruposAdapter(
    private val asignaturas: List<String>,
    private val onAsignaturaClick: (String) -> Unit
) : RecyclerView.Adapter<AsignaturasGruposAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAsignaturaGrupoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(asignaturas[position])
    }

    override fun getItemCount(): Int = asignaturas.size

    inner class ViewHolder(private val binding: ItemAsignaturaGrupoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(asignatura: String) {
            binding.txtNombreAsignatura.text = asignatura

            // Obtener número de estudiantes asignados
            val numEstudiantes = GruposRepository.obtenerEstudiantesPorAsignatura(asignatura).size
            binding.txtNumEstudiantes.text = "$numEstudiantes estudiantes asignados"

            binding.root.setOnClickListener {
                onAsignaturaClick(asignatura)
            }
        }
    }
}