package com.example.utils

import java.sql.Timestamp

data class WeatherData(override val timestamp: Long,
                       val description: String,
                       val temp: Float,
                       val feels_like: Float,
                       val temp_min: Float,
                       val temp_max: Float,
                       val pressure: Float,
                       val humidity: Float,
                       val wind_speed: Float,
                       val wind_direction: Float,
                       val clouds_density: Float): SensorData(timestamp) {
}