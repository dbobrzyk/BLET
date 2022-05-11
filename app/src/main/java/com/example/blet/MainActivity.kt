package com.example.blet

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class MainActivity : ComponentActivity() {

    private val _viewState: MutableStateFlow<BleViewState> = MutableStateFlow(
        BleViewState()
    )
    val viewState: StateFlow<BleViewState> = _viewState.asStateFlow()

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            super.onScanResult(callbackType, result)
            Log.d("BLE", "Scanning result: $result")

            if (result.device.name?.contains("Mi Smart") == true) {
                _viewState.update { currentState ->
                    currentState.copy(
                        chosenDevice = result
                    )
                }
            }

            if (!viewState.value.listOfBleDevices.map { it.scanResult.device.address }
                    .contains(result.device.address)) {
                val newList =
                    (viewState.value.listOfBleDevices + BleDeviceWrapper(scanResult = result)).sortedByDescending { it.range }
                _viewState.update { currentState ->
                    currentState.copy(
                        viewHeader = "Scanning results:",
                        listOfBleDevices = newList,
                        dataLoading = false
                    )
                }
            }
        }
    }

    private var scanning = false
    private val handler = Handler()
    private val SCAN_PERIOD: Long = 3000

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLETTheme {
                var pageSelected by remember {
                    mutableStateOf(0)
                }
                val state = viewState.collectAsState()
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
                                    scanning = { scanning },
                                    startScan = { startScan() },
                                    changeViewState = { state ->
                                        _viewState.update { state }
                                    },
                                    connectToDevice = { item ->
                                        connectToDevice(item)
                                    },
                                    addMarker = { item ->
                                        _viewState.value.location?.let { loc ->
                                            val distance =
                                                DistanceUtil.getDistanceFromDevice(
                                                    item.scanResult.rssi,
                                                    item.scanResult.txPower
                                                )
                                            val markerList = _viewState.value.listOfMarkers + Marker(loc, distance)
                                            _viewState.update { it.copy(listOfMarkers = markerList) }
                                        } ?: run {
                                            Toast.makeText(this@MainActivity, "No location", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    searchForDevice = { item ->
                                        pageSelected = 2
                                        searchForDevice(item)
                                    }
                                )
                            }
                            1 -> {
                                MapScreen(state = state)
                            }
                            2 -> {
                                SearchDeviceScreen(state = state, innerPadding)
                            }
                        }
                    }
                )
            }
        }

        startScan()
        getGpsLocation()
    }

    private fun searchForDevice(item: BleDeviceWrapper) {
        object : CountDownTimer(30000, 550) {

            override fun onTick(millisUntilFinished: Long) {
                scanChosenDevice(item.scanResult)
            }

            override fun onFinish() {
            }
        }.start()
    }

    private fun connectToDevice(item: BleDeviceWrapper) {
        val bleManager = MyBleManager(this@MainActivity)
        val bluetoothDevice = item.scanResult.device
        bleManager.connect(bluetoothDevice)
            .retry(3 /* times, with */, 100 /* ms interval */)
            .timeout(15_000 /* ms */)
            .useAutoConnect(true)
            // A connection timeout can be also set. This is additional to the Android's connection timeout which is 30 seconds.
            .timeout(15_000 /* ms */)
            // Each request has number of callbacks called in different situations:
            .before { device ->
                Log.d("BLE connect", "Before")
            }
            .done { device ->
                Log.d("BLE connect", "Done, device: $device")
            }
            .fail { device, code ->
                Log.d("BLE connect", "fail $code")
            }
            .then { device ->
                Log.d("BLE connect", "then")
            }
            // Each request must be enqueued.
            // Kotlin projects can use suspend() or suspendForResult() instead.
            // Java projects can also use await() which is blocking.
            .enqueue()
    }

    private fun startScan() {
        if (!hasPermissions(this, permissions)) {
            Log.d("BLE", "No permission")
            requestPermissions(permissions.toTypedArray(), REQUEST_CODE)
        } else {
            try {
                Log.d("BLE", "Prepare for scan")
                scanLeDevices()
            } catch (e: Exception) {
                Log.d("BLE", "Scanning failed: \n\n $e")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevices() {
        _viewState.update { currentState ->
            currentState.copy(
                viewHeader = "Scanning...",
                dataLoading = true,
                listOfBleDevices = listOf()
            )
        }
        val bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (canScan(bluetoothAdapter)) { // Stops scanning after a pre-defined scan period.
            Log.d("BLE", "Permissions OK - Start scanning")
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner?.startScan(leScanCallback)
        } else {
            _viewState.update { currentState ->
                currentState.copy(
                    viewHeader = "Something went wrong, try again",
                    dataLoading = false,
                    listOfBleDevices = listOf()
                )
            }
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    private val chosenScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (result.device.name?.contains("Mi Smart") == true) {
                _viewState.update { currentState ->
                    currentState.copy(
                        chosenDevice = result
                    )
                }
            }
        }
    }

    var chosenScanning = false

    @SuppressLint("MissingPermission")
    private fun scanChosenDevice(scanResult: ScanResult) {

        val bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        val scanFilter = ScanFilter.Builder()
            .setDeviceAddress(scanResult.device.address)
            .build()

        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build();

        if (canScan(bluetoothAdapter)) { // Stops scanning after a pre-defined scan period.
            Log.d("BLE", "Permissions OK - Start scanning")
            handler.postDelayed({
                chosenScanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }, 1000)
            chosenScanning = true
            bluetoothLeScanner?.startScan(
                listOf(scanFilter),
                scanSettings,
                chosenScanCallback
            )
        } else {
            Log.d("BLE", "Wont start scanning (chosen)")
        }
    }

    private fun canScan(bluetoothAdapter: BluetoothAdapter?): Boolean {
        return !scanning && hasPermissions(
            this,
            permissions
        ) && bluetoothAdapter?.isEnabled == true
    }

    private fun hasPermissions(context: Context, permissions: List<String>): Boolean {
        for (perm in permissions) {
            if (ActivityCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }


    private fun getGpsLocation() {

        Log.d("LOCALIAZABLE", "GET GPS")
        val locationListener: LocationListener = MyLocationListener { location ->
            _viewState.update { currentState ->
                currentState.copy(
                    location = location
                )
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LOCALIAZABLE", "NO PERMISSION")
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        location?.let {
            _viewState.update { currentState ->
                currentState.copy(
                    location = it
                )
            }
        }
        Log.d("LOCALIAZABLE", "Requestuje location updates jkc")
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 0, 0f, locationListener
        )
    }

    private class MyLocationListener(private val setLocation: (Location) -> Unit) : LocationListener {

        override fun onProviderEnabled(provider: String) {
            Log.d("LOCALIAZACKJAAA", "provider enabled")
            super.onProviderEnabled(provider)
        }

        override fun onProviderDisabled(provider: String) {
            Log.d("LOCALIAZACKJAAA", "provider disabled")
            super.onProviderDisabled(provider)
        }

        override fun onLocationChanged(loc: Location) {
            Log.d("LOCALIAZACKJAAA", loc.toString())
            setLocation(loc)
        }

        override fun onLocationChanged(locations: MutableList<Location>) {
            Log.d("LOCALIAZACJEEEEE!!!", locations.toString())
            setLocation(locations.first())
            super.onLocationChanged(locations)
        }
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
}

@Immutable
data class BleViewState(
    val viewHeader: String = "Bluetooth Devices Scanner",
    val dataLoading: Boolean = false,
    val listOfBleDevices: List<BleDeviceWrapper> = listOf(),
    val location: Location? = null,
    val view: ViewType = com.example.blet.ViewType.BLE_LIST,
    val listOfMarkers: List<Marker> = listOf(),
    val chosenDevice: ScanResult? = null
)

data class Marker(
    val location: Location,
    val distance: Double
)

enum class ViewType {
    BLE_LIST,
    MAP,
}

data class BleDeviceWrapper(
    val scanResult: ScanResult,
    val isExpanded: Boolean = false
) {
    val range: Int = when (scanResult.rssi) {
        in -60..0 -> {
            3
        }
        in -80..-60 -> {
            2
        }
        in -100..-80 -> {
            1
        }
        else -> {
            0
        }
    }

    val rangeDrawable = when (range) {
        3 -> {
            R.drawable.ic_range_3
        }
        2 -> {
            R.drawable.ic_range_2
        }
        1 -> {
            R.drawable.ic_range_1
        }
        else -> {
            R.drawable.ic_range_0
        }
    }
}


