package com.example.zazeks.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.databinding.FragmentMainMenuBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainMenuViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startNewGameButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_offlineMatchFragment, Bundle().apply {
                putBoolean(ARG_START_NEW_GAME, true)
            })
        }
        binding.resumeGameButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_offlineMatchFragment, Bundle().apply {
                putBoolean(ARG_RESUME_GAME, true)
            })
        }
        binding.openResultsButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_resultsFragment)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            val subtitle = when {
                state.lastMatchResult != null ->
                    getString(R.string.menu_last_result_format, formatOutcome(state.lastMatchResult))
                state.hasCompletedGame -> getString(
                    R.string.menu_last_result_format,
                    getString(R.string.game_match_in_progress)
                )
                else -> getString(R.string.menu_last_result_placeholder)
            }
            binding.menuSubtitle.text = subtitle
            binding.resumeGameButton.isEnabled = state.canResume
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_START_NEW_GAME = "start_new_game"
        const val ARG_RESUME_GAME = "resume_game"
    }

    private fun formatOutcome(code: String): String = when (code.lowercase()) {
        "win" -> getString(R.string.game_result_win)
        "loss" -> getString(R.string.game_result_loss)
        "draw" -> getString(R.string.game_result_draw)
        else -> code
    }
}
