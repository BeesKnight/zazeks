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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skilltracker.R
import com.example.skilltracker.databinding.FragmentSessionEditBinding
import com.example.skilltracker.domain.model.Skill
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class SessionEditFragment : Fragment() {

    private var _binding: FragmentSessionEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionEditViewModel by viewModels()

    private var skillItems: List<Skill> = emptyList()
    private var selectedSkill: Skill? = null
    private var selectedSource: String? = null
    private var difficultySelected = false
    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var pendingSkillId: Long? = null
    private lateinit var sourceOptions: List<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSourceSpinner()
        setupDifficultySeekBar()
        setupDateTimePickers()

        val sessionId = requireArguments().getLong("sessionId")
        viewModel.loadData(sessionId)

        viewModel.skills.observe(viewLifecycleOwner) { skills ->
            skillItems = skills
            updateSkillSpinner()
        }

        viewModel.session.observe(viewLifecycleOwner) { session ->
            if (session != null) {
                pendingSkillId = session.skillId
                binding.sessionDurationInput.setText(session.durationMinutes.toString())
                binding.sessionNotesInput.setText(session.notes.orEmpty())
                difficultySelected = session.difficulty != null
                if (session.difficulty != null) {
                    binding.sessionDifficultySeekBar.progress = (session.difficulty - 1).coerceIn(0, 4)
                    binding.sessionDifficultyValue.text = getString(R.string.session_difficulty_value, session.difficulty)
                } else {
                    binding.sessionDifficultySeekBar.progress = 2
                    binding.sessionDifficultyValue.text = getString(R.string.session_difficulty_placeholder)
                }
                val sourceValue = session.source
                selectedSource = sourceValue
                updateSourceSelection(sourceValue)
                val instant = runCatching { Instant.parse(session.sessionDate) }.getOrNull()
                if (instant != null) {
                    val zoned = instant.atZone(ZoneId.systemDefault())
                    selectedDate = zoned.toLocalDate()
                    selectedTime = zoned.toLocalTime().withSecond(0).withNano(0)
                } else {
                    selectedDate = null
                    selectedTime = null
                }
                updateDateTimeText()
                updateSkillSpinner()
            }
        }

        binding.saveSessionButton.setOnClickListener {
            val skillId = selectedSkill?.id
            val duration = binding.sessionDurationInput.text?.toString()?.toIntOrNull()
            val notes = binding.sessionNotesInput.text?.toString()?.trim()
            val difficulty = if (difficultySelected) binding.sessionDifficultySeekBar.progress + 1 else null
            val source = selectedSource
            val sessionDate = buildSessionInstant()

            if (skillId != null && duration != null && duration > 0) {
                lifecycleScope.launch {
                    val success = viewModel.updateSession(
                        skillId = skillId,
                        durationMinutes = duration,
                        notes = notes?.takeIf { it.isNotBlank() },
                        difficulty = difficulty,
                        source = source,
                        sessionDate = sessionDate
                    )
                    if (success) {
                        Toast.makeText(requireContext(), R.string.session_updated, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(), R.string.session_update_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), R.string.session_input_error, Toast.LENGTH_SHORT).show()
            }
        }

        binding.deleteSessionButton.setOnClickListener {
            lifecycleScope.launch {
                val success = viewModel.deleteSession()
                if (success) {
                    Toast.makeText(requireContext(), R.string.session_deleted, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), R.string.session_delete_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSourceSpinner() {
        sourceOptions = listOf(getString(R.string.session_source_none)) +
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

    private fun updateSourceSelection(sourceValue: String?) {
        if (!::sourceOptions.isInitialized) {
            return
        }
        val index = sourceValue?.let { sourceOptions.indexOf(it).takeIf { i -> i >= 0 } } ?: 0
        binding.sessionSourceSpinner.setSelection(index)
        selectedSource = if (index == 0) null else sourceOptions[index]
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
    }

    private fun setupDateTimePickers() {
        binding.selectSessionDateButton.setOnClickListener {
            val now = selectedDate ?: LocalDate.now()
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
            val now = selectedTime ?: LocalTime.now()
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

    private fun updateSkillSpinner() {
        if (skillItems.isEmpty()) {
            binding.sessionSkillSpinner.adapter = null
            selectedSkill = null
            return
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, skillItems.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sessionSkillSpinner.adapter = adapter
        binding.sessionSkillSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedSkill = skillItems.getOrNull(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedSkill = null
            }
        }
        val targetId = pendingSkillId ?: selectedSkill?.id
        val index = targetId?.let { id -> skillItems.indexOfFirst { it.id == id } } ?: 0
        if (index >= 0 && index < skillItems.size) {
            binding.sessionSkillSpinner.setSelection(index)
            selectedSkill = skillItems[index]
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
