package com.example.skilltracker.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.skilltracker.databinding.FragmentSessionCreateBinding

class SessionCreateFragment : Fragment() {

    private var _binding: FragmentSessionCreateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionCreateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveSessionButton.setOnClickListener {
            val skillId = binding.sessionSkillIdInput.text?.toString()?.toLongOrNull()
            val duration = binding.sessionDurationInput.text?.toString()?.toIntOrNull()
            val notes = binding.sessionNotesInput.text?.toString()
            if (skillId != null && duration != null) {
                viewModel.createSession(skillId, duration, notes)
            } else {
                Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isSuccess.observe(viewLifecycleOwner) { success ->
            when (success) {
                true -> {
                    Toast.makeText(requireContext(), "Session created", Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                false -> {
                    Toast.makeText(requireContext(), "Failed to create session", Toast.LENGTH_SHORT).show()
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
