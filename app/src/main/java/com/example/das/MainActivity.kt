package com.example.das

import AnalysisDatapointData
import Json4Kotlin_Base
import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
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
import android.os.*
import android.util.Log
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.utils.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import io.nlopez.smartlocation.SmartLocation
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
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
    private val localScope = CoroutineScope(SupervisorJob() + IO)

    // region BLE
    private var band_device_found = false
    private var band_device: BluetoothDevice? = null
    private var gatt:BluetoothGatt? = null
    private val scanResults = mutableListOf<ScanResult>()
    private var isScanning = false
    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            // User tapped on a scan result
            if (isScanning) {
                stopBleScan()
            }
        }
    }
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    var bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }

                    // TODO: Store a reference to BluetoothGatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                gatt.close()
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    //endregion

    private val PERMISSION_CODE = 1000

    val APP_TAG = "SimpleHealth"

    private var mInstance: MainActivity? = null
    var coroutines_finished = MutableStateFlow(false)

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

        start_button.isEnabled = false;

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        context = this

        mInstance = this

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

        start_scan.setOnClickListener {
            ScanTest()
//            if (isScanning) {
//                stopBleScan()
//            }
//            else
//            {
//                startBleScan()
//            }
        }

        start_button.setOnClickListener {
            try
            {
                readBatteryLevel(gatt)
            }
            catch (e: Throwable)
            {
                var x = 5
            }


            enumValues<SensorType>().forEach { data[it] = ArrayList() }

            start_button.isEnabled = false;
            var finished_flag = false;
            localScope.launch {
                coroutines_finished.emit(false)
            }

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

                            val value = localScope.launch {
                                try {
                                    for (i in 0 until loc_data.size)
                                    {
                                        sendWeatherGet(loc_data[i].lat, loc_data[i].lon)
                                        delay(1000)
                                    }
                                    coroutines_finished.emit(true)
                                }
                                catch (e: Throwable)
                                {
                                    println(e)
                                }

                            }

                            localScope.launch(IO) {
                                try{
                                    while(!coroutines_finished.value)
                                    {
                                        delay(1000)
                                    }

                                    val gson = Gson()
                                    val json_data = gson.toJson(all_data)
                                    val response = postTrainingData(json_data)?.string()

                                    //val adpList: List<AnalysisDatapointData> = gson.fromJson(response?.string() , Array<AnalysisDatapointData>::class.java).toList()
                                    withContext(Main)
                                    {
                                        val intent = Intent(this@MainActivity, ResultingActivity::class.java)
                                        val bundle = Bundle()
                                        bundle.putString("datapoint", response)
                                        intent.putExtras(bundle)
                                        startActivity(intent)
                                    }
                                }
                                catch (e: Throwable)
                                {
                                    println(e)
                                }

                            }
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
            catch (e: Throwable)
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
                                    w_data.mainOpenWeather.temp.toFloat() - 273,
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
        catch (e: Throwable)
        {
            println(e.toString())
        }
    }

    fun postTrainingData(training_data: String): ResponseBody? {
        try{
            //val url = "http://109.92.100.93:5000/process_exercise"
            val url = "http://192.168.85.159:5000/process_exercise"

            val client = OkHttpClient()

            val JSON = "application/json; charset=utf-8".toMediaType()
            val body = RequestBody.create(JSON, training_data)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute().body
            return response
        }
        catch (e: Exception)
        {
            println(e)
            return null
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

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            //while(!band_device_found)
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.d(
                        "ScanCallback",
                        "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address"
                    )
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)

                var rez_device_str = result.device.toString()
                if(rez_device_str.equals("FF:2C:43:E7:64:E9")) // amazfit band 5
                {
                    with(result.device){
                        gatt = connectGatt(context, false, gattCallback)
                        Log.w("ScanResultAdapter", "Connecting to $address")
                    }
                    if(isScanning)
                    {
                        band_device_found = true
                        band_device = result.device
                        stopBleScan()
                        start_button.isEnabled = true;
                        start_scan.isEnabled = false;
                    }
                    isScanning = false
                }
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.d("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        }
        else {
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            println("Location permission required")
            requestPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    fun Context.hasPermission(permissionType: String): kotlin.Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun BluetoothGattCharacteristic.containsProperty(property: Int): kotlin.Boolean {
        return properties and property != 0
    }

    fun BluetoothGattCharacteristic.isReadable(): kotlin.Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    private fun readBatteryLevel(gatt: BluetoothGatt?) {
        // val batteryServiceUuid = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb") // pulse oximeter
        // val batteryLevelCharUuid = UUID.fromString("00002a62-0000-1000-8000-00805f9b34fb")
        if (gatt != null)
        {
            //.discoverServices()
            var list_services: List<BluetoothGattService> = gatt.services

            val batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb") // battery
            val batteryLevelCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
            val batteryLevelChar = gatt
                .getService(batteryServiceUuid)?.getCharacteristic(batteryLevelCharUuid)
            if (batteryLevelChar?.isReadable() == true) {
                gatt.readCharacteristic(batteryLevelChar)
            }
        }
    }

    fun ScanTest()
    {
        var context: Context = this;
        var rxBleClient: RxBleClient? = RxBleClient.create(context)

        val scanSettings = com.polidea.rxandroidble2.scan.ScanSettings.Builder().build()
        val scanFilter = com.polidea.rxandroidble2.scan.ScanFilter.Builder().build()

        val scanSubscription: Disposable? = rxBleClient?.scanBleDevices(scanSettings, scanFilter)
            ?.subscribe { scanResult ->
                var device_mac = scanResult.bleDevice.bluetoothDevice
                var address = scanResult.bleDevice.bluetoothDevice.address
                val batteryServiceUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb") //batery
                //val batteryServiceUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb") //heart rate
                //val batteryServiceUuid = UUID.fromString("00002a62-0000-1000-8000-00805f9b34fb") //pulse oximeter
                if(address.equals("FF:2C:43:E7:64:E9")) {
                    Log.d("Here log", "Device found")

                    /////////////////////////////////////////
                    //https://github.com/Polidea/RxAndroidBle
                    /////////////////////////////////////////

                    val device: RxBleDevice = rxBleClient.getBleDevice(address)
                    device.establishConnection(false)
                        .flatMapSingle { rxBleConnection -> rxBleConnection.readCharacteristic(batteryServiceUuid) }
//                        .flatMapSingle { rxBleConnection ->
//                            rxBleConnection.discoverServices()
//                        }
                        .subscribe(
                            { characteristicValue ->
                                Log.d("value", characteristicValue.toString())
                                val x = 5
                            }
                        ) { throwable ->
                            Log.d("throwable", throwable.toString())
                        }
                }
            }
    }
}