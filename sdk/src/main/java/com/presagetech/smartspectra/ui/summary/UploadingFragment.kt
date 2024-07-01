package com.presagetech.smartspectra.ui.summary

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.presagetech.smartspectra.R
import com.presagetech.smartspectra.ui.SmartSpectraActivity
import com.presagetech.smartspectra.ui.viewmodel.ScreeningViewModel
import kotlinx.coroutines.launch

class UploadingFragment : Fragment() {
    private val viewModel: ScreeningViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            (requireActivity() as SmartSpectraActivity).viewModelFactory
        )[ScreeningViewModel::class.java]
    }

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_uploading_layout, container, false).also {
            statusText = it.findViewById(R.id.text_status)
            progressBar = it.findViewById(R.id.progress_bar)
            retryButton = it.findViewById(R.id.button_retry)
        }

        retryButton.setOnClickListener {
            viewModel.viewModelScope.launch {
                viewModel.startUploadingProcess()
            }
        }

        toggleFailed(false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.viewModelScope.launch {
            viewModel.startUploadingProcess()
        }
        viewModel.uploadProgressLiveData.observe(viewLifecycleOwner, ::handleUploadingState)
        viewModel.rrstrictPulseRatePairLiveData.observe(viewLifecycleOwner) { data ->
            ScreeningViewModel.screeningResultHolder = data
            requireActivity().let {
                it.setResult(Activity.RESULT_OK)
                it.finish()
            }
        }
    }

    private fun handleUploadingState(state: UploadingState) {
        toggleFailed(state is UploadingState.Failed)
        when (state) {
            is UploadingState.Failed -> {
                statusText.setText(R.string.uploading_failed_hint)
                progressBar.isIndeterminate = false
                progressBar.progress = 0
            }
            is UploadingState.Processing -> {
                statusText.setText(R.string.processing_captured_data)
                progressBar.isIndeterminate = true
            }
            is UploadingState.Uploading -> {
                statusText.setText(R.string.uploading_captured_data)
                progressBar.isIndeterminate = false
                progressBar.progress = (state.progress * 100).toInt()
            }
        }
    }

    private fun toggleFailed(failed: Boolean) {
        progressBar.visibility = if (failed) View.GONE else View.VISIBLE
        retryButton.visibility = if (failed) View.VISIBLE else View.GONE
    }
}
