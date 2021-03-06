package com.example.blet

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.blet.ble.MyBleManager
import com.example.blet.ui.BleListScreen
import com.example.blet.ui.BottomBar
import com.example.blet.ui.MapScreen
import com.example.blet.ui.SearchDeviceScreen
import com.example.blet.ui.theme.BLETTheme
import com.example.blet.util.DistanceUtil


class MainActivity : ComponentActivity(), SensorEventListener {

    private val bleManager: MyBleManager by lazy { MyBleManager(this@MainActivity) }
    private val bluetoothAdapter by lazy { (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter }
    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private val viewModel: BleViewModel by viewModels()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.viewState.collectAsState()
            BLETTheme {
                var pageSelected by remember {
                    mutableStateOf(0)
                }
                Scaffold(
                    bottomBar = { BottomBar(pageSelected) { page -> pageSelected = page } },
                    modifier = Modifier
                        .fillMaxSize(),
                    content = { innerPadding ->
                        when (pageSelected) {
                            0 -> {
                                BleListScreen(
                                    context = this@MainActivity,
                                    state = state,
                                    scanning = { viewModel.scanning },
                                    startScan = { startScan() },
                                    changeViewState = { state ->
                                        viewModel.changeViewState(state)
                                    },
                                    connectToDevice = { item ->
                                        viewModel.connectToDevice(item, bleManager)
                                    },
                                    addMarker = { item ->
                                        state.location?.let { loc ->
                                            val distance =
                                                DistanceUtil.getDistanceFromDevice(
                                                    item.scanResult.rssi,
                                                    item.scanResult.txPower
                                                )
                                            val markerList = state.listOfMarkers + Marker(loc, distance)
                                            viewModel.changeViewState(state.copy(listOfMarkers = markerList))
                                        } ?: run {
                                            Toast.makeText(this@MainActivity, "No location", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    searchForDevice = { item ->
                                        pageSelected = 2
                                        viewModel.searchForDevice(item, bluetoothAdapter)
                                    },
                                    padding = innerPadding
                                )
                            }
                            1 -> {
                                MapScreen(state = state)
                            }
                            2 -> {
                                SearchDeviceScreen(state = state, innerPadding) { vibrate(250L) }
                            }
                        }
                    }
                )
            }
        }

        startScan()
        getGpsLocation()
        setupSensorListener()
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    private lateinit var sensorManager: SensorManager
    private fun setupSensorListener() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    private fun vibrate(ms: Long) {
        val appContext = (this@MainActivity).applicationContext
        val baseContext = (this@MainActivity).baseContext
        Log.d("VIBRATION", "VIBRATE for $ms")

        (appContext.getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.let {
            Log.d("VIBRATION", "appContext vibrator not null")
            it.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } ?: run {
            Log.d("VIBRATION", "appContext vibrator is null")
        }

        (baseContext.getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.let {
            Log.d("VIBRATION", "base vibrator not null")
            it.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } ?: run {
            Log.d("VIBRATION", "base vibrator is null")
        }
    }

    private fun getGpsLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(permissions.toTypedArray(), REQUEST_CODE)
            return
        }
        viewModel.getGpsLocation(locationManager)
    }


    private fun startScan() {
        if (!hasPermissions(this, permissions)) {
            requestPermissions(permissions.toTypedArray(), REQUEST_CODE)
        } else {
            try {
                viewModel.scanLeDevices(bluetoothAdapter)
            } catch (e: Exception) {
                //TODO
            }
        }
    }


    private fun hasPermissions(context: Context, permissions: List<String>): Boolean {
        for (perm in permissions) {
            if (ActivityCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    companion object {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        const val REQUEST_CODE = 175
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            viewModel.addSensorData(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}


