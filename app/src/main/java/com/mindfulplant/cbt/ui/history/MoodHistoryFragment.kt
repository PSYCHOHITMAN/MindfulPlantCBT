package com.mindfulplant.cbt.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindfulplant.cbt.MindfulPlantApp
import com.mindfulplant.cbt.data.remote.RetrofitClient
import com.mindfulplant.cbt.data.repository.RecordRepository
import com.mindfulplant.cbt.databinding.FragmentMoodHistoryBinding
import com.mindfulplant.cbt.util.SessionManager

class MoodHistoryFragment : Fragment() {

    private var _binding: FragmentMoodHistoryBinding? = null
    private val binding get() = _binding!!
    private val adapter = ThoughtRecordAdapter()

    private lateinit var sessionManager: SessionManager

    private val viewModel: MoodHistoryViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as MindfulPlantApp
                MoodHistoryViewModel(
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
        _binding = FragmentMoodHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recordsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recordsRecyclerView.adapter = adapter

        binding.syncNowButton.setOnClickListener { viewModel.syncNow() }

        viewModel.records.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)
            binding.emptyStateText.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            val countLabel = if (records.size == 1) "1 record" else "${records.size} records"
            binding.recordCountText.text = "Your last 7 days · $countLabel"
        }

        viewModel.chartValues.observe(viewLifecycleOwner) { values ->
            binding.moodChart.setData(values)
        }

        viewModel.pendingCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.pendingBanner.visibility = View.VISIBLE
                binding.pendingCountText.text = if (count == 1) "1 entry pending sync" else "$count entries pending sync"
            } else {
                binding.pendingBanner.visibility = View.GONE
            }
        }

        viewModel.syncMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
