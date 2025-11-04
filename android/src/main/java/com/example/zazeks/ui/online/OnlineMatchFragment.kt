package com.example.zazeks.ui.online

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.core.gestures.formatGesture
import com.example.zazeks.databinding.FragmentOnlineMatchBinding
import com.example.zazeks.infra.camera.CameraFrameAnalyzerFactory
import com.example.zazeks.infra.camera.CameraSession
import com.example.zazeks.infra.camera.CameraSessionManager
import com.example.zazeks.ui.common.Event
import com.example.zazeks.ui.offline.GameResultArgs
import com.example.zazeks.ui.offline.OfflineMatchFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnlineMatchFragment : Fragment() {

    private var _binding: FragmentOnlineMatchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnlineMatchViewModel by viewModels()

    @Inject lateinit var cameraSessionManager: CameraSessionManager
    @Inject lateinit var cameraFrameAnalyzerFactory: CameraFrameAnalyzerFactory

    private var cameraSession: CameraSession? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            showPermissionWarning()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnlineMatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.readyButton.setOnClickListener { viewModel.onReadyClicked() }
        binding.playAgainButton.setOnClickListener { viewModel.onPlayAgain() }
        binding.resultsButton.setOnClickListener { viewModel.onShowResults() }
        binding.errorActionButton.setOnClickListener { ensureCameraPermission() }

        viewModel.state.observe(viewLifecycleOwner, ::renderState)
        viewModel.effects.observe(viewLifecycleOwner, ::handleEffect)

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
        viewModel.onNavigateBack()
    }

    private fun ensureCameraPermission() {
        val context = requireContext()
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> startCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showPermissionWarning()
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
                    binding.playerPreview,
                    viewLifecycleOwner,
                    analyzer
                )
                binding.playerDetectionStatus.isVisible = false
            } catch (throwable: Throwable) {
                binding.playerDetectionStatus.isVisible = true
                binding.playerDetectionStatus.text = throwable.localizedMessage
                    ?: getString(R.string.offline_detection_error)
                binding.playerGestureOverlay.setDetection(null)
                binding.playerGestureOverlay.isVisible = false
            }
        }
    }

    private fun showPermissionWarning() {
        binding.playerDetectionStatus.isVisible = true
        binding.playerDetectionStatus.text = getString(R.string.offline_permission_required)
        binding.playerGestureOverlay.setDetection(null)
        binding.playerGestureOverlay.isVisible = false
    }

    private fun renderState(state: OnlineMatchViewState) {
        binding.loadingIndicator.isVisible = state is OnlineMatchViewState.Loading
        binding.errorContainer.isVisible = state is OnlineMatchViewState.Error
        binding.contentContainer.isVisible = state is OnlineMatchViewState.Content

        when (state) {
            is OnlineMatchViewState.Content -> renderContent(state)
            is OnlineMatchViewState.Error -> renderError(state)
            OnlineMatchViewState.Loading -> Unit
        }
    }

    private fun renderError(state: OnlineMatchViewState.Error) {
        binding.errorTitle.text = state.title
        binding.errorMessage.text = state.message
    }

    private fun renderContent(content: OnlineMatchViewState.Content) {
        val session = content.session
        val detection = content.detection

        binding.statusLabel.text = session.statusMessage ?: getString(R.string.online_status_waiting)
        binding.playerReadyStatus.text = if (session.playerReady) {
            getString(R.string.online_ready_status_ready)
        } else {
            getString(R.string.online_ready_status_waiting)
        }
        binding.opponentReadyStatus.text = if (session.opponentReady) {
            getString(R.string.online_ready_status_ready)
        } else {
            getString(R.string.online_ready_status_waiting)
        }
        binding.timerLabel.text = getString(
            R.string.online_timer_format,
            session.remainingMillis / 1000.0
        )
        binding.scoreLabel.text = getString(
            R.string.online_score_format,
            session.playerScore,
            session.opponentScore
        )
        binding.matchResultLabel.isVisible = !session.matchResult.isNullOrBlank()
        binding.matchResultLabel.text = session.matchResult?.let { result ->
            getString(R.string.online_match_result_format, formatOutcome(result))
        }
        binding.statsLabel.text = getString(
            R.string.online_stats_label,
            session.stats.wins,
            session.stats.losses,
            session.stats.draws
        )
        binding.playAgainButton.isVisible = session.showPlayAgain
        binding.blackoutOverlay.isVisible = session.blackout

        binding.playerGestureLabel.text = requireContext().formatGesture(session.playerGesture ?: detection.gesture)
        binding.opponentGestureLabel.text = requireContext().formatGesture(session.opponentGesture)

        when {
            detection.isProcessing -> {
                binding.playerDetectionStatus.isVisible = true
                binding.playerDetectionStatus.text = getString(R.string.offline_detection_processing)
            }
            !detection.errorMessage.isNullOrBlank() -> {
                binding.playerDetectionStatus.isVisible = true
                binding.playerDetectionStatus.text = detection.errorMessage
            }
            detection.hasGesture() -> binding.playerDetectionStatus.isVisible = false
            else -> {
                binding.playerDetectionStatus.isVisible = true
                binding.playerDetectionStatus.text = getString(R.string.offline_detection_hint)
            }
        }
        binding.playerGestureOverlay.setDetection(detection.boundingBox)
        binding.playerGestureOverlay.isVisible = detection.hasBoundingBox()

        val readyText = if (session.playerReady) {
            R.string.online_ready_cancel_button
        } else {
            R.string.online_ready_button
        }
        binding.readyButton.setText(readyText)
    }

    private fun handleEffect(event: Event<OnlineMatchEffect>) {
        event.getContentIfNotHandled()?.let { effect ->
            when (effect) {
                is OnlineMatchEffect.NavigateToResults -> navigateToResults(effect.result)
                is OnlineMatchEffect.ShowToast -> Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToResults(result: GameResultArgs) {
        val args = Bundle().apply {
            putParcelable(OfflineMatchFragment.ARG_GAME_RESULT, result)
        }
        findNavController().navigate(R.id.action_onlineMatchFragment_to_resultsFragment, args)
    }

    private fun formatOutcome(code: String): String = when (code.lowercase()) {
        "win" -> getString(R.string.game_result_win)
        "loss" -> getString(R.string.game_result_loss)
        "draw" -> getString(R.string.game_result_draw)
        else -> code
    }
}
