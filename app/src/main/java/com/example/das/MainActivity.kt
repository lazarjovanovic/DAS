package com.example.das

import Json4Kotlin_Base
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.utils.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.gson.Gson
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult
import com.samsung.android.sdk.healthdata.HealthConstants
import com.samsung.android.sdk.healthdata.HealthDataStore
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType
import io.nlopez.smartlocation.OnLocationUpdatedListener
import io.nlopez.smartlocation.SmartLocation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {
    private lateinit var context: MainActivity
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val ENVIRONMENTAL_SERVICE_UUID = UUID.randomUUID()
    private val ENABLE_BLUETOOTH_REQUEST_CODE = 1
    private val LOCATION_PERMISSION_REQUEST_CODE = 2
    private var location_service_started = false

    private val PERMISSION_CODE = 1000

    val APP_TAG = "SimpleHealth"

    private var mInstance: MainActivity? = null
    private var mStore: HealthDataStore? = null
    private var mConnError: HealthConnectionErrorResult? = null
    private var mKeySet: Set<PermissionKey>? = null
    @Volatile var coroutines_finished = false

    enum class SensorType{
        GRAVITY, ACC, LACC, GYRO, HR, STEPC, LOC, WEATHER
    }

    private val data: HashMap<SensorType, ArrayList<SensorData>> = HashMap()

    override fun onDestroy() {
        //mStore!!.disconnectService()
        super.onDestroy()
    }

    private fun getLastLocation() {
        if(!location_service_started) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            SmartLocation.with(context).location()
                //.oneFix()
                .start {
                    if (data[SensorType.LOC]?.isEmpty() == true || data[SensorType.LOC]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                        data[SensorType.LOC]?.add(
                            LocationData(
                                System.currentTimeMillis(),
                                it.longitude.toFloat(),
                                it.latitude.toFloat(),
                                it.altitude.toFloat(),
                                it.bearing,
                                it.speed
                            )
                        )
                    }
                };

            // SmartLocation.with(context).location().stop();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        context = this
        coroutines_finished = false

        mInstance = this
        mKeySet = HashSet()
        (mKeySet as HashSet<PermissionKey>).add(
            PermissionKey(
                HealthConstants.StepCount.HEALTH_DATA_TYPE,
                PermissionType.READ
            )
        )

        val mgr = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensors: List<Sensor> = mgr.getSensorList(Sensor.TYPE_ALL)

        for (sensor in sensors) {
            Log.d("Sensors", "" + sensor.getName())
        }

        if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
        ) {
            val permission = arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            requestPermissions(permission, PERMISSION_CODE)
        }

        start_button.setOnClickListener {
            enumValues<SensorType>().forEach { data[it] = ArrayList() }

            start_button.isEnabled = false;
            var finished_flag = false;

            //gravity sensor
            mgr.getDefaultSensor(Sensor.TYPE_GRAVITY).also { gravitySensor ->
                mgr.registerListener(
                    this, gravitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            //accelerometer sensor
            mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also { accelerometerSensor ->
                mgr.registerListener(
                    this, accelerometerSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            //linear accelerometer sensor
            mgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION).also { linearAccelerometerSensor ->
                mgr.registerListener(
                    this, linearAccelerometerSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            //gyroscope sensor
            mgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE).also { gyroscopeSensor ->
                mgr.registerListener(
                    this, gyroscopeSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            //hearth rate sensor
            mgr.getDefaultSensor(Sensor.TYPE_HEART_RATE).also { hearthRateSensor ->
                mgr.registerListener(
                    this, hearthRateSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }


            //step counter sensor
            mgr.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).also { stepCounterSensor ->
                mgr.registerListener(
                    this, stepCounterSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            // getLastLocation()

            val mChronometer = findViewById<Chronometer>(R.id.view_timer)
            mChronometer.base = SystemClock.elapsedRealtime()
            mChronometer.start()

            try {
                mChronometer.onChronometerTickListener = OnChronometerTickListener { chronometer ->
                    if (chronometer.text.toString().equals("00:10", ignoreCase = true)) {
                        if (!finished_flag) {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Info")
                            builder.setMessage("Exercise finished")
                            builder.show()
                            finished_flag = true;

                            val all_data = AllData()
                            // locationManager.removeUpdates(this);

                            enumValues<SensorType>().forEach {
                                when (it) {
                                    SensorType.ACC -> all_data.acc_data =
                                        (data[it] ?: arrayListOf()) as ArrayList<AccelerometerData>
                                    SensorType.STEPC -> all_data.stepc_data =
                                        (data[it] ?: arrayListOf()) as ArrayList<StepCounterData>
                                    SensorType.LACC -> all_data.lacc_data = (data[it]
                                        ?: arrayListOf()) as ArrayList<LinearAccelerometerData>
                                    SensorType.GYRO -> all_data.gyroscope_data =
                                        (data[it] ?: arrayListOf()) as ArrayList<GyroscopeData>
                                    SensorType.GRAVITY -> all_data.gravity_data =
                                        (data[it] ?: arrayListOf()) as ArrayList<GravityData>
                                    SensorType.HR -> all_data.hr_data =
                                        (data[it] ?: arrayListOf()) as ArrayList<HeartRateData>
                                    SensorType.LOC -> all_data.location_data =
                                        (data[it] ?: arrayListOf()) as ArrayList<LocationData>
                                    SensorType.WEATHER -> all_data.weather_data =
                                        (data[it] ?: arrayListOf()) as ArrayList<WeatherData>
                                }
                            }

                            SmartLocation.with(context).location().stop();

                            var loc_data = data[SensorType.LOC] as ArrayList<LocationData>

                            try {
                                for (i in 0..loc_data.size - 1) {
                                    GlobalScope.launch {
                                        suspend {
                                            Log.d(
                                                "coroutineScope",
                                                "#runs on ${Thread.currentThread().name}"
                                            )
                                            sendWeatherGet(loc_data[i].lat, loc_data[i].lon)
                                            delay(300)
                                            if (data[SensorType.WEATHER]?.size == loc_data.size) {
                                                coroutines_finished = true
                                            }
                                            withContext(Dispatchers.Main) {
                                                Log.d(
                                                    "coroutineScope",
                                                    "#runs on ${Thread.currentThread().name}"
                                                )
                                            }
                                        }.invoke()
                                    }
                                }
                            }
                            catch(e: Exception)
                            {
                                println(e)
                            }

                            while(!coroutines_finished)
                            {
                                println("Waiting for coroutines")
                            }

//                            loc_data?.forEach{it ->
//                                GlobalScope.launch {
//                                    suspend {
//                                        Log.d("coroutineScope", "#runs on ${Thread.currentThread().name}")
//                                        sendWeatherGet(it.lat, it.lon)
//                                        delay(1)
//                                        withContext(Dispatchers.Main) {
//                                            Log.d("coroutineScope", "#runs on ${Thread.currentThread().name}")
//                                        }
//                                    }.invoke()
//                                }
//                            }

                            val gson = Gson()
                            val json = gson.toJson(all_data)
                            val string = json
                            println(string)
                        }

                        println("Timer finished")
                        start_button.isEnabled = true;
                        mChronometer.stop();
                        //mChronometer.base = SystemClock.elapsedRealtime()
                    } else {
                        getLastLocation()
                    }
                }
            }
            catch (e: java.lang.Exception)
            {
                //Log.e(APP_TAG, e.toString());
                Log.e("ERROR", e.toString());
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor != null) {
            //if (sensor.name != "Acceleration Sensor" && sensor.name != "Gravity Sensor" && sensor.name != "LSM6DSL Acceleration Sensor")
            Log.d("SENSORS", "sensor ${sensor.name}, onAccuracyChanged: $accuracy")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when(event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                if (data[SensorType.GRAVITY]?.isEmpty() == true || data[SensorType.GRAVITY]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                    data[SensorType.GRAVITY]?.add(
                        GravityData(
                            System.currentTimeMillis(),
                            event.values[0],
                            event.values[1],
                            event.values[2]
                        )
                    )
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (data[SensorType.GYRO]?.isEmpty() == true || data[SensorType.GYRO]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                    data[SensorType.GYRO]?.add(
                        GyroscopeData(
                            System.currentTimeMillis(),
                            event.values[0],
                            event.values[1],
                            event.values[2]
                        )
                    )
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (data[SensorType.ACC]?.isEmpty() == true || data[SensorType.ACC]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                    data[SensorType.ACC]?.add(
                        AccelerometerData(
                            System.currentTimeMillis(),
                            event.values[0],
                            event.values[1],
                            event.values[2]
                        )
                    )
                }
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                if (data[SensorType.LACC]?.isEmpty() == true || data[SensorType.LACC]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                    data[SensorType.LACC]?.add(
                        LinearAccelerometerData(
                            System.currentTimeMillis(),
                            event.values[0],
                            event.values[1],
                            event.values[2]
                        )
                    )
                }
            }

            Sensor.TYPE_HEART_RATE -> {
                if (data[SensorType.HR]?.isEmpty() == true || data[SensorType.HR]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                    data[SensorType.HR]?.add(
                        HeartRateData(
                            System.currentTimeMillis(),
                            event.values[0]
                        )
                    )
                }
            }

            Sensor.TYPE_STEP_COUNTER -> {
                if (data[SensorType.STEPC]?.isEmpty() == true || data[SensorType.STEPC]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                    data[SensorType.STEPC]?.add(
                        StepCounterData(
                            System.currentTimeMillis(),
                            event.values[0]
                        )
                    )
                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    var x = 5;
                }
            }
        }
    }

    fun sendWeatherGet(latitude: Float, longitude: Float) {
        //val url = URL("https://api.openweathermap.org/data/2.5/weather?q=Nis,rs&appid=dfda8f8bd3b25a1f388c08c3f5d51065")
        val url = URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=dfda8f8bd3b25a1f388c08c3f5d51065")

        try {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        val w_data = Gson().fromJson(line, Json4Kotlin_Base::class.java)

                        if (data[SensorType.WEATHER]?.isEmpty() == true || data[SensorType.WEATHER]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                            data[SensorType.WEATHER]?.add(
                                WeatherData(
                                    System.currentTimeMillis(),
                                    w_data.weather[0].description,
                                    w_data.mainOpenWeather.temp.toFloat(),
                                    w_data.mainOpenWeather.feels_like.toFloat() - 273,
                                    w_data.mainOpenWeather.temp_min.toFloat() - 273,
                                    w_data.mainOpenWeather.temp_max.toFloat() - 273,
                                    w_data.mainOpenWeather.pressure.toFloat(),
                                    w_data.mainOpenWeather.humidity.toFloat(),
                                    w_data.wind.speed.toFloat(),
                                    w_data.wind.deg.toFloat(),
                                    w_data.clouds.all.toFloat()
                                )
                            )
                        }
                    }
                }
            }
        }
        catch (e: java.lang.Exception)
        {
            println(e.toString())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    // requestLocationPermission()
                    var x = 5;
                } else {
                    // startBleScan()
                    var x = 5;
                }
            }
        }
    }

    override fun onLocationChanged(p0: Location) {
        try{
            //Toast.makeText(this, "Alo", Toast.LENGTH_LONG).show()
            if (data[SensorType.LOC]?.isEmpty() == true || data[SensorType.LOC]?.last()?.timestamp ?: 0 < System.currentTimeMillis() - 1000) {
                data[SensorType.LOC]?.add(
                    LocationData(
                        System.currentTimeMillis(),
                        p0.longitude.toFloat(),
                        p0.latitude.toFloat(),
                        p0.altitude.toFloat(),
                        p0.bearing,
                        p0.speed
                    )
                )
            }
        }
        catch (e: java.lang.Exception)
        {
            Log.e(APP_TAG, e.toString());
        }
    }
}