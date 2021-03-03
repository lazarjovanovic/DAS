package com.example.das
import android.Manifest
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
import kotlinx.android.synthetic.main.activity_main.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {
    private val PERMISSION_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                mgr.registerListener(this, gravitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI)
            }

            //accelerometer sensor
            mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also { accelerometerSensor ->
                mgr.registerListener(this, accelerometerSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI)
            }

            //hearth rate sensor
            mgr.getDefaultSensor(Sensor.TYPE_HEART_RATE).also { hearthRateSensor ->
                mgr.registerListener(this, hearthRateSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI)
            }

            //temperature sensor
            mgr.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE).also { temperatureSensor ->
                mgr.registerListener(this, temperatureSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI)
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
            Log.d("SENSORS", "sensor ${event.sensor.name}, onSensorChanged: The values are ${Arrays.toString(event.values)}")
        }
        var x = 5;
    }
}