package com.example.mc_assignment2.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

private const val TAG = "FlightDataCollection"

/**
 * Background service to initialize flight data collection
 */
class FlightDataCollectionService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Flight data collection service started")
        setupRecurringWork()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Schedule the worker to run 3 times per day (every 8 hours)
        val workRequest = PeriodicWorkRequestBuilder<FlightDataWorker>(
            8, TimeUnit.HOURS, // Run every 8 hours
            30, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "flight_data_collection",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

        Log.d(TAG, "Scheduled periodic work")
    }
}