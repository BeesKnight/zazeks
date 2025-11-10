package com.example.skilltracker.ui.skills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skilltracker.R
import com.example.skilltracker.databinding.FragmentSkillsListBinding

class SkillsListFragment : Fragment() {

    private var _binding: FragmentSkillsListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SkillsListViewModel by viewModels()
    private val adapter = SkillsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkillsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.skillsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.skillsRecyclerView.adapter = adapter

        binding.addSkillButton.setOnClickListener {
            findNavController().navigate(R.id.action_skillsListFragment_to_skillCreateFragment)
        }

        binding.addSessionButton.setOnClickListener {
            findNavController().navigate(R.id.action_skillsListFragment_to_sessionCreateFragment)
        }

        binding.viewStatsButton.setOnClickListener {
            findNavController().navigate(R.id.action_skillsListFragment_to_statsFragment)
        }

        viewModel.skills.observe(viewLifecycleOwner) { skills ->
            adapter.submitList(skills)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
