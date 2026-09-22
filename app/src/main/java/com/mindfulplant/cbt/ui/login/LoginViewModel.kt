package com.mindfulplant.cbt.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Patterns
import com.mindfulplant.cbt.data.repository.AuthRepository
import com.mindfulplant.cbt.data.repository.AuthResult
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userId: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your email and password.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = LoginUiState.Error("Please enter a valid email address.")
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

    /** Called after Firebase Authentication's Google Sign-In flow succeeds. */
    fun onSsoSuccess(userId: String) {
        _uiState.value = LoginUiState.Success(userId)
    }

    fun onSsoError(message: String) {
        _uiState.value = LoginUiState.Error(message)
    }
}
