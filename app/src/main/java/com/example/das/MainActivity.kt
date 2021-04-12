package com.example.das

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.samsung.android.sdk.healthdata.*
import com.samsung.android.sdk.healthdata.HealthDataStore.ConnectionListener
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Boolean
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.UUID


class MainActivity : AppCompatActivity(), SensorEventListener {
    private val ENVIRONMENTAL_SERVICE_UUID = UUID.randomUUID()
    private val ENABLE_BLUETOOTH_REQUEST_CODE = 1
    private val LOCATION_PERMISSION_REQUEST_CODE = 2

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

    private fun readTodayStepCountData() {
//        val filter: HealthDataResolver.Filter = HealthDataResolver.Filter.and(
//            HealthDataResolver.Filter.eq(StepDailyTrend.DAY_TIME, getTodayStartUtcTime()),
//            HealthDataResolver.Filter.eq(StepDailyTrend.SOURCE_TYPE, StepDailyTrend.SOURCE_TYPE_ALL)
//        )

        val request: HealthDataResolver.ReadRequest = HealthDataResolver.ReadRequest.Builder() // Set the data type
            //.setDataType(StepDailyTrend.HEALTH_DATA_TYPE) // Set a filter
            .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
            //.setFilter(filter)
            .build()

        var mResolver = HealthDataResolver(mStore, null)

        try {
            //val rez = mResolver.read(request)
            var x = 6
            mResolver.read(request).setResultListener{result ->
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
                readTodayStepCountData()
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
                    readTodayStepCountData()
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


//    private var mStore1: HealthDataStore? = null
//    var mKeys1: HashSet<PermissionKey> = HashSet()
//
//    private val APP_TAG1 = "MyApp"
//    fun requestPermission() {
//        mStore1 = HealthDataStore(this, mConnectionListener)
//        mStore1!!.connectService();
//        // Acquire permission
//        val pmsManager1 = HealthPermissionManager(mStore1)
//        mKeys1.add(PermissionKey(Exercise.HEALTH_DATA_TYPE, PermissionType.READ))
//        mKeys1.add(PermissionKey(Exercise.HEALTH_DATA_TYPE, PermissionType.WRITE))
//        mKeys1.add(PermissionKey(StepDailyTrend.HEALTH_DATA_TYPE, PermissionType.READ))
//        try {
//            pmsManager1.requestPermissions(mKeys1, this@MainActivity)
//                .setResultListener(mPermissionListener)
//            var x = 5
//        } catch (e: java.lang.Exception) {
//            Log.d(APP_TAG1, "requestPermissions() fails")
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //requestPermission()

        ////////////////////////////////////////////////////////////
        mInstance = this
        mKeySet = HashSet()
        (mKeySet as HashSet<PermissionKey>).add(
            PermissionKey(
                HealthConstants.StepCount.HEALTH_DATA_TYPE,
                PermissionType.READ
            )
        )
//        (mKeySet as HashSet<PermissionKey>).add(
//            PermissionKey(
//                HealthConstants.OxygenSaturation.HEALTH_DATA_TYPE,
//                PermissionType.READ
//            )
//        )
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

        start_scan.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            }
            else
            {
                startBleScan()
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

    //blutetooth section
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
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

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        }
        else {
            //scanResults.clear()
            //scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            println("Location permission required")
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE)
            var x = 5
//            alert {
//                title = "Location permission required"
//                message = "Starting from Android M (6.0), the system requires apps to be granted " +
//                        "location access in order to scan for BLE devices."
//                isCancelable = false
//                positiveButton(android.R.string.ok) {
//                    requestPermission(
//                        Manifest.permission.ACCESS_FINE_LOCATION,
//                        LOCATION_PERMISSION_REQUEST_CODE
//                    )
//                }
//            }.show()
        }
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
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    val filter = ScanFilter.Builder().setServiceUuid(
        ParcelUuid.fromString(ENVIRONMENTAL_SERVICE_UUID.toString())
    ).build()


    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

//    private val scanCallback = object : ScanCallback() {
//        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            with(result.device) {
//                Log.d("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
//            }
//        }
//    }

    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { start_scan.text = if (value) "Stop Scan" else "Start Scan" }
        }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            // User tapped on a scan result
            if (isScanning) {
                stopBleScan()
            }
            val x = 5
//            with(result.device) {
//                Log.w("ScanResultAdapter", "Connecting to $address")
//                connectGatt(context, false, gattCallback)
//            }
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

                    var x = 5
                    // TODO: Store a reference to BluetoothGatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                printGattTable() // See implementation just above this section
                var x = 5
                readBatteryLevel(gatt)
                // Consider connection setup as complete here
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        //Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.toHexString()}")
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value[0]}")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }
    }

    fun BluetoothGattCharacteristic.isReadable(): kotlin.Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): kotlin.Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): kotlin.Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): kotlin.Boolean {
        return properties and property != 0
    }

    private fun readBatteryLevel(gatt: BluetoothGatt) {
//        val batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb") // battery
//        val batteryLevelCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        val batteryServiceUuid = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb") // pulse oximeter
        val batteryLevelCharUuid = UUID.fromString("00002a62-0000-1000-8000-00805f9b34fb")
        var x = 5
        val batteryLevelChar = gatt
            .getService(batteryServiceUuid)?.getCharacteristic(batteryLevelCharUuid)
        if (batteryLevelChar?.isReadable() == true) {
            gatt.readCharacteristic(batteryLevelChar)
        }
        x = 6
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.d("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)

                var rez_device_str = result.device.toString()
                if(rez_device_str.equals("FF:2C:43:E7:64:E9"))
                {
                    with(result.device){
                        Log.w("ScanResultAdapter", "Connecting to $address")
                        var context: Context? = null
                        var gatt = connectGatt(context, false, gattCallback)

                        val batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")

                        val x = 5
                    }
                }
                else
                {
                    val y = 5
                }
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.d("ScanCallback", "onScanFailed: code $errorCode")
        }
    }
}