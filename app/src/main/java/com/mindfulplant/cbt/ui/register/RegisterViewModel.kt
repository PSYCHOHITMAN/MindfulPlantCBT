package com.mindfulplant.cbt.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Patterns
import com.mindfulplant.cbt.data.repository.AuthRepository
import com.mindfulplant.cbt.data.repository.AuthResult
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val userId: String) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<RegisterUiState>(RegisterUiState.Idle)
    val uiState: LiveData<RegisterUiState> = _uiState

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState.Error("Please fill in all fields.")
            return
        }
        if (fullName.trim().length > 120) {
            _uiState.value = RegisterUiState.Error("Full name must be 120 characters or fewer.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = RegisterUiState.Error("Please enter a valid email address.")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = RegisterUiState.Error("Passwords do not match.")
            return
        }
        if (password.length < 8) {
            _uiState.value = RegisterUiState.Error("Password must be at least 8 characters.")
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
