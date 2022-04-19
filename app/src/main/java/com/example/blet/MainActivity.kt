package com.example.blet

import android.Manifest
import android.annotation.SuppressLint
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
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            super.onScanResult(callbackType, result)
            Log.d("BLE", "Scanning result: $result")
            val newList = viewState.value.listOfBleDevices + BleDeviceWrapper(scanResult = result)
            _viewState.update { currentState ->
                currentState.copy(
                    viewHeader = "Scanning results:",
                    listOfBleDevices = newList,
                    dataLoading = false
                )
            }
        }
    }
    private var scanning = false
    private val handler = Handler()
    private val SCAN_PERIOD: Long = 10000

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
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .shadow(5.dp, RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
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
                                        Text(
                                            text = "Device id: ${item.scanResult.device} \nDevice name: ${item.scanResult.device.name}",
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                    if (item.isExpanded) {
                                        Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "${item.scanResult}",
                                                modifier = Modifier
                                                    .padding(16.dp)
                                                    .fillMaxWidth()
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
)



