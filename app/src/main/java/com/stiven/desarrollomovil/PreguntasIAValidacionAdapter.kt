// Archivo: app/src/main/java/com/stiven/desarrollomovil/PreguntasIAValidacionAdapter.kt

package com.stiven.desarrollomovil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stiven.desarrollomovil.databinding.ItemPreguntaValidacionBinding

// --- ¡CORRECCIÓN! ---
// Se añade el import que faltaba para que el archivo reconozca la clase PreguntaIA.
import com.stiven.desarrollomovil.models.PreguntaIA

// Typealias específico para este adapter
typealias OnPreguntaIAClickListener = (PreguntaIA) -> Unit

class PreguntasIAValidacionAdapter(
    private var preguntas: List<PreguntaIA>,
    private val onAprobarClick: OnPreguntaIAClickListener,
    private val onRechazarClick: OnPreguntaIAClickListener,
    private val onEditarClick: OnPreguntaIAClickListener
) : RecyclerView.Adapter<PreguntasIAValidacionAdapter.PreguntaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreguntaViewHolder {
        val binding = ItemPreguntaValidacionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PreguntaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreguntaViewHolder, position: Int) {
        val pregunta = preguntas[position]
        holder.bind(pregunta)
    }

    override fun getItemCount(): Int = preguntas.size

    inner class PreguntaViewHolder(private val binding: ItemPreguntaValidacionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val cards: List<MaterialCardView>
        private val texts: List<TextView>

        init {
            cards = with(binding) { listOf(cardOpcionA, cardOpcionB, cardOpcionC, cardOpcionD) }
            texts = with(binding) { listOf(txtOpcionA, txtOpcionB, txtOpcionC, txtOpcionD) }
        }

        fun bind(pregunta: PreguntaIA) {
            // El 'import' añadido soluciona los errores aquí.
            binding.txtPregunta.text = pregunta.texto

            // Asigna el texto a cada opción y oculta las que no se usan
            for (i in cards.indices) {
                if (i < pregunta.opciones.size) {
                    texts[i].text = pregunta.opciones[i].texto
                    cards[i].visibility = View.VISIBLE
                } else {
                    cards[i].visibility = View.GONE
                }
            }

            // Resetea los estilos y marca la respuesta correcta
            resetearEstilos()
            marcarRespuestaCorrecta(pregunta)

            // Configura los listeners para los botones de acción
            binding.btnAprobar.setOnClickListener { onAprobarClick(pregunta) }
            binding.btnRechazar.setOnClickListener { onRechazarClick(pregunta) }
            binding.btnEditar.setOnClickListener { onEditarClick(pregunta) }
        }

        private fun resetearEstilos() {

            val defaultBackgroundColor = ContextCompat.getColor(
                itemView.context,
                com.google.android.material.R.color.material_on_surface_disabled
            )
            val transparentColor = ContextCompat.getColor(itemView.context, android.R.color.transparent)

            cards.forEach { card ->
                card.setCardBackgroundColor(defaultBackgroundColor)
                card.strokeColor = transparentColor
                card.strokeWidth = 0
            }
        }


        private fun marcarRespuestaCorrecta(pregunta: PreguntaIA) {
            // --- ¡CORRECCIÓN DE LÓGICA! ---
            // Buscamos el índice de la primera opción que tenga 'esCorrecta = true'.
            val indiceCorrecto = pregunta.opciones.indexOfFirst { it.esCorrecta }

            if (indiceCorrecto != -1 && indiceCorrecto in cards.indices) {
                val cardCorrecta = cards[indiceCorrecto]
                val successColor = ContextCompat.getColor(itemView.context, R.color.success)
                val successBackgroundColor = ContextCompat.getColor(itemView.context, R.color.success_container)

                cardCorrecta.setCardBackgroundColor(successBackgroundColor)
                cardCorrecta.strokeColor = successColor
                cardCorrecta.strokeWidth = (2 * itemView.resources.displayMetrics.density).toInt()
            }
        }
    }

    /**
     * Actualiza la lista de preguntas en el adaptador y notifica al RecyclerView para que se redibuje.
     */
    fun actualizarPreguntas(nuevasPreguntas: List<PreguntaIA>) {
        preguntas = nuevasPreguntas
        // Es más eficiente usar DiffUtil, pero para simplicidad mantenemos notifyDataSetChanged()
        notifyDataSetChanged()
    }
}
