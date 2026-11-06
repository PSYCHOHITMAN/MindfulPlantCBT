package com.mindfulplant.cbt.ui.thoughtrecord

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindfulplant.cbt.data.repository.RecordRepository
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.util.UiText
import com.mindfulplant.cbt.util.uiText
import kotlinx.coroutines.launch

/**
 * Common cognitive distortions offered in the distortion picker
 * (Planning and Design, Section 8.2: "Guided distortion picker").
 * Kept as a short, fixed list rather than a free-text field, so Insights
 * can reliably count how often each one is logged.
 */
val COGNITIVE_DISTORTIONS = listOf(
    "Catastrophising",
    "All-or-nothing thinking",
    "Mind-reading",
    "Overgeneralising",
    "Emotional reasoning",
    "Should statements",
    "Personalising"
)

sealed class SaveRecordUiState {
    object Idle : SaveRecordUiState()
    object Saved : SaveRecordUiState()
    data class Error(val message: UiText) : SaveRecordUiState()
}

class NewThoughtRecordViewModel(
    private val recordRepository: RecordRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableLiveData<SaveRecordUiState>(SaveRecordUiState.Idle)
    val uiState: LiveData<SaveRecordUiState> = _uiState

    fun saveRecord(
        situation: String,
        automaticThought: String,
        distortionType: String,
        balancedReframe: String,
        moodBefore: Int,
        moodAfter: Int
    ) {
        if (userId.isBlank()) {
            _uiState.value = SaveRecordUiState.Error(uiText(R.string.err_session_expired))
            return
        }
        if (situation.isBlank() || automaticThought.isBlank() ||
            distortionType.isBlank() || balancedReframe.isBlank()
        ) {
            _uiState.value = SaveRecordUiState.Error(uiText(R.string.err_fill_all_before_saving))
            return
        }
        if (moodBefore !in 1..5 || moodAfter !in 1..5) {
            _uiState.value = SaveRecordUiState.Error(uiText(R.string.err_choose_moods))
            return
        }

        viewModelScope.launch {
            try {
                recordRepository.saveRecordLocally(
                    userId = userId,
                    situation = situation.trim(),
                    automaticThought = automaticThought.trim(),
                    distortionType = distortionType.trim(),
                    balancedReframe = balancedReframe.trim(),
                    moodBefore = moodBefore,
                    moodAfter = moodAfter
                )
                _uiState.value = SaveRecordUiState.Saved
            } catch (e: Exception) {
                _uiState.value = SaveRecordUiState.Error(uiText(R.string.err_save_failed))
            }
        }
    }
}
