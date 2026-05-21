package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val current_weather: CurrentWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val temperature: Double,
    val weathercode: Int,
    val windspeed: Double?,
    val time: String?
)
