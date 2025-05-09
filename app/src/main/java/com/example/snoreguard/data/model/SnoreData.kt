package com.example.snoreguard.data.model

import com.google.gson.annotations.SerializedName
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import java.util.*

data class Intervention(
    @SerializedName("active") val active: Boolean,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String
)

data class SnoreDataNew(
    @SerializedName("id") val id: String,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("snoringBegintime") val timestamp: String,
    @SerializedName("duration") val duration: Int, // in seconds
    @SerializedName("intensity") val intensity: Int, // 0-4 representing quiet to epic
    @SerializedName("intervention") val intervention: Intervention,
    @SerializedName("snoringEndtime") val snoringEndtime: String
)

data class SnoreDataResponse(
    @SerializedName("data") val data: List<SnoreDataNew>,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?
)

data class ProcessedSnoreData(
    val date: String,
    val totalSnoringTimeMinutes: Int,
    val totalInterventionTimeMinutes: Int,
    val interventionRate: Int,
    val snoringRate: Int,
    val sleepQuality: Int,
    val hourlyData: List<ProcessedHourlyData>,
    val quietTime: Int,
    val lightTime: Int,
    val mediumTime: Int,
    val loudTime: Int,
    val epicTime: Int
)

data class ProcessedHourlyData(
    val hour: String,
    val snoreIntensity: Int,
    val intervention: Boolean,
    val duration: Int
)