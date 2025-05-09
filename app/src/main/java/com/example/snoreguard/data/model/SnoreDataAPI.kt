package com.example.snoreguard.data.model

import com.google.gson.annotations.SerializedName

data class AHIData(
    @SerializedName("ahi") val ahi: Float,
    @SerializedName("apnea_events") val apneaEvents: Int,
    @SerializedName("hypopnea_events") val hypopneaEvents: Int,
    @SerializedName("sleep_hours") val sleepHours: Float,
    @SerializedName("severity") val severity: String,
    @SerializedName("date") val date: String
)

data class AHIResponse(
    @SerializedName("data") val data: List<AHIData>,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?
)