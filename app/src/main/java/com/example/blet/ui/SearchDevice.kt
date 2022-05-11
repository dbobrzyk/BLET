package com.example.blet.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.blet.BleViewState
import com.example.blet.ui.theme.lightBlue
import com.example.blet.ui.theme.lightestBlue

@SuppressLint("MissingPermission")
@Composable
fun SearchDeviceScreen(
    state: State<BleViewState>,
    innerPadding: PaddingValues
) {
    Surface {
        Column(
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
            Text(state.value.chosenDevice?.device?.name.toString() + "\n Strenght: ${state.value.chosenDevice?.rssi}")

        }
    }
}