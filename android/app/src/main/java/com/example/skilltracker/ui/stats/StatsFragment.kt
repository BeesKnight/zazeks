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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skilltracker.R
import com.example.skilltracker.databinding.FragmentStatsBinding

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels()
    private val adapter = StatsAdapter { stat ->
        val args = bundleOf(
            "skillId" to stat.skillId,
            "skillName" to stat.skillName
        )
        findNavController().navigate(R.id.action_statsFragment_to_skillDetailsFragment, args)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.statsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.statsRecyclerView.adapter = adapter

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            adapter.submitList(stats)
        }

        viewModel.overview.observe(viewLifecycleOwner) { overview ->
            if (overview != null) {
                binding.totalMinutesValue.text = getString(R.string.overview_total_minutes) + ": " + overview.totalMinutes
                binding.sessionsCountValue.text = getString(R.string.overview_sessions_count) + ": " + overview.sessionsCount
                binding.skillsCountValue.text = getString(R.string.overview_skills_count) + ": " + overview.skillsCount
                renderInactiveSkills(overview.inactiveSkills.map { it.skillName to it.daysSinceLastSession })
            } else {
                binding.totalMinutesValue.text = getString(R.string.overview_total_minutes)
                binding.sessionsCountValue.text = getString(R.string.overview_sessions_count)
                binding.skillsCountValue.text = getString(R.string.overview_skills_count)
                renderInactiveSkills(emptyList())
            }
        }

        viewModel.loadStats()
    }

    private fun renderInactiveSkills(items: List<Pair<String, Long>>) {
        binding.inactiveSkillsContainer.removeAllViews()
        val padding = (8 * resources.displayMetrics.density).toInt()
        if (items.isEmpty()) {
            val textView = TextView(requireContext())
            textView.text = getString(R.string.inactive_skills_empty)
            textView.setPadding(0, padding, 0, padding)
            binding.inactiveSkillsContainer.addView(textView)
            return
        }
        items.forEach { (name, days) ->
            val textView = TextView(requireContext())
            textView.text = getString(R.string.inactive_skill_format, name, days)
            textView.setPadding(0, padding, 0, padding)
            binding.inactiveSkillsContainer.addView(textView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
