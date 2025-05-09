package com.example.snoreguard.data.repository

import android.util.Log
import com.example.snoreguard.data.model.AHIData
import com.example.snoreguard.data.model.ProcessedHourlyData
import com.example.snoreguard.data.model.ProcessedSnoreData
import com.example.snoreguard.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class SnoreRepository(private val apiService: ApiService) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val hourFormat = SimpleDateFormat("HH:mm", Locale.US)

    suspend fun getSnoreData(date: Date? = null): Result<ProcessedSnoreData> {
        return withContext(Dispatchers.IO) {
            try {
                val selectedDate = date ?: Date()
                val calendar = Calendar.getInstance()
                calendar.time = selectedDate
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startDateStr = apiDateTimeFormat.format(calendar.time)

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val endDateStr = apiDateTimeFormat.format(calendar.time)

                Log.d("SnoreRepository", "Fetching data for date range: $startDateStr to $endDateStr")

                val response = apiService.getSnoreData(startDateStr, endDateStr)

                if (response.data.isNotEmpty()) {
                    val processedData = processSnoreData(response.data, dateFormat.format(selectedDate))
                    Result.success(processedData)
                } else {
                    Result.success(createEmptyProcessedData(dateFormat.format(selectedDate)))
                }
            } catch (e: Exception) {
                Log.e("SnoreRepository", "Error fetching snore data: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun getAHIData(date: Date? = null): Result<AHIData> {
        return withContext(Dispatchers.IO) {
            try {
                val selectedDate = date ?: Date()
                val calendar = Calendar.getInstance()
                calendar.time = selectedDate
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startDateStr = apiDateTimeFormat.format(calendar.time)

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val endDateStr = apiDateTimeFormat.format(calendar.time)

                Log.d("SnoreRepository", "Fetching AHI data for date range: $startDateStr to $endDateStr")

                val response = apiService.getAHIData(startDateStr, endDateStr)

                if (response.data.isNotEmpty()) {
                    Result.success(response.data[0])
                } else {
                    Result.failure(Exception("No AHI data available for the selected date"))
                }
            } catch (e: Exception) {
                Log.e("SnoreRepository", "Error fetching AHI data: ${e.message}")
                Result.failure(e)
            }
        }
    }

    private fun processSnoreData(snoreData: List<com.example.snoreguard.data.model.SnoreDataNew>, date: String): ProcessedSnoreData {
        var totalSnoringTime = 0
        var totalInterventionTime = 0
        var quietTime = 0
        var lightTime = 0
        var mediumTime = 0
        var loudTime = 0
        var epicTime = 0

        val hourlyDataMap = mutableMapOf<String, MutableList<ProcessedHourlyData>>()

        snoreData.forEach { episode ->
            try {
                val duration = episode.duration / 60 // Convert seconds to minutes

                totalSnoringTime += duration

                // Calculate intensity times
                when (episode.intensity) {
                    0 -> quietTime += duration
                    1 -> lightTime += duration
                    2 -> mediumTime += duration
                    3 -> loudTime += duration
                    4 -> epicTime += duration
                }

                if (episode.intervention.active) {
                    totalInterventionTime += duration
                }

                // Parse timestamp to get hour
                val date = timestampFormat.parse(episode.timestamp)
                val hour = date?.let { hourFormat.format(it) } ?: "00:00"

                // Create hourly data entry
                val hourlyData = ProcessedHourlyData(
                    hour = hour,
                    snoreIntensity = (episode.intensity * 25).coerceIn(0, 100), // Scale 0-4 to 0-100
                    intervention = episode.intervention.active,
                    duration = duration
                )

                // Group by hour
                if (!hourlyDataMap.containsKey(hour)) {
                    hourlyDataMap[hour] = mutableListOf()
                }
                hourlyDataMap[hour]?.add(hourlyData)

            } catch (e: Exception) {
                Log.e("SnoreRepository", "Error processing snore data: ${e.message}")
            }
        }

        // Process hourly data to get average intensity for each hour
        val hourlyData = hourlyDataMap.entries.map { (hour, data) ->
            val avgIntensity = data.map { it.snoreIntensity }.average().roundToInt()
            val totalDuration = data.sumOf { it.duration }
            val hasIntervention = data.any { it.intervention }

            ProcessedHourlyData(
                hour = hour,
                snoreIntensity = avgIntensity,
                intervention = hasIntervention,
                duration = totalDuration
            )
        }.sortedBy { it.hour }

        // Calculate intervention effectiveness
        val interventionsCount = snoreData.count { it.intervention.active }
        val snoringRate = calculateSnoringRate(totalSnoringTime)
        val sleepQuality = (100 - snoringRate).coerceIn(0, 100)

        val interventionRate = if (interventionsCount > 0) {
            val effectiveInterventions = calculateEffectiveInterventions(snoreData)
            (effectiveInterventions.toFloat() / interventionsCount * 100).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        return ProcessedSnoreData(
            date = date,
            totalSnoringTimeMinutes = totalSnoringTime,
            totalInterventionTimeMinutes = totalInterventionTime,
            interventionRate = interventionRate,
            snoringRate = snoringRate,
            sleepQuality = sleepQuality,
            hourlyData = hourlyData,
            quietTime = quietTime,
            lightTime = lightTime,
            mediumTime = mediumTime,
            loudTime = loudTime,
            epicTime = epicTime
        )
    }

    private fun calculateSnoringRate(totalSnoringMinutes: Int): Int {
        // Assuming 8 hours of sleep
        val estimatedSleepMinutes = 8 * 60
        return ((totalSnoringMinutes.toFloat() / estimatedSleepMinutes) * 100).roundToInt()
            .coerceIn(0, 100)
    }

    private fun calculateEffectiveInterventions(snoreData: List<com.example.snoreguard.data.model.SnoreDataNew>): Int {
        var effectiveCount = 0

        // Group interventions that happened close to each other (within 30 minutes)
        val interventionEpisodes = snoreData.filter { it.intervention.active }
        val processedEpisodes = mutableSetOf<Int>()

        for (i in interventionEpisodes.indices) {
            if (i in processedEpisodes) continue

            val episode = interventionEpisodes[i]
            val episodeTime = timestampFormat.parse(episode.timestamp)?.time ?: continue

            // Check if this is effective (not followed by another snoring episode within 30 min)
            var effective = true
            for (j in i + 1 until interventionEpisodes.size) {
                if (j in processedEpisodes) continue

                val otherEpisode = interventionEpisodes[j]
                val otherTime = timestampFormat.parse(otherEpisode.timestamp)?.time ?: continue

                // If episodes are within 30 minutes, they're part of the same intervention group
                if (Math.abs(otherTime - episodeTime) <= 30 * 60 * 1000) {
                    processedEpisodes.add(j)
                    effective = false
                }
            }

            if (effective) effectiveCount++
            processedEpisodes.add(i)
        }

        return effectiveCount
    }

    private fun createEmptyProcessedData(date: String): ProcessedSnoreData {
        return ProcessedSnoreData(
            date = date,
            totalSnoringTimeMinutes = 0,
            totalInterventionTimeMinutes = 0,
            interventionRate = 0,
            snoringRate = 0,
            sleepQuality = 100,
            hourlyData = emptyList(),
            quietTime = 0,
            lightTime = 0,
            mediumTime = 0,
            loudTime = 0,
            epicTime = 0
        )
    }
}