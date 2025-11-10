package com.example.skilltracker.ui.skills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
            viewModel.createSkill(name, description)
        }

        viewModel.isSuccess.observe(viewLifecycleOwner) { isSuccess ->
            when (isSuccess) {
                true -> {
                    Toast.makeText(requireContext(), "Skill created", Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                false -> {
                    Toast.makeText(requireContext(), "Failed to create skill", Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                null -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
