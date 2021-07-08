package com.example.utils
import kotlin.collections.ArrayList

import com.google.gson.annotations.SerializedName

data class AllData(
    @SerializedName("acc_data")
    var acc_data:ArrayList<AccelerometerData> = arrayListOf(),
    @SerializedName("gravity_data")
    var gravity_data:ArrayList<GravityData> = arrayListOf(),
    @SerializedName("gyroscope_data")
    var gyroscope_data:ArrayList<GyroscopeData> = arrayListOf(),
    @SerializedName("hr_data")
    var hr_data:ArrayList<HeartRateData> = arrayListOf(),
    @SerializedName("lacc_data")
    var lacc_data:ArrayList<LinearAccelerometerData> = arrayListOf(),
    @SerializedName("stepc_data")
    var stepc_data:ArrayList<StepCounterData> = arrayListOf()) {



}