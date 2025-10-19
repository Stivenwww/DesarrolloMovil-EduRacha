package com.stiven.sos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stiven.sos.databinding.ItemRankingBinding

class RankingAdapter : ListAdapter<RankingStudent, RankingAdapter.RankingViewHolder>(RankingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val binding = ItemRankingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RankingViewHolder(private val binding: ItemRankingBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(student: RankingStudent) {
            binding.tvPosition.text = student.position.toString()
            binding.tvName.text = student.name
            binding.tvPoints.text = student.points
            binding.tvLevel.text = student.level

            // Resaltar si es el usuario actual
            if (student.isCurrentUser) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.wisdom_light)
                )
                // Cambiar el color de fondo de la tarjeta para resaltar
                binding.cardRanking.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.wisdom_light)
                )
                // Aumentar la elevación para resaltar más
                binding.cardRanking.cardElevation = 8f
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.transparent)
                )
                binding.cardRanking.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.surface)
                )
                binding.cardRanking.cardElevation = 2f
            }

            // Configurar colores de posición
            when (student.position) {
                1 -> {
                    binding.tvPosition.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.level_gold)
                    )
                }
                2 -> {
                    binding.tvPosition.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.level_silver)
                    )
                }
                3 -> {
                    binding.tvPosition.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.level_bronze)
                    )
                }
                else -> {
                    binding.tvPosition.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.text_primary)
                    )
                }
            }
        }
    }
}

class RankingDiffCallback : DiffUtil.ItemCallback<RankingStudent>() {
    override fun areItemsTheSame(oldItem: RankingStudent, newItem: RankingStudent): Boolean {
        return oldItem.position == newItem.position
    }

    override fun areContentsTheSame(oldItem: RankingStudent, newItem: RankingStudent): Boolean {
        return oldItem == newItem
    }
}