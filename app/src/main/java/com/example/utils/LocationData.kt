package com.example.utils

import java.sql.Timestamp

data class LocationData(override val timestamp: Long, val lon: Float, val lat: Float, val alt: Float, val bearing: Float, val speed: Float): SensorData(timestamp) {
}