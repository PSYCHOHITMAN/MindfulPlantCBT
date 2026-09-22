package com.mindfulplant.cbt.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindfulplant.cbt.MindfulPlantApp
import com.mindfulplant.cbt.databinding.FragmentInsightsBinding
import com.mindfulplant.cbt.domain.Trend
import com.mindfulplant.cbt.util.SessionManager

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InsightsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as MindfulPlantApp
                val sessionManager = SessionManager(app)
                InsightsViewModel(
                    recordDao = app.database.thoughtRecordDao(),
                    userId = sessionManager.userId.orEmpty()
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Refresh every time the user visits this tab, so a newly-saved
        // thought record is reflected immediately.
        viewModel.refresh()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.insights.observe(viewLifecycleOwner) { result ->
            binding.trendLabel.text = when (result.trend) {
                Trend.UP -> "↗ Trending upward"
                Trend.DOWN -> "↘ Trending downward"
                Trend.STEADY -> "→ Steady"
                Trend.NOT_ENOUGH_DATA -> "Not enough entries yet"
            }
            binding.messageText.text = result.message
            binding.subtitleText.text = "Based on your last 7 days · ${result.analysedCount} record${if (result.analysedCount == 1) "" else "s"}"

            binding.avgMoodValue.text = result.avgMoodAfter?.let { String.format("%.1f", it) } ?: "–"
            binding.moodLiftValue.text = result.avgMoodLift?.let {
                (if (it >= 0) "+" else "") + String.format("%.1f", it)
            } ?: "–"
            binding.streakStatValue.text = result.streak.toString()

            binding.distortionText.text = if (result.mostLoggedDistortion != null) {
                "${result.mostLoggedDistortion} (${result.mostLoggedDistortionCount}x this week)"
            } else {
                "Log a few more thought records to see a pattern here."
            }
            if (result.distortionTip != null) {
                binding.distortionTipText.visibility = View.VISIBLE
                binding.distortionTipText.text = result.distortionTip
            } else {
                binding.distortionTipText.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
