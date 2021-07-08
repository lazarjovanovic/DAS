package com.example.utils

import java.sql.Timestamp

data class LinearAccelerometerData(override val timestamp: Long, val x: Float, val y: Float, val z: Float): SensorData(timestamp) {
}