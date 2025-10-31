package com.example.zazeks.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.zazeks.R
import com.example.zazeks.databinding.FragmentAuthBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.authTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedTab = if (tab.position == 0) AuthTab.LOGIN else AuthTab.REGISTER
                viewModel.selectTab(selectedTab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        binding.loginSubmitButton.setOnClickListener {
            viewModel.submitLogin(
                username = binding.loginUsernameInput.text?.toString().orEmpty(),
                password = binding.loginPasswordInput.text?.toString().orEmpty(),
            )
        }
        binding.registerSubmitButton.setOnClickListener {
            viewModel.submitRegistration(
                username = binding.registerUsernameInput.text?.toString().orEmpty(),
                password = binding.registerPasswordInput.text?.toString().orEmpty(),
                confirmation = binding.registerConfirmInput.text?.toString().orEmpty(),
            )
        }

        binding.loginUsernameInput.doAfterTextChanged {
            binding.loginUsernameInputLayout.error = null
            binding.errorText.isVisible = false
            viewModel.consumeError()
        }
        binding.loginPasswordInput.doAfterTextChanged {
            binding.loginPasswordInputLayout.error = null
            binding.errorText.isVisible = false
            viewModel.consumeError()
        }
        binding.registerUsernameInput.doAfterTextChanged {
            binding.registerUsernameInputLayout.error = null
            binding.errorText.isVisible = false
            viewModel.consumeError()
        }
        binding.registerPasswordInput.doAfterTextChanged {
            binding.registerPasswordInputLayout.error = null
            binding.errorText.isVisible = false
            viewModel.consumeError()
        }
        binding.registerConfirmInput.doAfterTextChanged {
            binding.registerConfirmInputLayout.error = null
            binding.errorText.isVisible = false
            viewModel.consumeError()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state -> renderState(state) }
                }
                launch {
                    viewModel.events.collect { event -> handleEvent(event) }
                }
            }
        }
    }

    private fun renderState(state: AuthViewState) {
        val selectedIndex = if (state.selectedTab == AuthTab.LOGIN) 0 else 1
        if (binding.authTabs.selectedTabPosition != selectedIndex) {
            binding.authTabs.getTabAt(selectedIndex)?.select()
        }
        binding.loginForm.isVisible = state.selectedTab == AuthTab.LOGIN
        binding.registerForm.isVisible = state.selectedTab == AuthTab.REGISTER

        binding.loginUsernameInputLayout.error = if (state.selectedTab == AuthTab.LOGIN) state.usernameError else null
        binding.loginPasswordInputLayout.error = if (state.selectedTab == AuthTab.LOGIN) state.passwordError else null

        binding.registerUsernameInputLayout.error = if (state.selectedTab == AuthTab.REGISTER) state.usernameError else null
        binding.registerPasswordInputLayout.error = if (state.selectedTab == AuthTab.REGISTER) state.passwordError else null
        binding.registerConfirmInputLayout.error = if (state.selectedTab == AuthTab.REGISTER) state.confirmPasswordError else null

        val hasError = !state.generalError.isNullOrBlank()
        binding.errorText.isVisible = hasError
        binding.errorText.text = state.generalError ?: ""

        binding.loadingIndicator.isVisible = state.isLoading
        binding.loginSubmitButton.isEnabled = !state.isLoading
        binding.registerSubmitButton.isEnabled = !state.isLoading
    }

    private fun handleEvent(event: AuthEvent) {
        when (event) {
            AuthEvent.NavigateToMain -> navigateToMainMenu()
            is AuthEvent.ShowMessage -> Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun navigateToMainMenu() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.authFragment, true)
            .build()
        findNavController().navigate(R.id.action_authFragment_to_mainMenuFragment, null, navOptions)
    }
}
