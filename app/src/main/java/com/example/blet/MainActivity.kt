package com.example.blet

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.blet.ble.MyBleManager
import com.example.blet.ui.theme.BLETTheme
import com.example.blet.ui.theme.darkBlue
import com.example.blet.ui.theme.lightBlue
import com.example.blet.ui.theme.lightestBlue
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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
    private val SCAN_PERIOD: Long = 30000

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLETTheme {
                val state = viewState.collectAsState()
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(state.value.dataLoading && scanning),
                        onRefresh = { startScan() },
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            lightestBlue,
                                            lightBlue
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            item {
                                Text(
                                    state.value.viewHeader,
                                    style = TextStyle(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp, bottom = 16.dp)
                                )
                            }
                            items(state.value.listOfBleDevices.size) { index ->
                                val item = state.value.listOfBleDevices[index]
                                val isMyBeacon = item.scanResult.device.address == "49:11:B5:A8:7F:D1"
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .shadow(5.dp, RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isMyBeacon)
                                                lightBlue
                                            else
                                                Color.White
                                        )
                                        .fillMaxWidth()
                                        .clickable {
                                            val newList = state.value.listOfBleDevices.map {
                                                if (it == item) {
                                                    item.copy(isExpanded = !item.isExpanded)
                                                } else {
                                                    it.copy(isExpanded = false)
                                                }
                                            }
                                            _viewState.update { currentState ->
                                                currentState.copy(
                                                    listOfBleDevices = newList
                                                )
                                            }
                                        }
                                        .animateContentSize(
                                            animationSpec = tween(
                                                delayMillis = 0,
                                                easing = LinearOutSlowInEasing
                                            )
                                        )
                                ) {

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()

                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_bluetooth),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .size(48.dp)
                                                .align(Alignment.CenterVertically)
                                        )
                                        val name =
                                            item.scanResult.device.name ?: item.scanResult.scanRecord?.deviceName ?: "-"
                                        Text(
                                            text = "Device uuid: ${item.scanResult.device.address} \nDevice name: $name",
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .weight(1f, false)
                                        )
                                        Image(
                                            painter = painterResource(id = item.rangeDrawable),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(16.dp)
                                                .align(Alignment.CenterVertically)
                                        )
                                    }
                                    if (item.isExpanded) {
                                        Divider(
                                            color = Color.LightGray,
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Text(text = "Signal strenght: ${item.scanResult.rssi} dBm")
                                            Text(text = "Connectable: ${item.scanResult.isConnectable}")
                                            Text(
                                                color = Color.White,
                                                text = "Connect",
                                                modifier = Modifier
                                                    .padding(top = 8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(darkBlue)
                                                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
                                                    .align(Alignment.CenterHorizontally)
                                                    .clickable {
                                                        connectToDevice(item)
                                                        item.scanResult.device.connectGatt(
                                                            this@MainActivity,
                                                            true,
                                                            object : BluetoothGattCallback() {

                                                                override fun onConnectionStateChange(
                                                                    gatt: BluetoothGatt?,
                                                                    status: Int,
                                                                    newState: Int
                                                                ) {
                                                                    super.onConnectionStateChange(
                                                                        gatt,
                                                                        status,
                                                                        newState
                                                                    )
                                                                    Log.i(
                                                                        "BLE",
                                                                        "State changed // from $status to $newState \n gatt: $gatt"
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    }
                                            )

                                        }
                                    }
                                }

                            }

                        }
                    }
                }
            }
        }

        startScan()
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
                scanLeDevice()
            } catch (e: Exception) {
                Log.d("BLE", "Scanning failed: \n\n $e")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        _viewState.update { currentState ->
            currentState.copy(
                viewHeader = "Scanning...",
                dataLoading = true,
                listOfBleDevices = listOf()
            )
        }
        val bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (!scanning && hasPermissions(
                this,
                permissions
            ) && bluetoothAdapter?.isEnabled == true
        ) { // Stops scanning after a pre-defined scan period.
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
}

@Immutable
data class BleViewState(
    val viewHeader: String = "Bluetooth Devices Scanner",
    val dataLoading: Boolean = false,
    val listOfBleDevices: List<BleDeviceWrapper> = listOf()
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



