package com.example.skilltracker.ui.skills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.skilltracker.R
import com.example.skilltracker.databinding.FragmentSkillCreateBinding

class SkillCreateFragment : Fragment() {

    private var _binding: FragmentSkillCreateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SkillCreateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkillCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveSkillButton.setOnClickListener {
            val name = binding.skillNameInput.text?.toString().orEmpty()
            val description = binding.skillDescriptionInput.text?.toString()
            val category = binding.skillCategoryInput.text?.toString()
            val color = binding.skillColorInput.text?.toString()
            viewModel.createSkill(name, description, category, color)
        }

        viewModel.isSuccess.observe(viewLifecycleOwner) { isSuccess ->
            when (isSuccess) {
                true -> {
                    Toast.makeText(requireContext(), R.string.skill_created, Toast.LENGTH_SHORT).show()
                    clearInputs()
                    viewModel.resetState()
                }
                false -> {
                    Toast.makeText(requireContext(), R.string.skill_create_failed, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                null -> Unit
            }
        }
    }

    private fun clearInputs() {
        binding.skillNameInput.text?.clear()
        binding.skillDescriptionInput.text?.clear()
        binding.skillCategoryInput.text?.clear()
        binding.skillColorInput.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
