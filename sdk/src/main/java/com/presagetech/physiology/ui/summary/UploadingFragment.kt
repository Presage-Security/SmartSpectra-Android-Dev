package com.presagetech.physiology.ui.summary

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
import com.presagetech.physiology.R
import com.presagetech.physiology.ui.SmartSpectraActivity
import com.presagetech.physiology.ui.viewmodel.ScreeningViewModel
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.uploading_fragment_layout, container, false).also {
            statusText = it.findViewById(R.id.text_status)
            progressBar = it.findViewById(R.id.progress_bar)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.viewModelScope.launch {
            viewModel.startUploadingProcess()
        }
        viewModel.uploadProgressLiveData.observe(viewLifecycleOwner) {
            if (!it.processing) {
                statusText.setText(R.string.uploading_captured_data)
                progressBar.isIndeterminate = false
                progressBar.progress = (it.uploading * 100).toInt()
            } else {
                statusText.text = getString(R.string.processing_captured_data)
                progressBar.isIndeterminate = true
            }
        }
        viewModel.rrHRAveragePairLiveData.observe(viewLifecycleOwner) {
            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(RR_RESULT_KEY, it.rrAverage)
                putExtra(HR_RESULT_KEY, it.hrAverage)
            })
            requireActivity().finish()
        }
    }

    companion object {
        const val RR_RESULT_KEY = "rr"
        const val HR_RESULT_KEY = "hr"
    }
}
