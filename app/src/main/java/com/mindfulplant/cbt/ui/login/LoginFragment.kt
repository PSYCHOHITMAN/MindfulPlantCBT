package com.mindfulplant.cbt.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.mindfulplant.cbt.MindfulPlantApp
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.data.remote.RetrofitClient
import com.mindfulplant.cbt.data.repository.AuthRepository
import com.mindfulplant.cbt.databinding.FragmentLoginBinding
import com.mindfulplant.cbt.util.SessionManager

/**
 * Login screen. Email/password goes through AuthRepository -> the custom
 * REST API. SSO is intentionally deferred because it is marked POE-only in
 * the assessment brief; Part 2 demonstrates the working email/password flow.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as MindfulPlantApp
                val sessionManager = SessionManager(app)
                LoginViewModel(
                    AuthRepository(
                        apiService = RetrofitClient.apiService,
                        userDao = app.database.userDao(),
                        sessionManager = sessionManager
                    )
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            viewModel.login(
                binding.emailInput.text.toString(),
                binding.passwordInput.text.toString()
            )
        }

        binding.registerPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.loadingSpinner.visibility =
                if (state is LoginUiState.Loading) View.VISIBLE else View.GONE
            binding.loginButton.isEnabled = state !is LoginUiState.Loading
            binding.errorText.visibility =
                if (state is LoginUiState.Error) View.VISIBLE else View.GONE

            when (state) {
                is LoginUiState.Error -> binding.errorText.text = state.message
                is LoginUiState.Success -> {
                    findNavController().navigate(R.id.action_login_to_home)
                }
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
