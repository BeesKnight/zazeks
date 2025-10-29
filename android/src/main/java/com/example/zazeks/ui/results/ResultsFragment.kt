package com.example.zazeks.ui.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.gridlayout.widget.GridLayout
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.databinding.FragmentResultsBinding
import com.example.zazeks.ui.game.GameFragment
import com.example.zazeks.ui.game.GameResultArgs
import com.example.zazeks.ui.menu.MainMenuFragment
import com.google.android.material.button.MaterialButton
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
            findNavController().navigate(R.id.action_resultsFragment_to_gameFragment, args)
        }

        val args = arguments?.getParcelable<GameResultArgs>(GameFragment.ARG_GAME_RESULT)
        arguments?.remove(GameFragment.ARG_GAME_RESULT)
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
        binding.turnsLabel.text = getString(R.string.results_turns_label, content.turnCount)
        binding.winnerLabel.text = if (content.isDraw) {
            getString(R.string.results_draw_label)
        } else {
            getString(R.string.results_winner_label, content.winner)
        }
        renderBoard(content.boardRows)
    }

    private fun renderBoard(boardRows: List<String>) {
        val grid = binding.resultsBoard
        grid.removeAllViews()
        grid.columnCount = boardRows.firstOrNull()?.length ?: 0
        grid.rowCount = boardRows.size
        if (grid.columnCount == 0 || grid.rowCount == 0) {
            return
        }
        boardRows.forEach { row ->
            row.forEach { cell ->
                val button = MaterialButton(requireContext()).apply {
                    isEnabled = false
                    text = cell.takeIf { it != ' ' }?.toString() ?: ""
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }
                }
                grid.addView(button)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
