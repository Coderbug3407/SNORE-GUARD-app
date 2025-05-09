package com.example.snoreguard.ui.report

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.snoreguard.R
import com.example.snoreguard.data.model.ProcessedHourlyData
import com.example.snoreguard.data.model.ProcessedSnoreData
import com.example.snoreguard.data.remote.ApiClient
import com.example.snoreguard.data.repository.SnoreRepository
import com.example.snoreguard.databinding.FragmentReportBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ObjectAnimator
import com.example.snoreguard.ui.view.CircularProgressView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.US)

    private lateinit var viewModel: ReportViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = SnoreRepository(ApiClient.apiService)
        viewModel = ViewModelProvider(
            this,
            ReportViewModelFactory(repository)
        )[ReportViewModel::class.java]

        setupUI()
        observeData()
    }

    private fun setupUI() {
        binding.tvDate.text = viewModel.getFormattedDate()
        binding.ivCalendar.setOnClickListener {
            showDatePicker()
        }
        binding.waveformChart.visibility = View.GONE
        showLoadingState()
    }

    private fun observeData() {
        viewModel.snoreData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                if (isDataEmpty(data)) {
                    showNoDataState(viewModel.selectedDate.value)
                } else {
                    updateUIWithData(data)
                }
            } else {
                showNoDataState(viewModel.selectedDate.value)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoadingState()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                showNoDataState(viewModel.selectedDate.value)
            }
        }

        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = dateFormat.format(date)
        }
    }

    private fun isDataEmpty(data: ProcessedSnoreData): Boolean {
        return data.totalSnoringTimeMinutes == 0 &&
                data.totalInterventionTimeMinutes == 0 &&
                data.hourlyData.isEmpty()
    }

    private fun showNoDataState(date: Date?) {
        showLoadingState() // Reset all values to zero

        // Update the no data message with the selected date
        val dateStr = date?.let { dateFormat.format(it) } ?: "ngày này"
        binding.tvNoData.text = getString(R.string.no_data_for_date, dateStr)
        binding.tvNoData.visibility = View.VISIBLE

        binding.tvNoImprovementData.text = getString(R.string.no_data_for_date, dateStr)
        binding.tvNoImprovementData.visibility = View.VISIBLE

        // Hide charts
        binding.chartSnoring.visibility = View.GONE
        binding.waveformChart.visibility = View.GONE
        binding.chartImprovement.visibility = View.GONE
    }

    private fun updateUIWithData(data: ProcessedSnoreData) {
        // Animate progress indicators
        animateCircularProgress(binding.circularProgress, data.interventionRate)
        animateCircularProgress(binding.circularProgressSnoring, data.snoringRate)

        // Update time statistics
        updateTimeStatistics(data)

        // Update waveform chart
        updateWaveformChart(data.hourlyData)

        // Update improvement chart
        updateImprovementChart(data.hourlyData)

        // Show data containers
        binding.tvNoData.visibility = if (data.hourlyData.isEmpty()) View.VISIBLE else View.GONE
        binding.chartSnoring.visibility = View.GONE
        binding.waveformChart.visibility = if (data.hourlyData.isNotEmpty()) View.VISIBLE else View.GONE

        binding.tvNoImprovementData.visibility = if (data.hourlyData.isEmpty()) View.VISIBLE else View.GONE
        binding.chartImprovement.visibility = if (data.hourlyData.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun animateCircularProgress(view: CircularProgressView, targetProgress: Int) {
        ObjectAnimator.ofInt(0, targetProgress).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Int
                view.setProgress(animatedValue)
            }
            start()
        }
    }

    private fun updateTimeStatistics(data: ProcessedSnoreData) {
        // Format hours and minutes
        val snoreHours = data.totalSnoringTimeMinutes / 60
        val snoreMinutes = data.totalSnoringTimeMinutes % 60
        binding.tvSnoringTime.text = getString(R.string.time_format, snoreHours, snoreMinutes)

        val interventionHours = data.totalInterventionTimeMinutes / 60
        val interventionMinutes = data.totalInterventionTimeMinutes % 60
        binding.tvInterventionTime.text = getString(R.string.time_format, interventionHours, interventionMinutes)

        // Update snoring intensity times
        binding.tvQuietTime.text = getString(R.string.min_format, data.quietTime)
        binding.tvLightTime.text = getString(R.string.min_format, data.lightTime)
        binding.tvMediumTime.text = getString(R.string.min_format, data.mediumTime)
        binding.tvLoudTime.text = getString(R.string.min_format, data.loudTime)
        binding.tvEpicTime.text = getString(R.string.min_format, data.epicTime)
    }

    private fun updateWaveformChart(hourlyData: List<ProcessedHourlyData>) {
        if (hourlyData.isEmpty()) return

        // Extract intensity values and intervention flags
        val intensities = hourlyData.map { it.snoreIntensity.toFloat() }
        val interventions = hourlyData.map { it.intervention }

        // Update the waveform chart
        binding.waveformChart.setData(intensities, interventions)

        // Update time range labels
        if (hourlyData.isNotEmpty()) {
            binding.tvStartTime.text = hourlyData.first().hour
            binding.tvEndTime.text = hourlyData.last().hour
        }
    }

    private fun updateImprovementChart(hourlyData: List<ProcessedHourlyData>) {
        if (hourlyData.isEmpty()) return

        // Filter data into before and after intervention
        val beforeInterventionData = hourlyData.filter { !it.intervention }
        val afterInterventionData = hourlyData.filter { it.intervention }

        // Create entries for before intervention
        val beforeEntries = beforeInterventionData.mapIndexed { index, data ->
            Entry(index.toFloat(), data.snoreIntensity.toFloat())
        }

        // Create entries for after intervention
        val afterEntries = afterInterventionData.mapIndexed { index, data ->
            Entry(index.toFloat(), data.snoreIntensity.toFloat())
        }

        // Create datasets
        val beforeDataSet = LineDataSet(beforeEntries, "Before Intervention").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_purple)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val afterDataSet = LineDataSet(afterEntries, "After Intervention").apply {
            color = ContextCompat.getColor(requireContext(), R.color.accent_teal)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Create line data
        val lineData = LineData()
        if (beforeEntries.isNotEmpty()) lineData.addDataSet(beforeDataSet)
        if (afterEntries.isNotEmpty()) lineData.addDataSet(afterDataSet)

        // Set data to chart
        binding.chartImprovement.data = lineData
        binding.chartImprovement.invalidate()

        // Update time range labels
        if (hourlyData.isNotEmpty()) {
            binding.tvImprovementStartTime.text = hourlyData.first().hour
            binding.tvImprovementEndTime.text = hourlyData.last().hour
        }
    }

    private fun showLoadingState() {
        // Show loading indicators or placeholder views
        binding.circularProgress.setProgress(0)
        binding.circularProgressSnoring.setProgress(0)
        binding.tvSnoringTime.text = getString(R.string.time_format, 0, 0)
        binding.tvInterventionTime.text = getString(R.string.time_format, 0, 0)

        binding.tvNoData.visibility = View.VISIBLE
        binding.chartSnoring.visibility = View.GONE
        binding.waveformChart.visibility = View.GONE

        binding.tvNoImprovementData.visibility = View.VISIBLE
        binding.chartImprovement.visibility = View.GONE

        // Reset intensity times
        binding.tvQuietTime.text = getString(R.string.min_format, 0)
        binding.tvLightTime.text = getString(R.string.min_format, 0)
        binding.tvMediumTime.text = getString(R.string.min_format, 0)
        binding.tvLoudTime.text = getString(R.string.min_format, 0)
        binding.tvEpicTime.text = getString(R.string.min_format, 0)
    }

    private fun showDatePicker() {
        if (!isAdded || activity == null || activity?.isFinishing == true || activity?.isDestroyed == true) return

        // Giới hạn chỉ chọn ngày hôm nay trở về trước
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        // Lấy ngày hiện tại hoặc ngày đã chọn
        val selectedDate = viewModel.selectedDate.value?.time ?: MaterialDatePicker.todayInUtcMilliseconds()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(selectedDate)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { millis ->
            if (isAdded) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = millis
                viewModel.selectDate(calendar.time)
            }
        }

        datePicker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}