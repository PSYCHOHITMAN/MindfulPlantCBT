package com.mindfulplant.cbt.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mindfulplant.cbt.data.local.dao.UserDao
import kotlinx.coroutines.launch

/** en / zu / st, matching the values-zu and values-st resource folders. */
val SUPPORTED_LANGUAGES = listOf("English" to "en", "isiZulu" to "zu", "Sesotho" to "st")

class SettingsViewModel(
    private val userDao: UserDao,
    private val userId: String
) : ViewModel() {

    val user = userDao.observeUser(userId).asLiveData()

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch { userDao.updateLanguage(userId, languageCode) }
    }
}
