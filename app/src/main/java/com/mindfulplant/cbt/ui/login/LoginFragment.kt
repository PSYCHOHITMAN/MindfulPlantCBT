package com.mindfulplant.cbt.ui.login

import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
 * REST API. "Continue with Google" signs in through Firebase Authentication, then
 * exchanges the Google ID token for an API session via POST /sso.
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

    // Receives the result of the Google account picker.
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken == null) {
                    viewModel.onSsoError("Google sign-in failed. Please try again.")
                } else {
                    // Sign in to Firebase Authentication (the SSO provider), then
                    // exchange the same Google token for our own API session.
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                viewModel.ssoLogin(idToken)
                            } else {
                                Log.e("LoginFragment", "Firebase sign-in failed", task.exception)
                                viewModel.onSsoError("Google sign-in failed. Please try again.")
                            }
                        }
                }
            } catch (e: ApiException) {
                // 12501 = the user closed the account picker; not an error worth showing.
                if (e.statusCode != 12501) {
                    Log.e("LoginFragment", "Google sign-in failed: ${e.statusCode}", e)
                    viewModel.onSsoError("Google sign-in failed. Please try again.")
                }
            }
        }

    private fun startGoogleSignIn() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), options)
        // Sign out first so the account chooser is always shown.
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
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

        // Already signed in (e.g. reopening the app or tapping the reminder
        // notification): skip the login screen and go straight to Home.
        val app = requireActivity().application as MindfulPlantApp
        if (SessionManager(app).isLoggedIn) {
            findNavController().navigate(R.id.action_login_to_home)
            return
        }

        binding.loginButton.setOnClickListener {
            viewModel.login(
                binding.emailInput.text.toString(),
                binding.passwordInput.text.toString()
            )
        }

        binding.googleSignInButton.setOnClickListener { startGoogleSignIn() }

        binding.registerPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.loadingSpinner.visibility =
                if (state is LoginUiState.Loading) View.VISIBLE else View.GONE
            binding.loginButton.isEnabled = state !is LoginUiState.Loading
            binding.googleSignInButton.isEnabled = state !is LoginUiState.Loading
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
