package com.example.blet.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import com.example.blet.BleDeviceWrapper
import com.example.blet.BleViewState
import com.example.blet.R
import com.example.blet.ui.theme.darkBlue
import com.example.blet.ui.theme.lightBlue
import com.example.blet.ui.theme.lightestBlue
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@SuppressLint("MissingPermission")
@Composable
fun BleListScreen(
    context: Context,
    state: State<BleViewState>,
    scanning: () -> Boolean,
    startScan: () -> Unit,
    changeViewState: (BleViewState) -> Unit,
    connectToDevice: (BleDeviceWrapper) -> Unit,
    addMarker: (BleDeviceWrapper) -> Unit,
    searchForDevice: (BleDeviceWrapper) -> Unit,
    padding: PaddingValues
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(state.value.dataLoading && scanning()),
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
                    .padding(padding)
            ) {
                item {
                    Column {

                        Text("Localization \nLatitude: ${state.value.location?.latitude}\nLongitude: ${state.value.location?.longitude}")

                        Text(
                            state.value.viewHeader,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 48.dp, bottom = 16.dp)
                        )
                    }
                }
                items(state.value.listOfBleDevices.size) { index ->
                    val item = state.value.listOfBleDevices[index]
                    val isMyMiBand = false
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .shadow(5.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isMyMiBand)
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
                                changeViewState(
                                    state.value.copy(
                                        listOfBleDevices = newList
                                    )
                                )
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
                                Text(text = "Signal strenght: ${item.scanResult.rssi} dBm [rssi]")
                                Text(text = "Signal power: ${item.scanResult.txPower} dBm [txpower]")
                                Text(text = "Connectable: ${item.scanResult.isConnectable}")
                                Row{
                                    Text(
                                        color = Color.White,
                                        text = "Add marker",
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(darkBlue)
                                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                                            .clickable {
                                                addMarker(item)
                                            }
                                    )
                                    Text(
                                        color = Color.White,
                                        text = "Connect",
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(darkBlue)
                                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                                            .clickable {
                                                connectToDevice(item)
                                            }
                                    )
                                    Text(
                                        color = Color.White,
                                        text = "Search",
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(darkBlue)
                                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                                            .clickable {
                                                searchForDevice(item)
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