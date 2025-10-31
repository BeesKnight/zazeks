package com.example.zazeks.ui.profile
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.databinding.FragmentProfileBinding
import com.example.zazeks.ui.common.loadBase64Image
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            handleSelectedImage(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.profileToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.profileRetryButton.setOnClickListener { viewModel.loadProfile() }
        binding.profileEditButton.setOnClickListener { viewModel.startEditing() }
        binding.profileCancelButton.setOnClickListener { viewModel.cancelEditing() }
        binding.profileSaveButton.setOnClickListener { viewModel.saveChanges() }
        binding.profileChangePhotoButton.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.profileUsernameInput.doAfterTextChanged { editable ->
            if (binding.profileUsernameInput.hasFocus()) {
                viewModel.onUsernameChanged(editable?.toString().orEmpty())
            }
        }

        viewModel.state.observe(viewLifecycleOwner, ::renderState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest(::handleEvent)
        }
    }

    private fun renderState(state: ProfileViewState) {
        binding.profileLoading.isVisible = state.isLoading
        val hasError = state.loadError != null
        binding.profileErrorContainer.isVisible = hasError && !state.isLoading
        binding.profileScroll.isVisible = !state.isLoading && !hasError

        when (val error = state.loadError) {
            is ProfileViewModel.ProfileError.Load -> {
                binding.profileErrorText.text = error.message ?: getString(R.string.profile_error_load)
                binding.profileRetryButton.isVisible = true
            }
            ProfileViewModel.ProfileError.InvalidUser -> {
                binding.profileErrorText.text = getString(R.string.profile_invalid_user)
                binding.profileRetryButton.isVisible = false
            }
            null -> Unit
        }

        val placeholder = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces)
        val avatarSource = state.pendingAvatarBase64 ?: state.profile?.photo
        binding.profileAvatar.loadBase64Image(avatarSource, placeholder)

        binding.profileUsername.text = state.profile?.username.orEmpty()

        val isEditing = state.isEditing
        binding.profileUsername.isVisible = !isEditing
        binding.profileUsernameInputLayout.isVisible = isEditing
        binding.profileChangePhotoButton.isVisible = isEditing
        binding.profileSaveButton.isVisible = isEditing
        binding.profileCancelButton.isVisible = isEditing
        binding.profileEditButton.isVisible = !isEditing
        binding.profileEditButton.isEnabled = state.profile != null
        binding.profileSaveButton.isEnabled = !state.isSaving
        binding.profileChangePhotoButton.isEnabled = !state.isSaving
        binding.profileCancelButton.isEnabled = !state.isSaving

        if (isEditing) {
            val currentText = binding.profileUsernameInput.text?.toString().orEmpty()
            if (currentText != state.usernameInput) {
                binding.profileUsernameInput.setText(state.usernameInput)
                binding.profileUsernameInput.setSelection(state.usernameInput.length)
            }
        } else {
            binding.profileUsernameInput.setText("")
        }

        val profile = state.profile
        val offlineWins = profile?.wins ?: 0
        val offlineGames = profile?.gamesPlayed ?: 0
        val onlineWins = profile?.onlineWins ?: 0
        val onlineGames = profile?.onlineGames ?: 0

        binding.profileOfflineWins.text = getString(R.string.profile_stat_wins_format, offlineWins)
        binding.profileOfflineGames.text = getString(R.string.profile_stat_games_format, offlineGames)
        binding.profileOnlineWins.text = getString(R.string.profile_stat_wins_format, onlineWins)
        binding.profileOnlineGames.text = getString(R.string.profile_stat_games_format, onlineGames)

        val winRate = profile?.winRate
        if (winRate != null) {
            val percentage = (winRate * 100).coerceIn(0f, 100f)
            binding.profileWinrateValue.text = getString(R.string.profile_winrate_value, percentage)
            binding.profileWinrateProgress.isVisible = true
            binding.profileWinrateProgress.setProgressCompat(percentage.roundToInt(), false)
        } else {
            binding.profileWinrateValue.text = getString(R.string.profile_winrate_unknown)
            binding.profileWinrateProgress.isVisible = true
            binding.profileWinrateProgress.setProgressCompat(0, false)
        }
    }

    private fun handleEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.ShowMessage -> {
                val message = event.message?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.profile_update_success)
                showToast(message)
            }
            is ProfileEvent.ShowError -> {
                val message = event.message
                    .takeUnless { it.isBlank() || it.startsWith("HTTP", ignoreCase = true) }
                    ?: getString(R.string.profile_update_error)
                showToast(message)
            }
            is ProfileEvent.ProfileUpdated -> {
                setFragmentResult(
                    REQUEST_KEY_PROFILE_UPDATED,
                    bundleOf(RESULT_KEY_PROFILE_UPDATED to true)
                )
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        val context = requireContext()
        val result = runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri)?.lowercase(Locale.ROOT)
                ?: throw IllegalArgumentException(getString(R.string.profile_avatar_error_type))
            if (mimeType != MIME_PNG && mimeType != MIME_JPEG && mimeType != MIME_JPG) {
                throw IllegalArgumentException(getString(R.string.profile_avatar_error_type))
            }
            val bytes = resolver.openInputStream(uri)?.use { input -> input.readBytes() }
                ?: throw IOException("Unable to read image")
            if (bytes.size > MAX_IMAGE_SIZE_BYTES) {
                throw IllegalArgumentException(getString(R.string.profile_avatar_error_size))
            }
            val normalizedMime = if (mimeType == MIME_JPG) MIME_JPEG else mimeType
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:$normalizedMime;base64,$base64"
        }
        result.onSuccess { base64 ->
            viewModel.onAvatarSelected(base64)
        }.onFailure { error ->
            val message = when (error) {
                is IllegalArgumentException -> error.message
                else -> getString(R.string.profile_update_error)
            }
            showToast(message ?: getString(R.string.profile_update_error))
        }
    }

    private fun showToast(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_USER_ID = "userId"
        const val REQUEST_KEY_PROFILE_UPDATED = "profile_updated"
        const val RESULT_KEY_PROFILE_UPDATED = "profile_updated_result"
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
        private const val MIME_PNG = "image/png"
        private const val MIME_JPEG = "image/jpeg"
        private const val MIME_JPG = "image/jpg"
    }
}
