package com.example.memogotchi.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.ResolveTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object NearbyRssiRepository {
    private val _rssiByToken = MutableStateFlow<Map<String, Int>>(emptyMap())
    val rssiByToken: StateFlow<Map<String, Int>> = _rssiByToken

    fun update(tokenHex: String, rssi: Int) {
        _rssiByToken.update { it + (tokenHex to rssi) }
    }

    fun getRssi(tokenHex: String): Int? = _rssiByToken.value[tokenHex]
}

class BleScanner(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMatched: (matchId: String) -> Unit = {}
) {
    private val tag = "BleScanner"
    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false

    private val recentlyResolvedTokens = mutableSetOf<String>()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleResult(it) }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "Scan failed, error=$errorCode")
        }
    }

    private fun handleResult(result: ScanResult) {
        val serviceData = result.scanRecord?.getServiceData(BleConstants.SERVICE_PARCEL_UUID) ?: return
        val tokenHex = bytesToHexString(serviceData)
        NearbyRssiRepository.update(tokenHex, result.rssi)

        if (recentlyResolvedTokens.contains(tokenHex)) return
        recentlyResolvedTokens.add(tokenHex)

        scope.launch {
            try {
                val response = ApiClient.service.resolveNearbyToken(ResolveTokenRequest(tokenHex))
                if (response.matched) {
                    Log.d(tag, "Matched! matchId=${response.matchId}")
                    response.matchId?.let(onMatched)
                } else {
                    Log.d(tag, "No match for scanned token: ${response.reason}")
                }
            } catch (e: Exception) {
                Log.e(tag, "resolveNearbyToken failed", e)
            } finally {
                delay(30_000L)
                recentlyResolvedTokens.remove(tokenHex)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start(mode: BleRadioMode = BleRadioMode.LOW_LATENCY) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(tag, "Bluetooth unavailable or disabled, cannot scan")
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(tag, "Device does not support BLE scanning")
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceData(
                BleConstants.SERVICE_PARCEL_UUID,
                ByteArray(BleConstants.TOKEN_BYTE_LENGTH),
                ByteArray(BleConstants.TOKEN_BYTE_LENGTH)
            )
            .build()

        val scanMode = when (mode) {
            BleRadioMode.LOW_LATENCY -> ScanSettings.SCAN_MODE_LOW_LATENCY
            BleRadioMode.LOW_POWER -> ScanSettings.SCAN_MODE_BALANCED
        }
        val settings = ScanSettings.Builder()
            .setScanMode(scanMode)
            .build()

        stop()
        scanner?.startScan(listOf(filter), settings, callback)
        isScanning = true
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (isScanning) {
            scanner?.stopScan(callback)
            isScanning = false
        }
    }
}