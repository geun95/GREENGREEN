package com.example.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double = 37.5665,
        @Query("longitude") longitude: Double = 126.9780,
        @Query("current_weather") currentWeather: Boolean = true
    ): WeatherResponse
}
