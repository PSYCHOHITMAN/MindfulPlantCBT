package com.mindfulplant.cbt.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindfulplant.cbt.MindfulPlantApp
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.data.remote.RetrofitClient
import com.mindfulplant.cbt.data.repository.RecordRepository
import com.mindfulplant.cbt.databinding.FragmentHomeBinding
import com.mindfulplant.cbt.ui.history.ThoughtRecordAdapter
import com.mindfulplant.cbt.util.ReminderPrefs
import com.mindfulplant.cbt.util.SessionManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var reminderPrefs: ReminderPrefs
    private val recentEntriesAdapter = ThoughtRecordAdapter()

    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as MindfulPlantApp
                HomeViewModel(
                    recordRepository = RecordRepository(
                        apiService = RetrofitClient.apiService,
                        recordDao = app.database.thoughtRecordDao(),
                        sessionManager = sessionManager
                    ),
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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireActivity().application as MindfulPlantApp
            val user = app.database.userDao().getUser(sessionManager.userId.orEmpty())
            val name = user?.fullName?.trim()?.substringBefore(" ")
                ?.takeIf { it.isNotBlank() }
                ?: getString(R.string.home_greeting)
            binding.greetingText.text = name
            val profileInitials = binding.root.findViewById<TextView>(R.id.profileInitials)
            profileInitials?.text = user?.fullName
                ?.trim()
                ?.split(Regex("\\s+"))
                ?.filter { it.isNotBlank() }
                ?.take(2)
                ?.joinToString("") { it.first().uppercase() }
                ?.takeIf { it.isNotBlank() }
                ?: "MP"
        }

        binding.recentEntriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recentEntriesRecyclerView.adapter = recentEntriesAdapter

        binding.newRecordButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_newThoughtRecord)
        }

        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            binding.streakStatValue.text = streak.toString()
        }

        viewModel.todayMood.observe(viewLifecycleOwner) { mood ->
            binding.todayStatValue.text = if (mood != null) "$mood/5" else "–"
        }

        viewModel.moodTrendValues.observe(viewLifecycleOwner) { values ->
            binding.moodChart.setData(values)
        }

        viewModel.recentEntries.observe(viewLifecycleOwner) { entries ->
            recentEntriesAdapter.submitList(entries)
            binding.emptyStateText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            binding.recentEntriesRecyclerView.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        binding.reminderStatValue.text = reminderPrefs.formatted()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
