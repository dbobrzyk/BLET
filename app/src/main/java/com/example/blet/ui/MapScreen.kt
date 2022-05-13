package com.example.blet.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.blet.BleViewState
import com.example.blet.ui.theme.darkBlue
import com.example.blet.ui.theme.lightBlue
import com.example.blet.ui.theme.lightestBlue
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(
    state: BleViewState
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
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)
        ) {
            val singapore = LatLng(1.35, 103.87)
            val point = LatLng(
                state.location?.latitude ?: singapore.latitude,
                state.location?.longitude ?: singapore.longitude
            )
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(point, 20f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                state.listOfMarkers.forEach {
                    Log.d("MARKER", "ADDING MARKER with distance ${it.distance}")
                    Circle(
                        center = LatLng(it.location.latitude, it.location.longitude),
                        radius = it.distance,
                        strokeColor = darkBlue,
                        strokeWidth = 3.0f
                    )
                }
            }
        }
    }
}

