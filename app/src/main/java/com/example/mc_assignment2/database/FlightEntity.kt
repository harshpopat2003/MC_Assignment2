package com.example.mc_assignment2.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.*

/**
 * Entity class representing a flight record stored in the local Room database.
 * Maps directly to a table named "flights".
 */
@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,          // Auto-generated unique ID for each entry
    val flightnumber: String,                                   // IATA flight number (e.g., AI202)
    val flightdate: Date,                                       // Scheduled date of the flight
    val departureairport: String,                               // Departure airport IATA code
    val arrivalairport: String,                                 // Arrival airport IATA code
    val scheduleddeparture: Date,                               // Scheduled departure time
    val actualdeparture: Date?,                                 // Actual departure time (nullable)
    val scheduledarrival: Date,                                 // Scheduled arrival time
    val actualarrival: Date?,                                   // Actual arrival time (nullable)
    val departuredelay: Int?,                                   // Delay at departure (in minutes, nullable)
    val arrivaldelay: Int?,                                     // Delay at arrival (in minutes, nullable)
    val scheduledduration: Int,                                 // Scheduled flight duration in minutes
    val actualduration: Int?,                                   // Actual duration in minutes (nullable)
    val flightstatus: String,                                   // Status (e.g., landed, cancelled, scheduled)
    val datacollectiontime: Date = Date()                       // Timestamp when this data was recorded
)

/**
 * Type converters for Room to handle non-primitive data types.
 * Room doesn't support Date natively, so we convert it to/from Long.
 */
class Converters {
    /**
     * Converts a timestamp (Long) to a Date object.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Converts a Date object to a timestamp (Long).
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
