package com.example.skilltracker.ui.skills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.skilltracker.R
import com.example.skilltracker.domain.model.Skill

class SkillsAdapter : RecyclerView.Adapter<SkillsAdapter.SkillViewHolder>() {

    private val items = mutableListOf<Skill>()

    fun submitList(skills: List<Skill>) {
        items.clear()
        items.addAll(skills)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skill, parent, false)
        return SkillViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class SkillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.skillTitle)
        private val description: TextView = itemView.findViewById(R.id.skillDescription)

        fun bind(skill: Skill) {
            title.text = skill.name
            description.text = skill.description ?: ""
        }
    }
}
