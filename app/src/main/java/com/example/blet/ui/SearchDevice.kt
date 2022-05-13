package com.example.blet.ui

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.blet.BleViewState
import com.example.blet.R
import com.example.blet.ui.theme.lightBlue
import com.example.blet.ui.theme.lightestBlue
import com.example.blet.util.DistanceUtil

@SuppressLint("MissingPermission")
@Composable
fun SearchDeviceScreen(
    state: BleViewState,
    innerPadding: PaddingValues
) {
    val device = state.chosenDevice
    Surface {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
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
                .padding(innerPadding)
        ) {
            if (device != null) {

                Image(
                    painter = painterResource(id = R.drawable.ic_bluetooth),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 56.dp)
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    state.chosenDevice.device?.name.toString() + "\n\n Strenght: ${device.rssi}",
                    modifier = Modifier.padding(8.dp)
                )
                RsiiPower(
                    statValue = (device.rssi + 165),
                    modifier = Modifier.padding(16.dp)
                )

                device.rssi.let {
                    val distance = DistanceUtil.getDistanceFromDevice(it, device.txPower)
                    if (distance < 1.0)
                        Text("Distance: ~${(distance * 100).toInt()}cm")
                    else
                        Text("Distance: ~${distance}m")
                }
            } else {
                Text(
                    text = "Choose device to search from the BLE list first!",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 64.dp, start = 48.dp, end = 48.dp)

                )
            }

        }
    }
}

@Composable
fun RsiiPower(
    statValue: Int,
    statMaxValue: Int = 150,
    statColor: Color = lightBlue,
    height: Dp = 28.dp,
    animDuration: Int = 500,
    animDelay: Int = 0,
    modifier: Modifier
) {
    val animationPlayed = remember {
        mutableStateOf(false)
    }
    val curPercent = animateFloatAsState(
        targetValue = if (animationPlayed.value) {
            statValue / statMaxValue.toFloat()
        } else 0f,
        animationSpec = tween(
            animDuration,
            animDelay
        )
    )
    LaunchedEffect(key1 = true) {
        animationPlayed.value = true
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(
                if (isSystemInDarkTheme()) {
                    Color(0xFF505050)
                } else {
                    Color.LightGray
                }
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(curPercent.value)
                .clip(CircleShape)
                .background(statColor)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "RSII power",
                fontWeight = FontWeight.Bold
            )
            Text(
                text = (curPercent.value * statMaxValue).toInt().toString(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}