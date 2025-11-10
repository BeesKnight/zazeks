package com.example.skilltracker.ui.skills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skilltracker.R
import com.example.skilltracker.databinding.FragmentSkillEditBinding
import kotlinx.coroutines.launch

class SkillEditFragment : Fragment() {

    private var _binding: FragmentSkillEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SkillEditViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkillEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val skillId = requireArguments().getLong("skillId")
        viewModel.loadSkill(skillId)

        viewModel.skill.observe(viewLifecycleOwner) { skill ->
            if (skill != null) {
                binding.skillNameInput.setText(skill.name)
                binding.skillDescriptionInput.setText(skill.description.orEmpty())
                binding.skillCategoryInput.setText(skill.category.orEmpty())
                binding.skillColorInput.setText(skill.color.orEmpty())
            }
        }

        binding.saveSkillButton.setOnClickListener {
            val name = binding.skillNameInput.text?.toString()?.trim().orEmpty()
            val description = binding.skillDescriptionInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val category = binding.skillCategoryInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val color = binding.skillColorInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

            if (name.isBlank()) {
                Toast.makeText(requireContext(), R.string.skill_name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val success = viewModel.updateSkill(name, description, category, color)
                if (success) {
                    Toast.makeText(requireContext(), R.string.skill_updated, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), R.string.skill_update_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.archiveSkillButton.setOnClickListener {
            lifecycleScope.launch {
                val success = viewModel.archiveSkill()
                if (success) {
                    Toast.makeText(requireContext(), R.string.skill_archived, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), R.string.skill_archive_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
