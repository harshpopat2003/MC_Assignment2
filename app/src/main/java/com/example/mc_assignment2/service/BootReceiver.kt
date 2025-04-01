package com.example.mc_assignment2.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "FlightDataCollection"

/**
 * Auto-start receiver to handle device reboots
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, FlightDataCollectionService::class.java)
            context.startService(serviceIntent)
            Log.d(TAG, "Starting service after device boot")
        }
    }
}