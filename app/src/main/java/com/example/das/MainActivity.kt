package com.example.das

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.utils.*
import com.google.gson.Gson
import com.samsung.android.sdk.healthdata.*
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType
import kotlinx.android.synthetic.main.activity_main.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {
    private lateinit var context: MainActivity
    private lateinit var locationManager: LocationManager
    private val ENVIRONMENTAL_SERVICE_UUID = UUID.randomUUID()
    private val ENABLE_BLUETOOTH_REQUEST_CODE = 1
    private val LOCATION_PERMISSION_REQUEST_CODE = 2

    private val PERMISSION_CODE = 1000

    val APP_TAG = "SimpleHealth"

    private var mInstance: MainActivity? = null
    private var mStore: HealthDataStore? = null
    private var mConnError: HealthConnectionErrorResult? = null
    private var mKeySet: Set<PermissionKey>? = null

    enum class SensorType{
        GRAVITY, ACC, LACC, GYRO, HR, STEPC
    }

    private val data: HashMap<SensorType, ArrayList<SensorData>> = HashMap()

    override fun onDestroy() {
        mStore!!.disconnectService()
        super.onDestroy()
    }

    private fun readTodayStepCountData() {
//        val filter: HealthDataResolver.Filter = HealthDataResolver.Filter.and(
//            HealthDataResolver.Filter.eq(StepDailyTrend.DAY_TIME, getTodayStartUtcTime()),
//            HealthDataResolver.Filter.eq(StepDailyTrend.SOURCE_TYPE, StepDailyTrend.SOURCE_TYPE_ALL)
//        )

        val request: HealthDataResolver.ReadRequest =
            HealthDataResolver.ReadRequest.Builder() // Set the data type
                //.setDataType(StepDailyTrend.HEALTH_DATA_TYPE) // Set a filter
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                //.setFilter(filter)
                .build()

        var mResolver = HealthDataResolver(mStore, null)

        try {
            //val rez = mResolver.read(request)
            var x = 6
            mResolver.read(request).setResultListener { result ->
                run {
                    try {
                        var iter: Iterator<HealthData> = result.iterator()
                        while (iter.hasNext()) {
                            var data: HealthData = iter.next()
                            Log.d(APP_TAG, data.toString())
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e(APP_TAG, e.toString());
                    }
                }
            }
            x = 5
        } catch (e: java.lang.Exception) {
            Log.e(APP_TAG, e.toString());
        }
    }

    private fun getLastLocation() {
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
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
//        val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
//        if (localGpsLocation != null)
//            Log.d("Location","Location changed: $localGpsLocation")
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        context = this

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

            val mChronometer = findViewById<Chronometer>(R.id.view_timer)
            mChronometer.base = SystemClock.elapsedRealtime()
            mChronometer.start()

            mChronometer.onChronometerTickListener = OnChronometerTickListener { chronometer ->
                if (chronometer.text.toString().equals("00:10", ignoreCase = true)) {

                    if (!finished_flag) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Info")
                        builder.setMessage("Exercise finished")
                        builder.show()
                        finished_flag = true;

                        val all_data = AllData()

                        enumValues<SensorType>().forEach {
                            when (it)
                            {
                                SensorType.ACC -> all_data.acc_data = (data[it]?: arrayListOf()) as ArrayList<AccelerometerData>
                                SensorType.STEPC -> all_data.stepc_data = (data[it]?: arrayListOf()) as ArrayList<StepCounterData>
                                SensorType.LACC -> all_data.lacc_data = (data[it]?: arrayListOf()) as ArrayList<LinearAccelerometerData>
                                SensorType.GYRO -> all_data.gyroscope_data = (data[it]?: arrayListOf()) as ArrayList<GyroscopeData>
                                SensorType.GRAVITY -> all_data.gravity_data = (data[it]?: arrayListOf()) as ArrayList<GravityData>
                                SensorType.HR -> all_data.hr_data = (data[it]?: arrayListOf()) as ArrayList<HeartRateData>
                            }
                        }

                        val gson = Gson()
                        val json = gson.toJson(all_data)
                        val string = json

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
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor != null) {
            //if (sensor.name != "Acceleration Sensor" && sensor.name != "Gravity Sensor" && sensor.name != "LSM6DSL Acceleration Sensor")
            Log.d("SENSORS", "sensor ${sensor.name}, onAccuracyChanged: $accuracy")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when(event.sensor.type) {
            Sensor.TYPE_GRAVITY ->
            {
                if(data[SensorType.GRAVITY]?.isEmpty() == true || data[SensorType.GRAVITY]?.last()?.timestamp?:0 < System.currentTimeMillis() - 1000)
                {
                    data[SensorType.GRAVITY]?.add(GravityData(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]))
                }
            }

            Sensor.TYPE_GYROSCOPE ->
            {
                if(data[SensorType.GYRO]?.isEmpty() == true || data[SensorType.GYRO]?.last()?.timestamp?:0 < System.currentTimeMillis() - 1000)
                {
                    data[SensorType.GYRO]?.add(GyroscopeData(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]))
                }
            }

            Sensor.TYPE_ACCELEROMETER ->
            {
                if(data[SensorType.ACC]?.isEmpty() == true || data[SensorType.ACC]?.last()?.timestamp?:0 < System.currentTimeMillis() - 1000)
                {
                    data[SensorType.ACC]?.add(AccelerometerData(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]))
                }
            }

            Sensor.TYPE_LINEAR_ACCELERATION ->
            {
                if(data[SensorType.LACC]?.isEmpty() == true || data[SensorType.LACC]?.last()?.timestamp?:0 < System.currentTimeMillis() - 1000)
                {
                    data[SensorType.LACC]?.add(LinearAccelerometerData(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]))
                }
            }

            Sensor.TYPE_HEART_RATE ->
            {
                if(data[SensorType.HR]?.isEmpty() == true || data[SensorType.HR]?.last()?.timestamp?:0 < System.currentTimeMillis() - 1000)
                {
                    data[SensorType.HR]?.add(HeartRateData(System.currentTimeMillis(), event.values[0]))
                }
            }

            Sensor.TYPE_STEP_COUNTER ->
            {
                if(data[SensorType.STEPC]?.isEmpty() == true || data[SensorType.STEPC]?.last()?.timestamp?:0 < System.currentTimeMillis() - 1000)
                {
                    data[SensorType.STEPC]?.add(StepCounterData(System.currentTimeMillis(), event.values[0]))
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

    fun sendGet() {
        //val url = URL("http://www.google.com/")
        val url =
            URL("http://api.openweathermap.org/data/2.5/weather?q=Nis,rs&appid=dfda8f8bd3b25a1f388c08c3f5d51065")

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET

            println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

            inputStream.bufferedReader().use {
                it.lines().forEach { line ->
                    println(line)
                }
            }
        }
    }

    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    fun Context.hasPermission(permissionType: String): kotlin.Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
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
        println("Location changed: $p0")
    }

//    //region location
//    @SuppressLint("MissingPermission")
//    private fun getLastLocation() {
//        if (checkPermissions()) {
//            if (isLocationEnabled()) {
//
//                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
//                    var location: Location? = task.result
//                    if (location == null) {
//                        requestNewLocationData()
//                    } else {
//                        findViewById<TextView>(R.id.latTextView).text = location.latitude.toString()
//                        findViewById<TextView>(R.id.lonTextView).text = location.longitude.toString()
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
//                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                startActivity(intent)
//            }
//        } else {
//            requestPermissions()
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun requestNewLocationData() {
//        var mLocationRequest = LocationRequest()
//        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        mLocationRequest.interval = 0
//        mLocationRequest.fastestInterval = 0
//        mLocationRequest.numUpdates = 1
//
//        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        mFusedLocationClient!!.requestLocationUpdates(
//            mLocationRequest, mLocationCallback,
//            Looper.myLooper()
//        )
//    }
//
//    private val mLocationCallback = object : LocationCallback() {
//        override fun onLocationResult(locationResult: LocationResult) {
//            var mLastLocation: Location = locationResult.lastLocation
//            findViewById<TextView>(R.id.latTextView).text = mLastLocation.latitude.toString()
//            findViewById<TextView>(R.id.lonTextView).text = mLastLocation.longitude.toString()
//        }
//    }
//
//    private fun isLocationEnabled(): Boolean {
//        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
//            LocationManager.NETWORK_PROVIDER
//        )
//    }
//
//    private fun checkPermissions(): kotlin.Boolean {
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            return true
//        }
//        return false
//    }
//
//    private fun requestPermissions() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
//            PERMISSION_ID
//        )
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        if (requestCode == PERMISSION_ID) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                getLastLocation()
//            }
//        }
//    }
    //endregion
}