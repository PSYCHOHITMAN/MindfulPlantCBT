package com.mindfulplant.cbt.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Patterns
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.data.repository.AuthRepository
import com.mindfulplant.cbt.data.repository.AuthResult
import com.mindfulplant.cbt.util.UiText
import com.mindfulplant.cbt.util.uiText
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userId: String) : LoginUiState()
    data class Error(val message: UiText) : LoginUiState()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error(uiText(R.string.err_login_fields))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = LoginUiState.Error(uiText(R.string.err_invalid_email))
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.login(email.trim(), password)) {
                is AuthResult.Success -> _uiState.value = LoginUiState.Success(result.userId)
                is AuthResult.Error -> _uiState.value = LoginUiState.Error(result.message)
            }
        }
    }

    /** Called after Google + Firebase sign-in succeed: swap the Google token for our API session. */
    fun ssoLogin(idToken: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.ssoLogin(idToken)) {
                is AuthResult.Success -> _uiState.value = LoginUiState.Success(result.userId)
                is AuthResult.Error -> _uiState.value = LoginUiState.Error(result.message)
            }
        }
    }

    fun onSsoError(message: UiText) {
        _uiState.value = LoginUiState.Error(message)
    }
}
