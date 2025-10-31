package com.example.zazeks.ui.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.databinding.FragmentResultsBinding
import com.example.zazeks.ui.offline.OfflineMatchFragment
import com.example.zazeks.ui.offline.GameResultArgs
import com.example.zazeks.ui.menu.MainMenuFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ResultsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backToMenuButton.setOnClickListener {
            findNavController().navigate(R.id.action_resultsFragment_to_mainMenuFragment)
        }
        binding.newGameButton.setOnClickListener {
            val args = Bundle().apply { putBoolean(MainMenuFragment.ARG_START_NEW_GAME, true) }
            findNavController().navigate(R.id.action_resultsFragment_to_offlineMatchFragment, args)
        }

        val args = arguments?.getParcelable<GameResultArgs>(OfflineMatchFragment.ARG_GAME_RESULT)
        arguments?.remove(OfflineMatchFragment.ARG_GAME_RESULT)
        viewModel.load(args)

        viewModel.state.observe(viewLifecycleOwner, ::renderState)
    }

    private fun renderState(state: ResultsViewState) {
        when (state) {
            ResultsViewState.Empty -> showEmptyState()
            is ResultsViewState.Content -> showContent(state)
        }
    }

    private fun showEmptyState() {
        binding.resultsCard.isVisible = false
        binding.emptyState.isVisible = true
    }

    private fun showContent(content: ResultsViewState.Content) {
        binding.resultsCard.isVisible = true
        binding.emptyState.isVisible = false
        binding.sessionLabel.text = getString(R.string.results_session_label, content.sessionId)
        binding.roundsLabel.text = getString(R.string.results_rounds_label, content.roundsPlayed)
        binding.scoreLabel.text = getString(
            R.string.results_score_label,
            content.playerScore,
            content.opponentScore
        )
        binding.playerGestureLabel.text = getString(
            R.string.results_player_gesture_label,
            formatGesture(content.playerGesture)
        )
        binding.opponentGestureLabel.text = getString(
            R.string.results_opponent_gesture_label,
            formatGesture(content.opponentGesture)
        )
        binding.matchResultLabel.text = content.matchResult?.let {
            getString(R.string.results_match_result_label, formatOutcome(it))
        } ?: getString(R.string.results_match_result_pending)
    }

    private fun formatGesture(value: String?): String = when (value?.lowercase()) {
        "rock" -> getString(R.string.game_select_rock)
        "paper" -> getString(R.string.game_select_paper)
        "scissors" -> getString(R.string.game_select_scissors)
        null -> getString(R.string.results_gesture_unknown)
        else -> value
    }

    private fun formatOutcome(code: String?): String = when (code?.lowercase()) {
        "win" -> getString(R.string.game_result_win)
        "loss" -> getString(R.string.game_result_loss)
        "draw" -> getString(R.string.game_result_draw)
        else -> code ?: getString(R.string.results_gesture_unknown)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
