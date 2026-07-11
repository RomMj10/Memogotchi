package com.example.memogotchi.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log

/**
 * Caller is responsible for confirming BLUETOOTH_ADVERTISE (API 31+) or
 * having Bluetooth otherwise usable before calling start() — this class
 * does not request permissions itself (see NearbyOptInScreen for that).
 */
class BleAdvertiser(private val context: Context) {
    private val tag = "BleAdvertiser"
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    private val callback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            Log.d(tag, "Advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            Log.e(tag, "Advertising failed to start, error=$errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun start(tokenHex: String) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(tag, "Bluetooth unavailable or disabled, cannot advertise")
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(tag, "Device does not support BLE advertising")
            return
        }

        val tokenBytes = hexStringToBytes(tokenHex)
        if (tokenBytes.size != BleConstants.TOKEN_BYTE_LENGTH) {
            Log.w(tag, "Unexpected token byte length: ${tokenBytes.size}, expected ${BleConstants.TOKEN_BYTE_LENGTH}")
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        // Service Data (128-bit UUID) carries both our identifying UUID and
        // the token in ONE AD structure — deliberately no separate
        // addServiceUuid() call, which would duplicate the UUID bytes and
        // blow the 31-byte legacy advertising budget.
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceData(BleConstants.SERVICE_PARCEL_UUID, tokenBytes)
            .build()

        stop() // avoid stacking duplicate advertise sets on repeated calls
        advertiser?.startAdvertising(settings, data, callback)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (isAdvertising) {
            advertiser?.stopAdvertising(callback)
            isAdvertising = false
        }
    }
}