package com.stiven.desarrollomovil

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stiven.desarrollomovil.databinding.ItemPreguntaValidacionBinding

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
        private val texts: List<android.widget.TextView>

        init {
            cards = with(binding) { listOf(cardOpcionA, cardOpcionB, cardOpcionC, cardOpcionD) }
            texts = with(binding) { listOf(txtOpcionA, txtOpcionB, txtOpcionC, txtOpcionD) }
        }

        fun bind(pregunta: PreguntaIA) {
            binding.txtPregunta.text = pregunta.pregunta

            // Asigna el texto a cada opción y oculta las que no se usan
            for (i in cards.indices) {
                if (i < pregunta.opciones.size) {
                    texts[i].text = pregunta.opciones[i]
                    cards[i].visibility = android.view.View.VISIBLE
                } else {
                    cards[i].visibility = android.view.View.GONE
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
            val defaultBackgroundColor = ContextCompat.getColor(itemView.context, R.color.surface_variant)
            val transparentColor = ContextCompat.getColor(itemView.context, android.R.color.transparent)

            cards.forEach { card ->
                card.setCardBackgroundColor(defaultBackgroundColor)
                card.strokeColor = transparentColor
                card.strokeWidth = 0
            }
        }

        private fun marcarRespuestaCorrecta(pregunta: PreguntaIA) {
            val indiceCorrecto = pregunta.respuestaCorrecta

            if (indiceCorrecto in cards.indices) {
                val cardCorrecta = cards[indiceCorrecto]
                val successColor = ContextCompat.getColor(itemView.context, R.color.success)
                val successBackgroundColor = ContextCompat.getColor(itemView.context, R.color.quiz_neutral)

                cardCorrecta.setCardBackgroundColor(successBackgroundColor)
                cardCorrecta.strokeColor = successColor
                cardCorrecta.strokeWidth = (2 * itemView.resources.displayMetrics.density).toInt()
            }
        }
    }

    fun actualizarPreguntas(nuevasPreguntas: List<PreguntaIA>) {
        preguntas = nuevasPreguntas
        notifyDataSetChanged()
    }
}