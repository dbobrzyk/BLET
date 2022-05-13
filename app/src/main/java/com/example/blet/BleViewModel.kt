package com.example.blet

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.CountDownTimer
import android.os.Handler
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.example.blet.ble.MyBleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class BleViewModel @Inject constructor() : ViewModel() {

    var scanning = false
    private val handler = Handler()
    private val SCAN_PERIOD: Long = 3000

    private val _viewState: MutableStateFlow<BleViewState> = MutableStateFlow(BleViewState())
    val viewState: StateFlow<BleViewState> = _viewState.asStateFlow()

    fun changeViewState(state: BleViewState) {
        _viewState.update { state }
    }

    fun searchForDevice(item: BleDeviceWrapper, bluetoothAdapter: BluetoothAdapter?) {
        object : CountDownTimer(30000, 250) {

            override fun onTick(millisUntilFinished: Long) {
                scanChosenDevice(item.scanResult, bluetoothAdapter)
            }

            override fun onFinish() {}
        }.start()
    }

    @SuppressLint("MissingPermission")
    fun scanLeDevices(bluetoothAdapter: BluetoothAdapter?) {
        _viewState.update { currentState ->
            currentState.copy(
                viewHeader = "Scanning...",
                dataLoading = true,
                listOfBleDevices = listOf()
            )
        }
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (canScan(bluetoothAdapter)) { // Stops scanning after a pre-defined scan period.
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

    fun connectToDevice(item: BleDeviceWrapper, bleManager: MyBleManager) {
        //TODO in progress
        val bluetoothDevice = item.scanResult.device
        bleManager.connect(bluetoothDevice)
            .retry(3 /* times, with */, 100 /* ms interval */)
            .timeout(15_000 /* ms */)
            .useAutoConnect(true)
            // A connection timeout can be also set. This is additional to the Android's connection timeout which is 30 seconds.
            .timeout(15_000 /* ms */)
            // Each request has number of callbacks called in different situations:
            // Each request must be enqueued.
            // Kotlin projects can use suspend() or suspendForResult() instead.
            // Java projects can also use await() which is blocking.
            .enqueue()
    }

    @SuppressLint("MissingPermission")
    fun getGpsLocation(locationManager: LocationManager) {
        val locationListener: LocationListener = MyLocationListener { location ->
            _viewState.update { currentState ->
                currentState.copy(
                    location = location
                )
            }
        }

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        location?.let {
            _viewState.update { currentState ->
                currentState.copy(
                    location = it
                )
            }
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 0, 0f, locationListener
        )
    }

    @SuppressLint("MissingPermission")
    private fun scanChosenDevice(scanResult: ScanResult, bluetoothAdapter: BluetoothAdapter?) {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        val scanFilter = ScanFilter.Builder()
            .setDeviceAddress(scanResult.device.address)
            .build()

        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build();

        if (canScan(bluetoothAdapter)) { // Stops scanning after a pre-defined scan period.
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
        }
    }

    private fun canScan(bluetoothAdapter: BluetoothAdapter?): Boolean {
        return !scanning && bluetoothAdapter?.isEnabled == true
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

    private class MyLocationListener(private val setLocation: (Location) -> Unit) : LocationListener {
        override fun onLocationChanged(loc: Location) {
            setLocation(loc)
        }

        override fun onLocationChanged(locations: MutableList<Location>) {
            setLocation(locations.first())
            super.onLocationChanged(locations)
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
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
}

@Immutable
data class BleViewState(
    val viewHeader: String = "Bluetooth Devices Scanner",
    val dataLoading: Boolean = false,
    val listOfBleDevices: List<BleDeviceWrapper> = listOf(),
    val location: Location? = null,
    val listOfMarkers: List<Marker> = listOf(),
    val chosenDevice: ScanResult? = null
)

data class Marker(
    val location: Location,
    val distance: Double
)

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
