package com.example.utils

import java.sql.Timestamp

data class StepCounterData(override val timestamp: Long, val steps: Float): SensorData(timestamp) {
}