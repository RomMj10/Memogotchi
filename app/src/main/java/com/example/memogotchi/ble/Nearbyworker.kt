package com.example.memogotchi.ble

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

/**
 * Passive nearby mode: instead of scanning continuously (which drains
 * battery), this runs a short advertise+scan burst roughly every 15
 * minutes via WorkManager's periodic scheduling.
 */
class NearbyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "nearby_passive_scan"
        private const val BURST_DURATION_MS = 15_000L
    }

    override suspend fun doWork(): Result {
        val token = NearbyTokenManager.ensureFreshToken() ?: return Result.retry()

        val advertiser = BleAdvertiser(applicationContext)
        val scanner = BleScanner(applicationContext, CoroutineScope(coroutineContext))

        advertiser.start(token)
        scanner.start()

        delay(BURST_DURATION_MS)

        scanner.stop()
        advertiser.stop()

        return Result.success()
    }
}