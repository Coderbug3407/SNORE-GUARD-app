package com.example.snoreguard.ui.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snoreguard.data.model.ProcessedSnoreData
import com.example.snoreguard.data.repository.SnoreRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportViewModel(private val repository: SnoreRepository) : ViewModel() {

    private val _snoreData = MutableLiveData<ProcessedSnoreData?>()
    val snoreData: LiveData<ProcessedSnoreData?> = _snoreData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedDate = MutableLiveData<Date>()
    val selectedDate: LiveData<Date> = _selectedDate

    private val dateFormatter = SimpleDateFormat("MMMM dd", Locale.US)

    init {
        _selectedDate.value = Calendar.getInstance().time
        fetchSnoreData()
    }

    fun fetchSnoreData(date: Date? = _selectedDate.value) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                repository.getSnoreData(date).fold(
                    onSuccess = { data ->
                        _snoreData.value = data
                        _isLoading.value = false
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "Unknown error occurred"
                        _isLoading.value = false
                        _snoreData.value = null
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _isLoading.value = false
                _snoreData.value = null
            }
        }
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
        fetchSnoreData(date)
    }

    fun getFormattedDate(): String {
        return _selectedDate.value?.let { dateFormatter.format(it) } ?: "Today"
    }

    fun selectPreviousDay() {
        val calendar = Calendar.getInstance()
        _selectedDate.value?.let { calendar.time = it }
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        _selectedDate.value = calendar.time
        fetchSnoreData(_selectedDate.value)
    }

    fun selectNextDay() {
        val calendar = Calendar.getInstance()
        _selectedDate.value?.let { calendar.time = it }
        calendar.add(Calendar.DAY_OF_MONTH, 1)

        // Don't allow future dates
        val today = Calendar.getInstance()
        if (calendar.before(today) || calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            _selectedDate.value = calendar.time
            fetchSnoreData(_selectedDate.value)
        }
    }
}