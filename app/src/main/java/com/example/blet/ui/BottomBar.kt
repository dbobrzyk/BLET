package com.example.blet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.blet.R
import com.example.blet.ui.theme.darkBlue
import com.example.blet.ui.theme.lightBlue

@Composable
fun BottomBar(pageSelected: Int, selectPage: (Int) -> Unit) {

    Row(
        modifier = Modifier
            .background(darkBlue)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("bottom0")
                .clickable {
                    selectPage.invoke(0)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BottomButton(
                0,
                R.drawable.ic_bluetooth,
                "BLE list",
                pageSelected
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("bottom1")
                .clickable {
                    selectPage.invoke(1)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BottomButton(
                1,
                R.drawable.ic_map,
               "Map",
                pageSelected
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("bottom2")
                .clickable {
                    selectPage.invoke(2)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BottomButton(
                2,
               R.drawable.ic_search,
                "Search device",
                pageSelected
            )
        }
    }
}

@Composable
fun BottomButton(
    index: Int,
    drawableId: Int?,
    buttonName: String,
    pageSelected: Int
) {

    drawableId?.let{
        Icon(
            painter = painterResource(
                id = drawableId
            ),
            contentDescription = "document",
            modifier = Modifier
                .padding(top = 8.dp)
                .size(width = 30.dp, height = 30.dp),
            tint =
            if (index == pageSelected)
                Color.White
            else
                lightBlue,
        )
    }
    Text(
        text = buttonName,
        style = MaterialTheme.typography.body2,
        color =
        if (index == pageSelected)
            Color.White
        else
            lightBlue,
        modifier = Modifier.padding(bottom = 8.dp)
    )

}