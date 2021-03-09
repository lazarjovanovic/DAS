package com.example.das

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.sdk.healthdata.*
import com.samsung.android.sdk.healthdata.HealthConstants.Exercise
import com.samsung.android.sdk.healthdata.HealthConstants.StepDailyTrend
import com.samsung.android.sdk.healthdata.HealthDataStore.ConnectionListener
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Boolean
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {
    private val PERMISSION_CODE = 1000

    val APP_TAG = "SimpleHealth"

    private var mInstance: MainActivity? = null
    private var mStore: HealthDataStore? = null
    private var mConnError: HealthConnectionErrorResult? = null
    private var mKeySet: Set<PermissionKey>? = null

    override fun onDestroy() {
        mStore!!.disconnectService()
        super.onDestroy()
    }

    private fun showConnectionFailureDialog(error: HealthConnectionErrorResult) {
        val alert = AlertDialog.Builder(this)
        mConnError = error
        var message = "Connection with Samsung Health is not available"
        if (mConnError!!.hasResolution()) {
            message = when (error.errorCode) {
                HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED -> "Please install Samsung Health"
                HealthConnectionErrorResult.OLD_VERSION_PLATFORM -> "Please upgrade Samsung Health"
                HealthConnectionErrorResult.PLATFORM_DISABLED -> "Please enable Samsung Health"
                HealthConnectionErrorResult.USER_AGREEMENT_NEEDED -> "Please agree with Samsung Health policy"
                else -> "Please make Samsung Health available"
            }
        }
        alert.setMessage(message)
        alert.setPositiveButton("OK", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, id: Int) {
                if (mConnError!!.hasResolution()) {
                    mConnError!!.resolve(mInstance)
                }
            }
        })
        if (error.hasResolution()) {
            alert.setNegativeButton("Cancel", null)
        }
        alert.show()
    }

    private val mPermissionListener: HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult> = object : HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult>
    {
        override fun onResult(result: HealthPermissionManager.PermissionResult)
        {
            Log.d(APP_TAG, "Permission callback is received.")
            val resultMap = result.resultMap

            if (resultMap.containsValue(Boolean.FALSE)) {
                var x = 5
                // Requesting permission fails
            } else {
                var x = 5;
                // Get the current step count and display it
            }
        }
    }

    private val mConnectionListener: ConnectionListener = object : ConnectionListener {
        override fun onConnected() {
            Log.d(APP_TAG, "Health data service is connected.")
            val pmsManager = HealthPermissionManager(mStore)

            try {
                // Check whether the permissions that this application needs are acquired
                val resultMap = pmsManager.isPermissionAcquired(mKeySet)
                if (resultMap.containsValue(Boolean.FALSE)) {
                    // Request the permission for reading step counts if it is not acquired
                    pmsManager.requestPermissions(mKeySet, this@MainActivity)
                        .setResultListener(mPermissionListener)
                    var x = 5;
                } else {
                    var x = 5;
                    // Get the current step count and display it
                    // ...
                }
            } catch (e: Exception) {
                Log.e(APP_TAG, e.javaClass.name + " - " + e.message)
                Log.e(APP_TAG, "Permission setting fails.")
            }
        }

        override fun onConnectionFailed(error: HealthConnectionErrorResult) {
            Log.d(APP_TAG, "Health data service is not available.")
            showConnectionFailureDialog(error)
        }

        override fun onDisconnected() {
            Log.d(APP_TAG, "Health data service is disconnected.")
        }
    }


    private var mStore1: HealthDataStore? = null
    var mKeys1: HashSet<PermissionKey> = HashSet()

    private val APP_TAG1 = "MyApp"

    fun requestPermission() {
        mStore1 = HealthDataStore(this, mConnectionListener)
        mStore1!!.connectService();
        // Acquire permission
        val pmsManager1 = HealthPermissionManager(mStore1)
        mKeys1.add(PermissionKey(Exercise.HEALTH_DATA_TYPE, PermissionType.READ))
        mKeys1.add(PermissionKey(Exercise.HEALTH_DATA_TYPE, PermissionType.WRITE))
        mKeys1.add(PermissionKey(StepDailyTrend.HEALTH_DATA_TYPE, PermissionType.READ))
        try {
            pmsManager1.requestPermissions(mKeys1, this@MainActivity)
                .setResultListener(mPermissionListener)
        } catch (e: java.lang.Exception) {
            Log.d(APP_TAG1, "requestPermissions() fails")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //requestPermission()

        ////////////////////////////////////////////////////////////
        mInstance = this
        mKeySet = HashSet()
        (mKeySet as HashSet<PermissionKey>).add(PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ))
        // Create a HealthDataStore instance and set its listener
        mStore = HealthDataStore(this, mConnectionListener)
        // Request the connection to the health data store
        mStore!!.connectService()
        ////////////////////////////////////////////////////////////

        val mgr = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensors: List<Sensor> = mgr.getSensorList(Sensor.TYPE_ALL)

        for (sensor in sensors) {
            Log.d("Sensors", "" + sensor.getName())
        }

        if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_DENIED)
        {
            val permission = arrayOf(Manifest.permission.INTERNET, Manifest.permission.BODY_SENSORS)
            requestPermissions(permission, PERMISSION_CODE)
        }
        else {
            Thread(Runnable {
                sendGet();
            }).start()
        }

        start_button.setOnClickListener {
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

            //hearth rate sensor
            mgr.getDefaultSensor(Sensor.TYPE_HEART_RATE).also { hearthRateSensor ->
                mgr.registerListener(
                    this, hearthRateSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            //temperature sensor
            mgr.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE).also { temperatureSensor ->
                mgr.registerListener(
                    this, temperatureSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            val mChronometer = findViewById<Chronometer>(R.id.view_timer)
            mChronometer.base = SystemClock.elapsedRealtime()
            mChronometer.start()

            mChronometer.onChronometerTickListener = OnChronometerTickListener { chronometer ->
                if (chronometer.text.toString().equals("00:05", ignoreCase = true)) {

                    if(!finished_flag)
                    {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Info")
                        builder.setMessage("Exercise finished")
                        builder.show()
                        finished_flag = true;
                    }

                    println("Timer finished")
                    start_button.isEnabled = true;
                    mChronometer.stop();
                    //mChronometer.base = SystemClock.elapsedRealtime()
                }
            }
        }
    }

    fun sendGet() {
        //val url = URL("http://www.google.com/")
        val url = URL("http://api.openweathermap.org/data/2.5/weather?q=Nis,rs&appid=dfda8f8bd3b25a1f388c08c3f5d51065")

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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor != null) {
            if (sensor.name != "Acceleration Sensor" && sensor.name != "Gravity Sensor" && sensor.name != "LSM6DSL Acceleration Sensor")
                Log.d("SENSORS", "sensor ${sensor.name}, onAccuracyChanged: $accuracy")
        }
        var x = 5;
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.name != "Acceleration Sensor" && event.sensor.name != "Gravity Sensor" && event.sensor.name != "LSM6DSL Acceleration Sensor") {
            Log.d(
                "SENSORS", "sensor ${event.sensor.name}, onSensorChanged: The values are ${
                    Arrays.toString(
                        event.values
                    )
                }"
            )
        }
        var x = 5;
    }
}