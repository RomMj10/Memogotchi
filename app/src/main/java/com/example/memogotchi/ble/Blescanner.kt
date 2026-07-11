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

/**
 * In-memory "last known RSSI per scanned token" store, exposed as a
 * StateFlow so Compose screens (e.g. Step 6's CircleMapScreen) can
 * observe it reactively via collectAsState() instead of polling.
 * BleScanner updates this on every scan result.
 */
object NearbyRssiRepository {
    private val _rssiByToken = MutableStateFlow<Map<String, Int>>(emptyMap())
    val rssiByToken: StateFlow<Map<String, Int>> = _rssiByToken

    fun update(tokenHex: String, rssi: Int) {
        _rssiByToken.update { it + (tokenHex to rssi) }
    }

    fun getRssi(tokenHex: String): Int? = _rssiByToken.value[tokenHex]
}

/**
 * Caller is responsible for confirming BLUETOOTH_SCAN (API 31+) or
 * ACCESS_FINE_LOCATION (pre-API 31) before calling start().
 *
 * @param onMatched invoked with matchId whenever resolveNearbyToken returns
 *   a genuine match. Note the push notification is already sent server-side
 *   at that point — this callback is mainly for local logging/testing until
 *   Step 5/6 give it something more to do.
 */
class BleScanner(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMatched: (matchId: String) -> Unit = {}
) {
    private val tag = "BleScanner"
    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false

    // Prevents hammering the API with a resolve call for every single scan
    // callback of the same token — BLE devices rebroadcast frequently.
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
                // Allow re-resolution after a short window in case this
                // attempt failed transiently (e.g. a network blip).
                delay(30_000L)
                recentlyResolvedTokens.remove(tokenHex)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
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
            .setServiceUuid(BleConstants.SERVICE_PARCEL_UUID)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
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