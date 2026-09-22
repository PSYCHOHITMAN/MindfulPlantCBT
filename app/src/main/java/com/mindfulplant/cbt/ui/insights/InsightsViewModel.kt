package com.mindfulplant.cbt.ui.insights

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindfulplant.cbt.data.local.dao.ThoughtRecordDao
import com.mindfulplant.cbt.domain.InsightsEngine
import com.mindfulplant.cbt.domain.InsightsResult
import kotlinx.coroutines.launch

/**
 * Loads this user's thought records and runs them through InsightsEngine
 * (Planning and Design, Section 9.1). All the actual trend-detection logic
 * lives in InsightsEngine, kept deliberately separate from this ViewModel
 * so it stays a pure, easily unit-testable function - see
 * InsightsEngineTest in the test source set.
 */
class InsightsViewModel(
    private val recordDao: ThoughtRecordDao,
    private val userId: String
) : ViewModel() {

    private val _insights = MutableLiveData<InsightsResult>()
    val insights: LiveData<InsightsResult> = _insights

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val records = recordDao.getRecordsForUser(userId) // already sorted newest-first
                _insights.value = InsightsEngine.analyse(records)
            } catch (e: Exception) {
                // A local database error should leave the screen usable.
                _insights.value = InsightsEngine.analyse(emptyList())
            }
        }
    }
}
