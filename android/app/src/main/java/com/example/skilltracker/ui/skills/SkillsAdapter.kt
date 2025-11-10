package com.example.skilltracker.ui.skills

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        private val category: TextView = itemView.findViewById(R.id.skillCategory)
        private val archivedLabel: TextView = itemView.findViewById(R.id.skillArchivedLabel)
        private val colorIndicator: View = itemView.findViewById(R.id.skillColorIndicator)
        private val defaultColor = ContextCompat.getColor(itemView.context, android.R.color.darker_gray)

        fun bind(skill: Skill) {
            title.text = skill.name
            description.text = skill.description.orEmpty()
            val categoryText = skill.category
            if (!categoryText.isNullOrBlank()) {
                category.text = categoryText
                category.visibility = View.VISIBLE
            } else {
                category.visibility = View.GONE
            }
            archivedLabel.visibility = if (skill.archived) View.VISIBLE else View.GONE
            val colorHex = skill.color
            val parsedColor = colorHex?.takeIf { it.isNotBlank() }?.let { hex ->
                runCatching { Color.parseColor(hex) }.getOrNull()
            }
            colorIndicator.setBackgroundColor(parsedColor ?: defaultColor)
            val alpha = if (skill.archived) 0.5f else 1f
            itemView.alpha = alpha
        }
    }
}
