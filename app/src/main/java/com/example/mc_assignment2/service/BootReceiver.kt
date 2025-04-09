package com.example.mc_assignment2.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "FlightDataCollection"

/**
 * BroadcastReceiver that listens for device boot completion.
 * Automatically restarts the [FlightDataCollectionService] after reboot,
 * ensuring that background data collection continues without manual intervention.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Create an intent to start the background flight data collection service
            val serviceIntent = Intent(context, FlightDataCollectionService::class.java)

            // Start the service (requires appropriate permissions in AndroidManifest)
            context.startService(serviceIntent)
            Log.d(TAG, "Starting service after device boot")
        }
    }
}
