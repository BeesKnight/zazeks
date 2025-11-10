package com.example.skilltracker.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.skilltracker.R
import com.example.skilltracker.databinding.FragmentSkillDetailsBinding
import com.example.skilltracker.domain.model.Session
import com.example.skilltracker.domain.model.SkillDailyStats
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class SkillDetailsFragment : Fragment() {

    private var _binding: FragmentSkillDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SkillDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkillDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val skillId = requireArguments().getLong("skillId")
        val skillName = requireArguments().getString("skillName")
        binding.skillNameText.text = skillName ?: getString(R.string.skill_details_title_placeholder)

        viewModel.details.observe(viewLifecycleOwner) { details ->
            if (details != null) {
                binding.skillNameText.text = details.skillName
                binding.totalMinutesText.text = getString(R.string.skill_details_total_minutes, details.totalMinutes)
                binding.sessionsCountText.text = getString(R.string.skill_details_sessions, details.sessionsCount)
                val averageText = details.averageDifficulty?.let {
                    val formatted = String.format(Locale.getDefault(), "%.2f", it)
                    getString(R.string.skill_details_average_difficulty, formatted)
                } ?: getString(R.string.skill_details_average_difficulty_na)
                binding.averageDifficultyText.text = averageText
                renderDailyStats(details.byDay)
            } else {
                binding.totalMinutesText.text = getString(R.string.skill_details_total_minutes, 0L)
                binding.sessionsCountText.text = getString(R.string.skill_details_sessions, 0L)
                binding.averageDifficultyText.text = getString(R.string.skill_details_average_difficulty_na)
                renderDailyStats(emptyList())
            }
        }

        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            renderSessions(sessions)
        }

        viewModel.loadSkillDetails(skillId)
    }

    private fun renderDailyStats(items: List<SkillDailyStats>) {
        val container = binding.dailyStatsContainer
        container.removeAllViews()
        val context = container.context
        val padding = (8 * resources.displayMetrics.density).toInt()
        if (items.isEmpty()) {
            val emptyView = TextView(context)
            emptyView.text = getString(R.string.skill_details_empty_daily)
            emptyView.setPadding(0, padding, 0, padding)
            container.addView(emptyView)
            return
        }
        items.forEach { stat ->
            val textView = TextView(context)
            textView.text = getString(R.string.skill_details_daily_item, stat.date, stat.minutes)
            textView.setPadding(0, padding, 0, padding)
            container.addView(textView)
        }
    }

    private fun renderSessions(sessions: List<Session>) {
        val container = binding.sessionsContainer
        container.removeAllViews()
        val context = container.context
        val padding = (8 * resources.displayMetrics.density).toInt()
        if (sessions.isEmpty()) {
            val emptyView = TextView(context)
            emptyView.text = getString(R.string.skill_details_empty_sessions)
            emptyView.setPadding(0, padding, 0, padding)
            container.addView(emptyView)
            return
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
        sessions.sortedByDescending { runCatching { Instant.parse(it.sessionDate) }.getOrNull() ?: Instant.EPOCH }
            .forEach { session ->
                val textView = TextView(context)
                val instant = runCatching { Instant.parse(session.sessionDate) }.getOrNull()
                val formattedDate = instant?.let { formatter.format(it) } ?: session.sessionDate
                textView.text = getString(
                    R.string.skill_details_session_item,
                    formattedDate,
                    session.durationMinutes
                )
                textView.setPadding(0, padding, 0, padding)
                textView.setOnClickListener {
                    val args = bundleOf("sessionId" to session.id)
                    findNavController().navigate(R.id.action_skillDetailsFragment_to_sessionEditFragment, args)
                }
                container.addView(textView)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
