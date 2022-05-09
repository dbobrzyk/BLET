package com.example.blet.util

object DistanceUtil {

    fun getDistanceFromDevice(rsii: Int, txPower: Int): Double {
        if (txPower == 127) {
            return when (rsii) {
                in 0 downTo -22 -> {
                    0.0
                }
                in -23 downTo -40 -> {
                    0.1
                }
                in -41 downTo -50 -> {
                    0.3
                }
                in -51 downTo -60 -> {
                    0.75
                }
                in -61 downTo -70 -> {
                    1.0
                }
                in -71 downTo -78 -> {
                    1.5
                }
                in -79 downTo -82 -> {
                    2.0
                }
                in -83 downTo -86 -> {
                    3.0
                }
                in -87 downTo -91 -> {
                    4.0
                }
                else -> {
                    5.0
                }
            }
        }
        return -1.0
    }
}