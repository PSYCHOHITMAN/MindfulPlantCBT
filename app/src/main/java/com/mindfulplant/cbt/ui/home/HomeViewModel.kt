package com.mindfulplant.cbt.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import com.mindfulplant.cbt.data.repository.RecordRepository
import com.mindfulplant.cbt.domain.StreakCalculator

class HomeViewModel(
    recordRepository: RecordRepository,
    userId: String
) : ViewModel() {

    /** Newest-first, as returned by the DAO. */
    private val records: LiveData<List<ThoughtRecordEntity>> =
        recordRepository.observeRecords(userId).asLiveData()

    val mostRecentEntry: LiveData<ThoughtRecordEntity?> = records.map { it.firstOrNull() }

    /** Most recent two entries, for the "Recent entries" section. */
    val recentEntries: LiveData<List<ThoughtRecordEntity>> = records.map { it.take(2) }

    /** Simple streak: consecutive days (including today) with at least one entry. */
    val currentStreak: LiveData<Int> = records.map { StreakCalculator.calculate(it) }

    /** Today's most recent mood-after score, if the user has already logged today. */
    val todayMood: LiveData<Int?> = records.map { list ->
        val dayInMillis = 86_400_000L
        val today = System.currentTimeMillis() / dayInMillis
        list.firstOrNull { it.createdAt / dayInMillis == today }?.moodAfter
    }

    /** Up to the last 7 entries' mood-after scores, oldest to newest, for the mini trend chart. */
    val moodTrendValues: LiveData<List<Float>> = records.map { list ->
        list.take(7).reversed().map { it.moodAfter.toFloat() }
    }
}
