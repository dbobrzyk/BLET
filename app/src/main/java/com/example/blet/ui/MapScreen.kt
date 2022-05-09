package com.example.blet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.blet.BleViewState
import com.example.blet.ViewType
import com.example.blet.ui.theme.lightBlue
import com.example.blet.ui.theme.lightestBlue

@Composable
fun MapScreen(
    state: State<BleViewState>,
    changeViewState: (BleViewState) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
    ) {
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
                .padding(16.dp)
        ) {
            Text("Ble devices List", textAlign = TextAlign.Center, modifier = Modifier
                .clickable {
                    changeViewState(
                        state.value.copy(
                            view = ViewType.BLE_LIST
                        )
                    )
                }
                .align(Alignment.CenterHorizontally)
                .background(Color.Red)
                .fillMaxWidth()
                .padding(16.dp))
            for (i in 1..10)
                Text("MAPA HERE")
        }

    }
}