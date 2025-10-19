package com.stiven.sos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stiven.sos.databinding.ItemEstudianteCheckBinding

class EstudiantesCheckAdapter(
    private val estudiantes: List<Estudiante>,
    private val estudiantesYaAsignados: List<Estudiante>,
    private val onCheckChanged: (Estudiante, Boolean) -> Unit
) : RecyclerView.Adapter<EstudiantesCheckAdapter.ViewHolder>() {

    private val estudiantesChecked = mutableSetOf<Int>()

    init {
        // Marcar como checkeados los estudiantes ya asignados
        estudiantesYaAsignados.forEach { estudianteAsignado ->
            estudiantes.forEachIndexed { index, estudiante ->
                if (estudiante.id == estudianteAsignado.id) {
                    estudiantesChecked.add(index)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEstudianteCheckBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(estudiantes[position], position)
    }

    override fun getItemCount(): Int = estudiantes.size

    inner class ViewHolder(private val binding: ItemEstudianteCheckBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(estudiante: Estudiante, position: Int) {
            binding.txtNombreEstudiante.text = "${estudiante.nombre} ${estudiante.apellido}"
            binding.txtEmailEstudiante.text = estudiante.email
            binding.checkbox.isChecked = estudiantesChecked.contains(position)

            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    estudiantesChecked.add(position)
                } else {
                    estudiantesChecked.remove(position)
                }
                onCheckChanged(estudiante, isChecked)
            }

            binding.root.setOnClickListener {
                binding.checkbox.isChecked = !binding.checkbox.isChecked
            }
        }
    }
}