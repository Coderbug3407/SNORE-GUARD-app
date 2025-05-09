// kotlin+java/com.example.snoreguard/data/remote/ApiService.kt
package com.example.snoreguard.data.remote

import com.example.snoreguard.data.model.AHIResponse
import com.example.snoreguard.data.model.SnoreDataResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("snoring_data")
    suspend fun getSnoreData(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): SnoreDataResponse

    @GET("ahi")
    suspend fun getAHIData(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): AHIResponse
}
