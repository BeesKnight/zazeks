package com.example.zazeks.ui.results

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.zazeks.R
import com.example.zazeks.core.gestures.formatGesture
import com.example.zazeks.databinding.ItemRoundResultBinding
import com.example.zazeks.domain.results.ResultMode
import com.example.zazeks.domain.results.RoundResult
import java.time.Instant
import java.time.format.DateTimeFormatter

class RoundResultsAdapter(
    private val timestampFormatter: DateTimeFormatter,
) : ListAdapter<RoundResult, RoundResultsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoundResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRoundResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RoundResult) {
            val resources = binding.root.resources
            val outcome = formatOutcome(item.result)
            binding.modeLabel.text = formatMode(item.mode)
            binding.resultLabel.text = resources.getString(
                R.string.results_round_outcome_format,
                outcome
            )
            binding.gesturesLabel.text = resources.getString(
                R.string.results_round_gestures_format,
                binding.root.context.formatGesture(item.playerGesture),
                binding.root.context.formatGesture(item.opponentGesture)
            )
            val hasScore = item.playerScore != null && item.opponentScore != null
            binding.scoreLabel.isVisible = hasScore
            if (hasScore) {
                binding.scoreLabel.text = resources.getString(
                    R.string.results_score_label,
                    item.playerScore!!,
                    item.opponentScore!!
                )
            }
            val timestampLabel = formatTimestamp(item.timestamp)
            binding.timestampLabel.text = timestampLabel?.let {
                resources.getString(R.string.results_round_played_at_format, it)
            } ?: resources.getString(R.string.results_round_played_at_unknown)
        }

        private fun formatOutcome(code: String?): String = when (code?.lowercase()) {
            "win" -> binding.root.context.getString(R.string.game_result_win)
            "loss" -> binding.root.context.getString(R.string.game_result_loss)
            "draw" -> binding.root.context.getString(R.string.game_result_draw)
            null -> binding.root.context.getString(R.string.results_match_result_pending)
            else -> code
        }

        private fun formatMode(mode: ResultMode): String = when (mode) {
            ResultMode.OFFLINE -> binding.root.context.getString(R.string.results_mode_offline)
            ResultMode.ONLINE -> binding.root.context.getString(R.string.results_mode_online)
        }

        private fun formatTimestamp(instant: Instant?): String? = instant?.let {
            timestampFormatter.format(it)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RoundResult>() {
        override fun areItemsTheSame(oldItem: RoundResult, newItem: RoundResult): Boolean =
            oldItem.id == newItem.id && oldItem.mode == newItem.mode

        override fun areContentsTheSame(oldItem: RoundResult, newItem: RoundResult): Boolean =
            oldItem == newItem
    }
}
