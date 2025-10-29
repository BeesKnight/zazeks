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
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import androidx.gridlayout.widget.GridLayout

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
        binding.currentPlayer.text = getString(R.string.game_current_player, model.currentPlayer)
        binding.turnLabel.text = getString(R.string.game_turn, model.turnCount)

        binding.completionLabel.isVisible = model.isCompleted
        binding.completionLabel.text = if (model.isCompleted) {
            getString(R.string.game_completion)
        } else {
            null
        }
        binding.winnerLabel.isVisible = model.isCompleted
        binding.winnerLabel.text = when {
            model.winner != null -> getString(R.string.game_winner_format, model.winner)
            model.isCompleted -> getString(R.string.game_draw)
            else -> null
        }

        renderBoard(model)
    }

    private fun renderBoard(model: GameUiModel) {
        val grid = binding.boardGrid
        grid.removeAllViews()
        grid.columnCount = model.boardRows.firstOrNull()?.length ?: 0
        grid.rowCount = model.boardRows.size
        if (grid.columnCount == 0 || grid.rowCount == 0) {
            return
        }
        model.boardRows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cell ->
                val button = MaterialButton(requireContext()).apply {
                    text = cell.takeIf { it != ' ' }?.toString() ?: ""
                    isEnabled = !model.isCompleted && cell == ' '
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }
                    setOnClickListener { viewModel.onCellSelected(rowIndex, columnIndex) }
                }
                grid.addView(button)
            }
        }
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
