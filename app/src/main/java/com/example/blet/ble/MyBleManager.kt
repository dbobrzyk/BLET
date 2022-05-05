package com.example.blet.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.util.Log
import java.util.UUID
import no.nordicsemi.android.ble.BleManager


class MyBleManager(context: Context) : BleManager(context) {

    override fun getGattCallback(): BleManagerGattCallback {
        return MyGattCallbackImpl()
    }

    private class MyGattCallbackImpl : BleManagerGattCallback() {
        @SuppressLint("MissingPermission")
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            Log.d("NORDIC", "Got gatt")
            gatt.services.forEach {
//                Log.d("NORDIC", "Service found $it")
//                if (it.uuid.toString().contains("1801")) {

//                    Log.d("NORDIC", "Service with 1801 found, characteristic: ${characteristic}")

                it.characteristics.forEach { characteristic ->
                    if (characteristic != null) {
                        Log.d("NORDIC", "Characteristic value: ${characteristic.value}")
                        gatt.setCharacteristicNotification(characteristic, true)
                        characteristic.descriptors?.forEach {
//                            Log.d("NORDIC", "Descriptor: ${it}")
                            Log.d("NORDIC", "Descriptor value: ${it.value}")
                        }
                    }

//                }
                }
            }


            return true
        }


        override fun onServicesInvalidated() {
            Log.d("NORDIC", "Services invalidates")
        }

    }

}