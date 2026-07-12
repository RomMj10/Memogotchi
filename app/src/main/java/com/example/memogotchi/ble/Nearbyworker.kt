package com.example.memogotchi.ble

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

class NearbyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "nearby_passive_scan"
        private const val BURST_DURATION_MS = 15_000L
    }

    override suspend fun doWork(): Result {
        val token = NearbyTokenManager.ensureFreshToken() ?: return Result.retry()

        val advertiser = BleAdvertiser(applicationContext)
        val scanner = BleScanner(applicationContext, CoroutineScope(coroutineContext))

        return try {
            advertiser.start(token, BleRadioMode.LOW_POWER)
            scanner.start(BleRadioMode.LOW_POWER)
            delay(BURST_DURATION_MS)
            Result.success()
        } finally {
            scanner.stop()
            advertiser.stop()
        }
    }
}