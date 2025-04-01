package com.example.mc_assignment2.utils

import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Utility class for flight time calculations
 */
object FlightTimeCalculator {

    /**
     * Calculate the duration between two time points in minutes
     */
    fun calculateDurationInMinutes(startTime: Date, endTime: Date): Int {
        val durationMillis = endTime.time - startTime.time
        return TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()
    }

    /**
     * Format duration in minutes to a human-readable string (e.g., "5h 30m")
     */
    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            else -> "${remainingMinutes}m"
        }
    }

    /**
     * Format delay information for display
     */
    fun formatDelay(delay: Int?): String {
        return when {
            delay == null -> "No delay"
            delay <= 0 -> "On time"
            else -> "+${formatDuration(delay)}"
        }
    }

    /**
     * Calculate the actual flight time with delays
     * This accounts for both departure and arrival delays
     */
    fun getAverageFlightTimeWithDelays(
        scheduledDuration: Int,
        departureDelay: Int?,
        arrivalDelay: Int?
    ): Int {
        val actualDepartureDelay = departureDelay ?: 0
        val actualArrivalDelay = arrivalDelay ?: 0

        // If arrival delay is greater than departure delay, the flight took longer
        return scheduledDuration + (actualArrivalDelay - actualDepartureDelay)
    }
}