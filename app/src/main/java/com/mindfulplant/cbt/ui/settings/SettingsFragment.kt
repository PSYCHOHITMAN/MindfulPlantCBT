package com.mindfulplant.cbt.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.mindfulplant.cbt.MindfulPlantApp
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.databinding.FragmentSettingsBinding
import com.mindfulplant.cbt.util.ReminderPrefs
import com.mindfulplant.cbt.util.SessionManager
import java.util.Calendar

/**
 * Settings screen. Covers the core requirements from Planning and Design
 * Section 8.1: language and reminder time. SSO, push notifications,
 * offline sync, and multi-language support are POE-only in the brief; the
 * language/reminder controls remain useful prototype settings, but no
 * non-functional change-password control is presented in Part 2.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var reminderPrefs: ReminderPrefs

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as MindfulPlantApp
                SettingsViewModel(
                    userDao = app.database.userDao(),
                    userId = sessionManager.userId.orEmpty()
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        sessionManager = SessionManager(requireActivity().application)
        reminderPrefs = ReminderPrefs(requireActivity().application)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var isApplyingSavedSelection = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageNames = SUPPORTED_LANGUAGES.map { it.first }
        binding.languageSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, languageNames
        )
        binding.languageSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isApplyingSavedSelection) {
                    // This selection came from us restoring the saved preference,
                    // not from the user tapping a new option - skip re-applying it.
                    isApplyingSavedSelection = false
                    return
                }
                val languageCode = SUPPORTED_LANGUAGES[position].second
                viewModel.updateLanguage(languageCode)
                applyAppLocale(languageCode)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        })

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.root.findViewById<TextView>(R.id.profileNameText)?.text = user.fullName
                binding.root.findViewById<TextView>(R.id.profileEmailText)?.text = user.email
                binding.root.findViewById<TextView>(R.id.profileInitials)?.text = user.fullName
                    .trim()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifBlank { "MP" }
                val index = SUPPORTED_LANGUAGES.indexOfFirst { it.second == user.preferredLanguage }
                if (index >= 0 && binding.languageSpinner.selectedItemPosition != index) {
                    isApplyingSavedSelection = true
                    binding.languageSpinner.setSelection(index)
                }
            }
        }

        binding.reminderTimeButton.text = reminderPrefs.formatted()

        binding.reminderTimeButton.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    reminderPrefs.set(hour, minute)
                    binding.reminderTimeButton.text = reminderPrefs.formatted()
                    // TODO: reschedule the AlarmManager/WorkManager reminder
                    // for this new time (Planning and Design, Section 8.1).
                },
                reminderPrefs.hour,
                reminderPrefs.minute,
                true
            ).show()
        }

        binding.logoutButton.setOnClickListener {
            sessionManager.clear()
            findNavController().navigate(R.id.action_settings_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Applies the chosen language immediately, app-wide, using AndroidX's
     * per-app language support (AppCompatDelegate). This is what actually
     * makes "Multi-language support" demonstrable: AppCompat recreates the
     * activity with the new locale automatically, and remembers the choice
     * across app restarts without any extra code on our part.
     */
    private fun applyAppLocale(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
