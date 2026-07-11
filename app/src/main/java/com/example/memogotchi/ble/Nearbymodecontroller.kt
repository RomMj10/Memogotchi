package com.example.memogotchi.ble

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.memogotchi.ui.page.NearbyMode
import java.util.concurrent.TimeUnit

/**
 * Single entry point for switching nearby mode on/off — called from
 * NearbyOptInScreen whenever the user changes their toggle/button choice.
 */
object NearbyModeController {

    fun applyMode(context: Context, mode: NearbyMode) {
        when (mode) {
            NearbyMode.OFF -> {
                stopActiveService(context)
                cancelPassiveWork(context)
            }
            NearbyMode.PASSIVE -> {
                stopActiveService(context)
                schedulePassiveWork(context)
            }
            NearbyMode.ACTIVE -> {
                cancelPassiveWork(context)
                startActiveService(context)
            }
        }
    }

    private fun startActiveService(context: Context) {
        val intent = Intent(context, NearbyForegroundService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopActiveService(context: Context) {
        context.stopService(Intent(context, NearbyForegroundService::class.java))
    }

    private fun schedulePassiveWork(context: Context) {
        val request = PeriodicWorkRequestBuilder<NearbyWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NearbyWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun cancelPassiveWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NearbyWorker.WORK_NAME)
    }
}