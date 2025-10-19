package com.stiven.sos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class EstudiantesAdapter(
    private var estudiantes: MutableList<Estudiante>,
    private val onEstudianteClick: (Estudiante) -> Unit
) : RecyclerView.Adapter<EstudiantesAdapter.EstudianteViewHolder>() {

    inner class EstudianteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val badgeRanking: CardView = itemView.findViewById(R.id.badge_ranking)
        val txtPosicion: TextView = itemView.findViewById(R.id.txt_posicion)
        val imgAvatar: ImageView = itemView.findViewById(R.id.img_avatar)
        val txtNombre: TextView = itemView.findViewById(R.id.txt_nombre)
        val txtPuntos: TextView = itemView.findViewById(R.id.txt_puntos)
        val txtNivel: TextView = itemView.findViewById(R.id.txt_nivel)
        val txtRacha: TextView = itemView.findViewById(R.id.txt_racha)
        val txtPrecision: TextView = itemView.findViewById(R.id.txt_precision)

        fun bind(estudiante: Estudiante, position: Int) {
            // Posición en ranking
            txtPosicion.text = estudiante.posicionRanking.toString()

            // Cambiar color del badge según posición
            val badgeColor = when (estudiante.posicionRanking) {
                1 -> R.color.ranking_gold
                2 -> R.color.ranking_silver
                3 -> R.color.ranking_bronze
                else -> R.color.primary
            }
            badgeRanking.setCardBackgroundColor(
                ContextCompat.getColor(itemView.context, badgeColor)
            )

            // Información del estudiante
            txtNombre.text = "${estudiante.nombre} ${estudiante.apellido}"
            txtPuntos.text = "${estudiante.puntosTotal} puntos"
            txtNivel.text = "Nivel ${estudiante.getNivel()}"

            // Estadísticas
            txtRacha.text = estudiante.rachaActual.toString()
            txtPrecision.text = "${String.format("%.0f", estudiante.getPorcentajeAciertos())}%"

            // Click listener
            itemView.setOnClickListener {
                onEstudianteClick(estudiante)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EstudianteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_estudiante, parent, false)
        return EstudianteViewHolder(view)
    }

    override fun onBindViewHolder(holder: EstudianteViewHolder, position: Int) {
        holder.bind(estudiantes[position], position)
    }

    override fun getItemCount(): Int = estudiantes.size

    fun actualizarLista(nuevaLista: MutableList<Estudiante>) {
        estudiantes = nuevaLista
        notifyDataSetChanged()
    }

    fun filtrar(query: String) {
        val listaFiltrada = if (query.isEmpty()) {
            Estudiante.obtenerEstudiantesEjemplo()
        } else {
            Estudiante.obtenerEstudiantesEjemplo().filter {
                it.nombre.contains(query, ignoreCase = true) ||
                        it.apellido.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        actualizarLista(listaFiltrada)
    }
}