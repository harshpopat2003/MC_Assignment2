package com.example.mc_assignment2.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.*

/**
 * Entity representing a flight in the database
 */
@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flightnumber: String,
    val flightdate: Date,
    val departureairport: String,
    val arrivalairport: String,
    val scheduleddeparture: Date,
    val actualdeparture: Date?,
    val scheduledarrival: Date,
    val actualarrival: Date?,
    val departuredelay: Int?, // In minutes
    val arrivaldelay: Int?,   // In minutes
    val scheduledduration: Int, // In minutes
    val actualduration: Int?, // In minutes
    val flightstatus: String,
    val datacollectiontime: Date = Date()
)

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}