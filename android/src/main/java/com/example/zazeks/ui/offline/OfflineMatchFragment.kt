package com.example.zazeks.ui.offline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.databinding.FragmentOfflineMatchBinding
import com.example.zazeks.infra.camera.CameraFrameAnalyzerFactory
import com.example.zazeks.infra.camera.CameraSession
import com.example.zazeks.infra.camera.CameraSessionManager
import com.example.zazeks.ui.common.Event
import com.example.zazeks.ui.menu.MainMenuFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlin.math.max

@AndroidEntryPoint
class OfflineMatchFragment : Fragment() {

    private var _binding: FragmentOfflineMatchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OfflineMatchViewModel by viewModels()

    @Inject lateinit var cameraSessionManager: CameraSessionManager
    @Inject lateinit var cameraFrameAnalyzerFactory: CameraFrameAnalyzerFactory

    private var cameraSession: CameraSession? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            viewModel.onCameraPermissionDenied()
            showPermissionWarning()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfflineMatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.errorActionButton.setOnClickListener { viewModel.onErrorAction() }
        binding.restartButton.setOnClickListener { viewModel.onRestartRound() }
        binding.readyButton.setOnClickListener { viewModel.onReadyAction() }
        binding.quitButton.setOnClickListener { viewModel.onQuitToMenu() }

        viewModel.observeGameState().observe(viewLifecycleOwner, ::renderState)
        viewModel.effects().observe(viewLifecycleOwner, ::handleEffect)

        when {
            arguments?.getBoolean(MainMenuFragment.ARG_START_NEW_GAME) == true -> {
                viewModel.onStartNewMatch()
                arguments?.remove(MainMenuFragment.ARG_START_NEW_GAME)
            }
            arguments?.getBoolean(MainMenuFragment.ARG_RESUME_GAME) == true -> {
                viewModel.onResumeMatch()
                arguments?.remove(MainMenuFragment.ARG_RESUME_GAME)
            }
            else -> viewModel.onResumeMatch()
        }

        ensureCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraSession?.close()
        cameraSession = null
        _binding = null
    }

    private fun ensureCameraPermission() {
        val context = requireContext()
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> startCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                viewModel.onCameraPermissionDenied()
                showPermissionWarning()
            }
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val binding = _binding ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val analyzer = cameraFrameAnalyzerFactory.create { frame ->
                    viewModel.onFrameCaptured(frame)
                }
                cameraSession?.close()
                cameraSession = cameraSessionManager.bind(
                    binding.cameraPreview,
                    viewLifecycleOwner,
                    analyzer
                )
            } catch (throwable: Throwable) {
                viewModel.onCameraPermissionDenied()
                binding.playerDetectionStatus.isVisible = true
                binding.playerDetectionStatus.text = throwable.localizedMessage
                    ?: getString(R.string.offline_detection_error)
            }
        }
    }

    private fun showPermissionWarning() {
        binding.playerDetectionStatus.isVisible = true
        binding.playerDetectionStatus.text = getString(R.string.offline_permission_required)
    }

    private fun renderState(state: ViewState) {
        binding.loadingIndicator.isVisible = state is ViewState.Loading
        binding.contentContainer.isVisible = state is ViewState.Content
        binding.errorContainer.isVisible = state is ViewState.Error

        when (state) {
            is ViewState.Content -> renderContent(state)
            is ViewState.Error -> renderError(state)
            ViewState.Loading -> Unit
        }
    }

    private fun renderContent(content: ViewState.Content) {
        val session = content.session
        val detection = content.detection

        binding.roundLabel.text = getString(R.string.offline_round_format, session.round)
        binding.timerLabel.text = getString(R.string.offline_timer_format, max(0.0, session.remainingSeconds))
        binding.scoreLabel.text = getString(R.string.offline_score_format, session.playerScore, session.opponentScore)

        val gestureForDisplay = when {
            session.playerGesture != null -> session.playerGesture
            detection.hasGesture() -> detection.gesture
            else -> null
        }
        binding.playerGestureLabel.text = formatGesture(gestureForDisplay)

        val detectionErrorText = detection.errorMessage ?: detection.errorMessageRes?.let { getString(it) }
        val detectionStatus = when {
            session.isRoundCompleted || session.isMatchCompleted -> null
            detection.isProcessing -> getString(R.string.offline_detection_processing)
            detectionErrorText != null -> detectionErrorText
            detection.hasGesture() -> null
            else -> getString(R.string.offline_detection_hint)
        }
        binding.playerDetectionStatus.isVisible = !detectionStatus.isNullOrBlank()
        binding.playerDetectionStatus.text = detectionStatus.orEmpty()

        binding.opponentGestureLabel.text = formatGesture(session.opponentGesture)

        val roundResult = session.roundResult
        binding.roundResultLabel.isVisible = !roundResult.isNullOrBlank()
        if (!roundResult.isNullOrBlank()) {
            binding.roundResultLabel.text = getString(
                R.string.offline_round_result_format,
                formatOutcome(roundResult)
            )
        }

        binding.matchResultLabel.isVisible = session.isMatchCompleted
        if (session.isMatchCompleted) {
            val resultText = session.matchResult?.let { formatOutcome(it) } ?: getString(R.string.game_match_in_progress)
            binding.matchResultLabel.text = getString(R.string.offline_match_result_format, resultText)
        }

        binding.readyButton.isEnabled = when {
            session.isMatchCompleted -> true
            session.isRoundCompleted -> true
            else -> detection.hasGesture() && !detection.isProcessing
        }
        binding.restartButton.isEnabled = !session.isMatchCompleted

        binding.errorContainer.isVisible = false
    }

    private fun renderError(state: ViewState.Error) {
        binding.errorTitle.text = state.title
        binding.errorMessage.text = state.message
        binding.errorActionButton.isVisible = state.action != null
        binding.errorActionButton.isEnabled = state.action != null
        binding.errorActionButton.text = state.action?.label ?: getString(R.string.error_retry)
    }

    private fun handleEffect(event: Event<OfflineMatchEffect>) {
        event.getContentIfNotHandled()?.let { effect ->
            when (effect) {
                is OfflineMatchEffect.NavigateToResults -> navigateToResults(effect.result)
                OfflineMatchEffect.NavigateToMenu -> findNavController().popBackStack(R.id.mainMenuFragment, false)
            }
        }
    }

    private fun navigateToResults(result: GameResultArgs) {
        val bundle = Bundle().apply {
            putParcelable(ARG_GAME_RESULT, result)
        }
        findNavController().navigate(R.id.action_offlineMatchFragment_to_resultsFragment, bundle)
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

    companion object {
        const val ARG_GAME_RESULT = "offline_game_result"
    }
}
