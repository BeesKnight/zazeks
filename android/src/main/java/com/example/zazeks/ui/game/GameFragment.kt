package com.example.zazeks.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.databinding.FragmentGameBinding
import com.example.zazeks.ui.common.Event
import com.example.zazeks.ui.menu.MainMenuFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameFragment : Fragment() {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.errorActionButton.setOnClickListener { viewModel.onErrorAction() }
        binding.quitButton.setOnClickListener { viewModel.onQuitToMenu() }
        binding.rockButton.setOnClickListener { viewModel.onGestureSelected("rock") }
        binding.paperButton.setOnClickListener { viewModel.onGestureSelected("paper") }
        binding.scissorsButton.setOnClickListener { viewModel.onGestureSelected("scissors") }
        binding.restartRoundButton.setOnClickListener { viewModel.onRestartRound() }
        binding.confirmRoundButton.setOnClickListener { viewModel.onConfirmRoundResult() }

        viewModel.observeGameState().observe(viewLifecycleOwner, ::renderState)
        viewModel.effects().observe(viewLifecycleOwner, ::handleEffect)

        when {
            arguments?.getBoolean(MainMenuFragment.ARG_START_NEW_GAME) == true -> {
                viewModel.onStartNewGame()
                arguments?.remove(MainMenuFragment.ARG_START_NEW_GAME)
            }
            arguments?.getBoolean(MainMenuFragment.ARG_RESUME_GAME) == true -> {
                viewModel.onResumeGame()
                arguments?.remove(MainMenuFragment.ARG_RESUME_GAME)
            }
            else -> {
                viewModel.onResumeGame()
            }
        }
    }

    private fun renderState(state: ViewState) {
        binding.loadingIndicator.isVisible = state is ViewState.Loading
        binding.gameCard.isVisible = state is ViewState.Content
        binding.errorContainer.isVisible = state is ViewState.Error

        if (state is ViewState.Content) {
            renderContent(state.session)
        } else if (state is ViewState.Error) {
            binding.errorTitle.text = state.title
            binding.errorMessage.text = state.message
            binding.errorActionButton.isVisible = state.action != null
            binding.errorActionButton.isEnabled = state.action != null
            binding.errorActionButton.text = state.action?.label ?: getString(R.string.error_retry)
        }
    }

    private fun renderContent(model: GameUiModel) {
        binding.sessionId.text = getString(R.string.game_session_format, model.sessionId)
        binding.roundLabel.text = getString(R.string.game_round_label, model.round)
        binding.timerLabel.text = getString(R.string.game_timer_label, model.remainingSeconds)
        binding.scoreLabel.text = getString(R.string.game_score_label, model.playerScore, model.opponentScore)
        binding.playerGestureLabel.text = getString(
            R.string.game_player_gesture,
            formatGesture(model.playerGesture)
        )
        binding.opponentGestureLabel.text = getString(
            R.string.game_opponent_gesture,
            formatGesture(model.opponentGesture)
        )

        val roundResult = model.roundResult
        binding.roundResultLabel.isVisible = !roundResult.isNullOrBlank()
        binding.roundResultLabel.text = roundResult?.let {
            getString(R.string.game_round_result_label, formatOutcome(it))
        }

        binding.matchResultLabel.isVisible = model.isMatchCompleted
        binding.matchResultLabel.text = if (model.isMatchCompleted) {
            model.matchResult?.let { result ->
                getString(R.string.game_match_result_label, formatOutcome(result))
            }
                ?: getString(R.string.game_match_in_progress)
        } else {
            null
        }

        val canPlay = !model.isRoundCompleted && !model.isMatchCompleted
        binding.rockButton.isEnabled = canPlay
        binding.paperButton.isEnabled = canPlay
        binding.scissorsButton.isEnabled = canPlay
        binding.gestureButtonsContainer.isVisible = !model.isMatchCompleted
        binding.restartRoundButton.isEnabled = !model.isMatchCompleted
        binding.confirmRoundButton.isEnabled = model.isRoundCompleted
    }

    private fun formatGesture(value: String?): String = when (value?.lowercase()) {
        "rock" -> getString(R.string.game_select_rock)
        "paper" -> getString(R.string.game_select_paper)
        "scissors" -> getString(R.string.game_select_scissors)
        null -> getString(R.string.game_gesture_unknown)
        else -> value
    }

    private fun formatOutcome(code: String?): String = when (code?.lowercase()) {
        "win" -> getString(R.string.game_result_win)
        "loss" -> getString(R.string.game_result_loss)
        "draw" -> getString(R.string.game_result_draw)
        else -> code ?: getString(R.string.game_gesture_unknown)
    }

    private fun handleEffect(event: Event<GameEffect>) {
        event.getContentIfNotHandled()?.let { effect ->
            when (effect) {
                is GameEffect.NavigateToResults -> navigateToResults(effect.result)
                GameEffect.NavigateToMenu -> findNavController().popBackStack(R.id.mainMenuFragment, false)
            }
        }
    }

    private fun navigateToResults(result: GameResultArgs) {
        val bundle = Bundle().apply {
            putParcelable(ARG_GAME_RESULT, result)
        }
        findNavController().navigate(R.id.action_gameFragment_to_resultsFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_GAME_RESULT = "game_result"
    }
}
