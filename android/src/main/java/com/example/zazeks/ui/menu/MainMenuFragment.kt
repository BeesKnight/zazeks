package com.example.zazeks.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.core.gestures.formatGesture
import com.example.zazeks.data.auth.AuthRepository
import com.example.zazeks.databinding.FragmentMainMenuBinding
import com.example.zazeks.databinding.ItemMainMenuResultBinding
import com.example.zazeks.domain.results.ResultMode
import com.example.zazeks.domain.results.RoundResult
import com.example.zazeks.ui.common.loadBase64Image
import com.example.zazeks.ui.profile.ProfileFragment
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainMenuFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainMenuViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    private val timestampFormatter by lazy {
        DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.playOfflineButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_mainMenuFragment_to_offlineMatchFragment,
                Bundle().apply { putBoolean(ARG_START_NEW_GAME, true) }
            )
        }
        binding.playOnlineButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_onlineMatchFragment)
        }
        binding.resumeOfflineButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_mainMenuFragment_to_offlineMatchFragment,
                Bundle().apply { putBoolean(ARG_RESUME_GAME, true) }
            )
        }
        binding.profileCard.setOnClickListener { openProfile() }
        binding.profileMenuButton.setOnClickListener { openProfile() }
        binding.resultsLink.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_resultsFragment)
        }
        binding.logoutButton.setOnClickListener {
            binding.logoutButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    authRepository.logout()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                    findNavController().navigate(R.id.authFragment, null, navOptions)
                } finally {
                    binding.logoutButton.isEnabled = true
                }
            }
        }

        viewModel.state.observe(viewLifecycleOwner, ::renderState)
        parentFragmentManager.setFragmentResultListener(
            ProfileFragment.REQUEST_KEY_PROFILE_UPDATED,
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.refresh()
        }
    }

    private fun renderState(state: MainMenuViewState) {
        binding.profileName.text = formatProfileName(state)
        binding.profileSubtitle.text = formatProfileSubtitle(state)
        binding.profileAvatar.contentDescription = binding.profileName.text
        val placeholder = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces)
        binding.profileAvatar.loadBase64Image(state.profile?.photo, placeholder)
        val hasProfile = state.profileUserId != null
        binding.profileCard.isEnabled = hasProfile
        binding.profileCard.isClickable = hasProfile
        binding.profileCard.alpha = if (hasProfile) 1f else 0.6f
        binding.profileMenuButton.isEnabled = hasProfile
        binding.profileMenuButton.alpha = if (hasProfile) 1f else 0.5f

        binding.resumeOfflineButton.isVisible = state.canResume
        binding.resumeOfflineButton.isEnabled = state.canResume

        renderOfflineSummary(state.lastOffline)
        renderTopResults(state)
    }

    private fun renderOfflineSummary(result: RoundResult?) {
        if (result != null) {
            binding.offlineSubtitle.text = getString(
                R.string.menu_offline_summary_format,
                formatOutcome(result.result)
            )
            binding.offlineGestureSummary.isVisible = true
            binding.offlineGestureSummary.text = getString(
                R.string.results_round_gestures_format,
                requireContext().formatGesture(result.playerGesture),
                requireContext().formatGesture(result.opponentGesture)
            )
            val hasScore = result.playerScore != null && result.opponentScore != null
            binding.offlineScore.isVisible = hasScore
            if (hasScore) {
                binding.offlineScore.text = getString(
                    R.string.menu_offline_score_format,
                    result.playerScore!!,
                    result.opponentScore!!
                )
            }
        } else {
            binding.offlineSubtitle.text = getString(R.string.menu_offline_summary_placeholder)
            binding.offlineGestureSummary.isVisible = false
            binding.offlineScore.isVisible = false
        }
    }

    private fun renderTopResults(state: MainMenuViewState) {
        binding.topResultsProgress.isVisible = state.isLoading
        binding.topResultsError.isVisible = state.errorMessageRes != null
        binding.topResultsError.text = state.errorMessageRes?.let { getString(it) }

        binding.topResultsContainer.removeAllViews()
        state.topResults.forEach { result ->
            val itemBinding = ItemMainMenuResultBinding.inflate(
                layoutInflater,
                binding.topResultsContainer,
                false
            )
            itemBinding.modeLabel.text = formatMode(result.mode)
            val outcomeLabel = formatOutcome(result.result)
            itemBinding.resultLabel.text = getString(
                R.string.results_round_outcome_format,
                outcomeLabel
            )
            itemBinding.gesturesLabel.text = getString(
                R.string.results_round_gestures_format,
                requireContext().formatGesture(result.playerGesture),
                requireContext().formatGesture(result.opponentGesture)
            )
            val hasScore = result.playerScore != null && result.opponentScore != null
            itemBinding.scoreLabel.isVisible = hasScore
            if (hasScore) {
                itemBinding.scoreLabel.text = getString(
                    R.string.menu_offline_score_format,
                    result.playerScore!!,
                    result.opponentScore!!
                )
            }
            val timestamp = formatTimestamp(result.timestamp)
            itemBinding.timestampLabel.text = timestamp?.let {
                getString(R.string.results_round_played_at_format, it)
            } ?: getString(R.string.results_round_played_at_unknown)
            binding.topResultsContainer.addView(itemBinding.root)
        }
        binding.topResultsContainer.isVisible = state.topResults.isNotEmpty()
        binding.topResultsEmpty.isVisible =
            state.topResults.isEmpty() && !state.isLoading && state.errorMessageRes == null
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatProfileName(state: MainMenuViewState): String =
        state.profile?.username
            ?: state.profileUserId?.let { getString(R.string.menu_profile_user_format, it) }
            ?: getString(R.string.menu_profile_guest)

    private fun formatProfileSubtitle(state: MainMenuViewState): String =
        if (state.profileUserId != null) {
            getString(R.string.menu_profile_description_user)
        } else {
            getString(R.string.menu_profile_description_guest)
        }

    private fun formatOutcome(code: String?): String = when (code?.lowercase()) {
        "win" -> getString(R.string.game_result_win)
        "loss" -> getString(R.string.game_result_loss)
        "draw" -> getString(R.string.game_result_draw)
        null -> getString(R.string.results_match_result_pending)
        else -> code
    }

    private fun formatMode(mode: ResultMode): String = when (mode) {
        ResultMode.OFFLINE -> getString(R.string.results_mode_offline)
        ResultMode.ONLINE -> getString(R.string.results_mode_online)
    }

    private fun formatTimestamp(instant: java.time.Instant?): String? = instant?.let {
        timestampFormatter.format(it)
    }

    private fun openProfile() {
        val userId = viewModel.state.value?.profile?.id ?: viewModel.state.value?.profileUserId
        if (userId != null) {
            val args = bundleOf(ProfileFragment.ARG_USER_ID to userId)
            findNavController().navigate(R.id.action_mainMenuFragment_to_profileFragment, args)
        } else {
            Toast.makeText(requireContext(), R.string.menu_profile_guest, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ARG_START_NEW_GAME = "start_new_game"
        const val ARG_RESUME_GAME = "resume_game"
    }
}
