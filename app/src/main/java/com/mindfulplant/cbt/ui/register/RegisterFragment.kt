package com.mindfulplant.cbt.ui.register

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
import com.mindfulplant.cbt.databinding.FragmentRegisterBinding
import com.mindfulplant.cbt.util.SessionManager

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as MindfulPlantApp
                RegisterViewModel(
                    AuthRepository(
                        apiService = RetrofitClient.apiService,
                        userDao = app.database.userDao(),
                        sessionManager = SessionManager(app)
                    )
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener {
            viewModel.register(
                fullName = binding.nameInput.text.toString(),
                email = binding.emailInput.text.toString(),
                password = binding.passwordInput.text.toString(),
                confirmPassword = binding.confirmPasswordInput.text.toString()
            )
        }

        binding.loginPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.loadingSpinner.visibility =
                if (state is RegisterUiState.Loading) View.VISIBLE else View.GONE
            binding.registerButton.isEnabled = state !is RegisterUiState.Loading
            binding.errorText.visibility =
                if (state is RegisterUiState.Error) View.VISIBLE else View.GONE

            when (state) {
                is RegisterUiState.Error -> binding.errorText.text = state.message
                is RegisterUiState.Success -> {
                    findNavController().navigate(R.id.action_register_to_home)
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
