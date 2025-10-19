package com.stiven.sos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.stiven.sos.databinding.ActivityRankingBinding

data class RankingStudent(
    val position: Int,
    val name: String,
    val points: String,
    val level: String,
    val isCurrentUser: Boolean = false
)

class RankingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRankingBinding
    private lateinit var rankingAdapter: RankingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadRankingData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        rankingAdapter = RankingAdapter()
        binding.recyclerViewRanking.apply {
            layoutManager = LinearLayoutManager(this@RankingActivity)
            adapter = rankingAdapter
        }
    }

    private fun loadRankingData() {
        val rankingList = listOf(
            RankingStudent(1, "Laura", "2500 pts", "Nivel 8"),
            RankingStudent(2, "Ana", "2200 pts", "Nivel 7"),
            RankingStudent(3, "Jorge", "2000 pts", "Nivel 7"),
            RankingStudent(4, "Pedro", "1800 pts", "Nivel 6"),
            RankingStudent(5, "Carolina", "1600 pts", "Nivel 5"),
            RankingStudent(6, "Alejandro", "1400 pts", "Nivel 4"),
            RankingStudent(7, "María", "1200 pts", "Nivel 3"),
            RankingStudent(8, "Valentina", "1000 pts", "Nivel 3", true), // Usuario actual
            RankingStudent(9, "Carlos", "900 pts", "Nivel 2"),
            RankingStudent(10, "Sofia", "800 pts", "Nivel 2")
        )

        rankingAdapter.submitList(rankingList)

        // Actualizar podio
        if (rankingList.size >= 3) {
            binding.tvFirstPlace.text = rankingList[0].name
            binding.tvSecondPlace.text = rankingList[1].name
            binding.tvThirdPlace.text = rankingList[2].name
        }
    }
}