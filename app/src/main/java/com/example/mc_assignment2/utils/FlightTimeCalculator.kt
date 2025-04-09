package com.example.mc_assignment2.utils

import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Utility object providing helper functions for calculating and formatting flight times and delays.
 */
object FlightTimeCalculator {

    /**
     * Calculates the duration between two Date objects in minutes.
     *
     * @param startTime The start time of the flight.
     * @param endTime The end time of the flight.
     * @return Duration in minutes.
     */
    fun calculateDurationInMinutes(startTime: Date, endTime: Date): Int {
        val durationMillis = endTime.time - startTime.time
        return TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()
    }

    /**
     * Formats a given duration in minutes into a human-readable string.
     * Example: 90 minutes → "1h 30m"
     *
     * @param minutes Total duration in minutes.
     * @return A formatted string representing hours and minutes.
     */
    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return if (hours > 0) {
            "${hours}h ${remainingMinutes}m"
        } else {
            "${remainingMinutes}m"
        }
    }

    /**
     * Converts delay time into a readable status string.
     * Example: 0 → "On time", 25 → "+25m"
     *
     * @param delay Delay in minutes (nullable).
     * @return A status string based on the delay.
     */
    fun formatDelay(delay: Int?): String {
        return when {
            delay == null -> "No delay"
            delay <= 0 -> "On time"
            else -> "+${formatDuration(delay)}"
        }
    }

    /**
     * Computes an adjusted flight duration accounting for delays.
     * Adds the net delay difference between arrival and departure to the scheduled duration.
     *
     * @param scheduledDuration Scheduled flight duration in minutes.
     * @param departureDelay Delay at departure in minutes (nullable).
     * @param arrivalDelay Delay at arrival in minutes (nullable).
     * @return Adjusted duration in minutes.
     */
    fun getAverageFlightTimeWithDelays(
        scheduledDuration: Int,
        departureDelay: Int?,
        arrivalDelay: Int?
    ): Int {
        val actualDepartureDelay = departureDelay ?: 0
        val actualArrivalDelay = arrivalDelay ?: 0

        return scheduledDuration + (actualArrivalDelay - actualDepartureDelay)
    }
}
