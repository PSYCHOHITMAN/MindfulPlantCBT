package com.mindfulplant.cbt.ui.thoughtrecord

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.mindfulplant.cbt.MindfulPlantApp
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.data.remote.RetrofitClient
import com.mindfulplant.cbt.data.repository.RecordRepository
import com.mindfulplant.cbt.databinding.FragmentNewThoughtRecordBinding
import com.mindfulplant.cbt.util.SessionManager

class NewThoughtRecordFragment : Fragment() {

    private var _binding: FragmentNewThoughtRecordBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    private val viewModel: NewThoughtRecordViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as MindfulPlantApp
                NewThoughtRecordViewModel(
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
        _binding = FragmentNewThoughtRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.distortionSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            COGNITIVE_DISTORTIONS
        )

        binding.saveButton.setOnClickListener {
            val moodBefore = when (binding.moodBeforeGroup.checkedRadioButtonId) {
                R.id.moodBefore1 -> 1
                R.id.moodBefore2 -> 2
                R.id.moodBefore3 -> 3
                R.id.moodBefore4 -> 4
                R.id.moodBefore5 -> 5
                else -> 0
            }
            val moodAfter = when (binding.moodAfterGroup.checkedRadioButtonId) {
                R.id.mood1 -> 1
                R.id.mood2 -> 2
                R.id.mood3 -> 3
                R.id.mood4 -> 4
                R.id.mood5 -> 5
                else -> 0
            }

            viewModel.saveRecord(
                situation = binding.situationInput.text.toString(),
                automaticThought = binding.thoughtInput.text.toString(),
                distortionType = binding.distortionSpinner.selectedItem as String,
                balancedReframe = binding.reframeInput.text.toString(),
                moodBefore = moodBefore,
                moodAfter = moodAfter
            )
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.errorText.visibility =
                if (state is SaveRecordUiState.Error) View.VISIBLE else View.GONE

            when (state) {
                is SaveRecordUiState.Error -> binding.errorText.text = state.message
                is SaveRecordUiState.Saved -> {
                    findNavController().navigate(R.id.action_newThoughtRecord_to_home)
                }
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
