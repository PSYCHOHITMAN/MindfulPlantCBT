package com.mindfulplant.cbt.ui.register

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

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val userId: String) : RegisterUiState()
    data class Error(val message: UiText) : RegisterUiState()
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<RegisterUiState>(RegisterUiState.Idle)
    val uiState: LiveData<RegisterUiState> = _uiState

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState.Error(uiText(R.string.err_fill_all))
            return
        }
        if (fullName.trim().length > 120) {
            _uiState.value = RegisterUiState.Error(uiText(R.string.err_name_too_long))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = RegisterUiState.Error(uiText(R.string.err_invalid_email))
            return
        }
        if (password != confirmPassword) {
            _uiState.value = RegisterUiState.Error(uiText(R.string.err_passwords_mismatch))
            return
        }
        if (password.length < 8) {
            _uiState.value = RegisterUiState.Error(uiText(R.string.err_password_short))
            return
        }

        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.register(fullName.trim(), email.trim(), password)) {
                is AuthResult.Success -> _uiState.value = RegisterUiState.Success(result.userId)
                is AuthResult.Error -> _uiState.value = RegisterUiState.Error(result.message)
            }
        }
    }
}
