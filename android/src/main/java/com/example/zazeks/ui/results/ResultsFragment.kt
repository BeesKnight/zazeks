package com.example.zazeks.ui.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.zazeks.R
import com.example.zazeks.databinding.FragmentResultsBinding
import com.example.zazeks.ui.offline.GameResultArgs
import com.example.zazeks.ui.offline.OfflineMatchFragment
import com.example.zazeks.ui.menu.MainMenuFragment
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ResultsViewModel by viewModels()
    private lateinit var adapter: RoundResultsAdapter

    private val timestampFormatter by lazy {
        DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    private var suppressFilterCallback = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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

        adapter = RoundResultsAdapter(timestampFormatter)
        binding.resultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsList.adapter = adapter

        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressFilterCallback) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            filterFromChipId(checkedId)?.let { viewModel.onFilterSelected(it) }
        }

        val args = arguments?.getParcelable<GameResultArgs>(OfflineMatchFragment.ARG_GAME_RESULT)
        arguments?.remove(OfflineMatchFragment.ARG_GAME_RESULT)
        viewModel.load(args)

        viewModel.state.observe(viewLifecycleOwner, ::renderState)
    }

    private fun renderState(state: ResultsViewState) {
        val chipId = chipIdForFilter(state.filter)
        if (chipId != null && binding.filterChipGroup.checkedChipId != chipId) {
            suppressFilterCallback = true
            binding.filterChipGroup.check(chipId)
            suppressFilterCallback = false
        }

        binding.resultsProgress.isVisible = state.isLoading
        binding.resultsError.isVisible = state.errorMessageRes != null
        binding.resultsError.text = state.errorMessageRes?.let { getString(it) }

        adapter.submitList(state.visibleItems)
        binding.resultsList.isVisible = state.visibleItems.isNotEmpty()

        val showEmpty = state.visibleItems.isEmpty() && !state.isLoading && state.errorMessageRes == null
        binding.resultsEmpty.isVisible = showEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun chipIdForFilter(filter: ResultFilter): Int? = when (filter) {
        ResultFilter.ALL -> R.id.filterAllChip
        ResultFilter.OFFLINE -> R.id.filterOfflineChip
        ResultFilter.ONLINE -> R.id.filterOnlineChip
    }

    private fun filterFromChipId(chipId: Int): ResultFilter? = when (chipId) {
        R.id.filterAllChip -> ResultFilter.ALL
        R.id.filterOfflineChip -> ResultFilter.OFFLINE
        R.id.filterOnlineChip -> ResultFilter.ONLINE
        else -> null
    }
}
