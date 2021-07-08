package com.example.utils

import java.sql.Timestamp

data class HeartRateData(override val timestamp: Long, val hr: Float): SensorData(timestamp) {
}