package com.example.mc_assignment2.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

private const val TAG = "FlightDataCollection"

/**
 * Foreground-less Android Service responsible for initializing periodic
 * flight data collection using WorkManager.
 */
class FlightDataCollectionService : Service() {

    /**
     * Called when the service is started.
     * Schedules the recurring background work for collecting flight data.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Flight data collection service started")
        setupRecurringWork()
        return START_STICKY // Ensures the service is restarted if terminated by the system
    }

    /**
     * Binding not used in this service.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Sets up a periodic background worker using WorkManager to collect
     * flight data every 8 hours.
     */
    private fun setupRecurringWork() {
        // Define network constraint: only run when connected to the internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Define the periodic work request (every 8 hours with a 30 min flex interval)
        val workRequest = PeriodicWorkRequestBuilder<FlightDataWorker>(
            8, TimeUnit.HOURS,             // Run every 8 hours
            30, TimeUnit.MINUTES           // Flex: Worker can execute within this additional window
        )
            .setConstraints(constraints)
            .build()

        // Enqueue unique periodic work to avoid duplicate scheduling
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "flight_data_collection",             // Unique work name
                ExistingPeriodicWorkPolicy.KEEP,      // Keep existing schedule if already running
                workRequest
            )

        Log.d(TAG, "Scheduled periodic work")
    }
}
