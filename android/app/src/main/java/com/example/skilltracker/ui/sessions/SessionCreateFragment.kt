package com.example.skilltracker.ui.sessions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.skilltracker.R
import com.example.skilltracker.databinding.FragmentSessionCreateBinding
import com.example.skilltracker.domain.model.Skill
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SessionCreateFragment : Fragment() {

    private var _binding: FragmentSessionCreateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionCreateViewModel by viewModels()
    private var selectedSkill: Skill? = null
    private var selectedSource: String? = null
    private var difficultySelected = false
    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

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

        setupSourceSpinner()
        setupDifficultySeekBar()
        setupDateTimePickers()

        binding.saveSessionButton.setOnClickListener {
            val skillId = selectedSkill?.id
            val duration = binding.sessionDurationInput.text?.toString()?.toIntOrNull()
            val notes = binding.sessionNotesInput.text?.toString()?.trim()
            val difficulty = if (difficultySelected) binding.sessionDifficultySeekBar.progress + 1 else null
            val source = selectedSource
            val sessionDate = buildSessionInstant()

            if (skillId != null && duration != null && duration > 0) {
                viewModel.createSession(skillId, duration, notes, difficulty, source, sessionDate)
            } else {
                Toast.makeText(requireContext(), R.string.session_input_error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.skills.observe(viewLifecycleOwner) { skills ->
            updateSkillSpinner(skills)
        }

        viewModel.isSuccess.observe(viewLifecycleOwner) { success ->
            when (success) {
                true -> {
                    Toast.makeText(requireContext(), R.string.session_created, Toast.LENGTH_SHORT).show()
                    clearForm()
                    viewModel.resetState()
                }
                false -> {
                    Toast.makeText(requireContext(), R.string.session_create_failed, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                null -> Unit
            }
        }
    }

    private fun setupDateTimePickers() {
        binding.selectSessionDateButton.setOnClickListener {
            val now = LocalDate.now()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    updateDateTimeText()
                },
                now.year,
                now.monthValue - 1,
                now.dayOfMonth
            ).show()
        }

        binding.selectSessionTimeButton.setOnClickListener {
            val now = LocalTime.now()
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedTime = LocalTime.of(hourOfDay, minute)
                    updateDateTimeText()
                },
                now.hour,
                now.minute,
                true
            ).show()
        }

        binding.clearSessionDateButton.setOnClickListener {
            selectedDate = null
            selectedTime = null
            updateDateTimeText()
        }

        updateDateTimeText()
    }

    private fun buildSessionInstant(): Instant? {
        val date = selectedDate
        val time = selectedTime
        return if (date != null && time != null) {
            date.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
        } else if (date != null) {
            date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        } else {
            null
        }
    }

    private fun updateDateTimeText() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val displayText = when {
            selectedDate == null -> getString(R.string.session_date_placeholder)
            selectedTime == null -> getString(
                R.string.session_date_selected_date,
                selectedDate.toString()
            )
            else -> formatter.format(selectedDate!!.atTime(selectedTime))
        }
        binding.sessionDateValue.text = displayText
    }

    private fun setupSourceSpinner() {
        val sourceOptions = listOf(getString(R.string.session_source_none)) +
                resources.getStringArray(R.array.session_sources).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sourceOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sessionSourceSpinner.adapter = adapter
        binding.sessionSourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSource = if (position == 0) null else sourceOptions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedSource = null
            }
        }
    }

    private fun setupDifficultySeekBar() {
        binding.sessionDifficultySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    difficultySelected = true
                }
                if (difficultySelected) {
                    binding.sessionDifficultyValue.text = getString(R.string.session_difficulty_value, progress + 1)
                } else {
                    binding.sessionDifficultyValue.text = getString(R.string.session_difficulty_placeholder)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                difficultySelected = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                binding.sessionDifficultyValue.text = getString(
                    R.string.session_difficulty_value,
                    binding.sessionDifficultySeekBar.progress + 1
                )
            }
        })
        binding.sessionDifficultyValue.text = getString(R.string.session_difficulty_placeholder)
    }

    private fun updateSkillSpinner(skills: List<Skill>) {
        if (skills.isEmpty()) {
            selectedSkill = null
            binding.sessionSkillSpinner.adapter = null
            return
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, skills.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sessionSkillSpinner.adapter = adapter
        binding.sessionSkillSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSkill = skills.getOrNull(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedSkill = null
            }
        }
        selectedSkill = skills.firstOrNull()
        if (skills.isNotEmpty()) {
            binding.sessionSkillSpinner.setSelection(0)
        }
    }

    private fun clearForm() {
        binding.sessionDurationInput.text?.clear()
        binding.sessionNotesInput.text?.clear()
        binding.sessionSourceSpinner.setSelection(0)
        if ((binding.sessionSkillSpinner.adapter?.count ?: 0) > 0) {
            binding.sessionSkillSpinner.setSelection(0)
            selectedSkill = viewModel.skills.value?.firstOrNull()
        } else {
            selectedSkill = null
        }
        difficultySelected = false
        binding.sessionDifficultySeekBar.progress = 2
        binding.sessionDifficultyValue.text = getString(R.string.session_difficulty_placeholder)
        selectedSource = null
        selectedDate = null
        selectedTime = null
        updateDateTimeText()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
