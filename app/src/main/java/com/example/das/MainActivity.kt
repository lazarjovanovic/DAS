package com.example.das

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {
    private val INTERNET_PERMISSION_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mgr = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensors: List<Sensor> = mgr.getSensorList(Sensor.TYPE_ALL)

        for (sensor in sensors) {
            Log.d("Sensors", "" + sensor.getName())
        }


        if (checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED)
        {
            val permission = arrayOf(Manifest.permission.INTERNET)
            requestPermissions(permission, INTERNET_PERMISSION_CODE)
        }
        else {
            Thread(Runnable {
                sendGet();
            }).start()
        }

        start_button.setOnClickListener {
            start_button.isEnabled = false;

            val mChronometer = findViewById<Chronometer>(R.id.view_timer)
            mChronometer.base = SystemClock.elapsedRealtime()
            mChronometer.start()

            mChronometer.onChronometerTickListener = OnChronometerTickListener { chronometer ->
                if (chronometer.text.toString().equals("00:30", ignoreCase = true)) {
                    println("Timer finished")
                    start_button.isEnabled = true;
                    mChronometer.stop();
                }
            }


        }
        val x = 5;
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
}