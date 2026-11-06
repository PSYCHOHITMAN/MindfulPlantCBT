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
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.databinding.FragmentInsightsBinding
import com.mindfulplant.cbt.domain.InsightMessage
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
                Trend.UP -> getString(R.string.insights_trend_up)
                Trend.DOWN -> getString(R.string.insights_trend_down)
                Trend.STEADY -> getString(R.string.insights_trend_steady)
                Trend.NOT_ENOUGH_DATA -> getString(R.string.insights_trend_not_enough)
            }
            binding.messageText.text = getString(messageRes(result.message))
            val countLabel = if (result.analysedCount == 1) getString(R.string.history_record_count_one)
                else getString(R.string.history_record_count_other, result.analysedCount)
            binding.subtitleText.text = getString(R.string.insights_subtitle, countLabel)

            binding.avgMoodValue.text = result.avgMoodAfter?.let { String.format("%.1f", it) } ?: "–"
            binding.moodLiftValue.text = result.avgMoodLift?.let {
                (if (it >= 0) "+" else "") + String.format("%.1f", it)
            } ?: "–"
            binding.streakStatValue.text = result.streak.toString()

            binding.distortionText.text = if (result.mostLoggedDistortion != null) {
                getString(R.string.insights_distortion_count, result.mostLoggedDistortion, result.mostLoggedDistortionCount)
            } else {
                getString(R.string.insights_no_pattern)
            }
            val tipRes = tipRes(result.mostLoggedDistortion)
            if (tipRes != null) {
                binding.distortionTipText.visibility = View.VISIBLE
                binding.distortionTipText.text = getString(tipRes)
            } else {
                binding.distortionTipText.visibility = View.GONE
            }
        }
    }

    private fun messageRes(message: InsightMessage): Int = when (message) {
        InsightMessage.NOT_ENOUGH_DATA -> R.string.insights_msg_not_enough
        InsightMessage.BUILDING_HABIT -> R.string.insights_msg_habit
        InsightMessage.IMPROVED -> R.string.insights_msg_improved
        InsightMessage.DECLINE -> R.string.insights_msg_decline
        InsightMessage.PERSISTENT_DECLINE -> R.string.insights_msg_persistent_decline
        InsightMessage.STEADY -> R.string.insights_msg_steady
    }

    /** Distortion names are stored in English, so match on them to pick the translated tip. */
    private fun tipRes(distortion: String?): Int? = when (distortion) {
        "Catastrophising" -> R.string.tip_catastrophising
        "All-or-nothing thinking" -> R.string.tip_all_or_nothing
        "Mind-reading" -> R.string.tip_mind_reading
        "Overgeneralising" -> R.string.tip_overgeneralising
        "Emotional reasoning" -> R.string.tip_emotional_reasoning
        "Should statements" -> R.string.tip_should_statements
        "Personalising" -> R.string.tip_personalising
        else -> null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
