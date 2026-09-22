package com.mindfulplant.cbt.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import com.mindfulplant.cbt.data.repository.RecordRepository
import com.mindfulplant.cbt.data.repository.SyncResult
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

    private val _syncMessage = MutableLiveData<String?>()
    val syncMessage: LiveData<String?> = _syncMessage

    fun syncNow() {
        viewModelScope.launch {
            try {
                when (val result = recordRepository.syncPendingRecords(userId)) {
                    is SyncResult.Success -> _syncMessage.value = when {
                        result.syncedCount > 0 && result.failedCount > 0 ->
                            "Synced ${result.syncedCount}; ${result.failedCount} still pending."
                        result.syncedCount > 0 ->
                            "Synced ${result.syncedCount} entr${if (result.syncedCount == 1) "y" else "ies"}."
                        result.failedCount > 0 ->
                            "Couldn't sync yet. Check your connection and try again."
                        else -> "Everything's already up to date."
                    }
                    is SyncResult.Error -> _syncMessage.value = result.message
                }
            } catch (e: Exception) {
                _syncMessage.value = "Couldn't sync yet. Check your connection and try again."
            }
        }
    }
}
