package com.mindfulplant.cbt.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import com.mindfulplant.cbt.data.repository.RecordRepository
import com.mindfulplant.cbt.data.repository.SyncResult
import com.mindfulplant.cbt.util.UiText
import com.mindfulplant.cbt.util.uiText
import kotlinx.coroutines.launch

class MoodHistoryViewModel(
    private val recordRepository: RecordRepository,
    private val userId: String
) : ViewModel() {

    val records: LiveData<List<ThoughtRecordEntity>> =
        recordRepository.observeRecords(userId).asLiveData()

    val pendingCount: LiveData<Int> = records.map { list -> list.count { it.syncStatus == "pending" } }

    val chartValues: LiveData<List<Float>> = records.map { list ->
        list.take(7).reversed().map { it.moodAfter.toFloat() }
    }

    private val _syncMessage = MutableLiveData<UiText?>()
    val syncMessage: LiveData<UiText?> = _syncMessage

    fun syncNow() {
        viewModelScope.launch {
            try {
                when (val result = recordRepository.syncPendingRecords(userId)) {
                    is SyncResult.Success -> _syncMessage.value = when {
                        result.syncedCount > 0 && result.failedCount > 0 ->
                            uiText(R.string.sync_partial, result.syncedCount, result.failedCount)
                        result.syncedCount == 1 -> uiText(R.string.sync_done_one)
                        result.syncedCount > 0 -> uiText(R.string.sync_done_many, result.syncedCount)
                        result.failedCount > 0 -> uiText(R.string.sync_failed)
                        else -> uiText(R.string.sync_up_to_date)
                    }
                    is SyncResult.Error -> _syncMessage.value = result.message
                }
            } catch (e: Exception) {
                _syncMessage.value = uiText(R.string.sync_failed)
            }
        }
    }
}
