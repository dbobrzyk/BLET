package com.example.blet.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager


class MyBleManager(context: Context) : BleManager(context) {

    override fun getGattCallback(): BleManagerGattCallback {
        return MyGattCallbackImpl()
    }

    private class MyGattCallbackImpl : BleManagerGattCallback() {
        @SuppressLint("MissingPermission")
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            gatt.services.forEach {
                it.characteristics.forEach { characteristic ->
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true)
                        characteristic.descriptors?.forEach {
                            Log.d("NORDIC", "Descriptor value: ${it.value}")
                        }
                    }
                }
            }
            return true
        }

        override fun onServicesInvalidated() {
            Log.d("NORDIC", "Services invalidates")
        }
    }
}