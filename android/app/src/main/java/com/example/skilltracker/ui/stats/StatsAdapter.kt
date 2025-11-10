package com.example.skilltracker.ui.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.skilltracker.R
import com.example.skilltracker.domain.model.SkillStats

class StatsAdapter : RecyclerView.Adapter<StatsAdapter.StatsViewHolder>() {

    private val items = mutableListOf<SkillStats>()

    fun submitList(stats: List<SkillStats>) {
        items.clear()
        items.addAll(stats)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stat, parent, false)
        return StatsViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class StatsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.statSkillName)
        private val sessions: TextView = itemView.findViewById(R.id.statSessionCount)
        private val duration: TextView = itemView.findViewById(R.id.statDuration)

        fun bind(stat: SkillStats) {
            title.text = stat.skillName
            sessions.text = "Sessions: ${stat.sessionCount}"
            duration.text = "Minutes: ${stat.totalDurationMinutes}"
        }
    }
}
