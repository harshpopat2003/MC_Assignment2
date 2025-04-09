package com.example.mc_assignment2.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Data Access Object (DAO) for flight-related database operations.
 * Provides methods to insert, query, and delete flight data.
 */
@Dao
interface FlightDao {

    /**
     * Inserts a new flight record.
     * If a flight with the same primary key exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: FlightEntity)

    /**
     * Fetch a specific flight based on flight number and flight date.
     */
    @Query("SELECT * FROM flights WHERE flightnumber = :flightNumber AND flightdate = :date")
    suspend fun getFlightByNumberAndDate(flightNumber: String, date: Date): FlightEntity?

    /**
     * Fetch a list of flights between two airports, sorted by date (most recent first).
     * Emits results reactively using Flow.
     */
    @Query("SELECT * FROM flights WHERE departureairport = :departureAirport AND arrivalairport = :arrivalAirport ORDER BY flightdate DESC")
    fun getFlightsByRoute(departureAirport: String, arrivalAirport: String): Flow<List<FlightEntity>>

    /**
     * Calculates the average actual flight duration between two airports.
     * Ignores flights with zero or null duration.
     */
    @Query("SELECT AVG(actualduration) FROM flights WHERE departureairport = :departureAirport AND arrivalairport = :arrivalAirport AND actualduration > 0")
    suspend fun getAverageFlightDuration(departureAirport: String, arrivalAirport: String): Double

    /**
     * Calculates average total time including actual duration, departure delay, and arrival delay.
     * Returns null if no matching flights exist.
     */
    @Query("SELECT AVG(actualduration) + AVG(departuredelay) + AVG(arrivaldelay) FROM flights WHERE departureairport = :departureAirport AND arrivalairport = :arrivalAirport")
    suspend fun getAverageTimeTakenWithDelays(departureAirport: String, arrivalAirport: String): Double?

    /**
     * Returns one flight per route (grouped by departure and arrival airport).
     * Useful for getting a list of all routes flown.
     */
    @Query("SELECT * FROM flights GROUP BY departureairport, arrivalairport")
    fun getAllRoutes(): Flow<List<FlightEntity>>

    /**
     * Fetch all flights that took place between two given dates (inclusive).
     */
    @Query("SELECT * FROM flights WHERE flightdate BETWEEN :startDate AND :endDate")
    suspend fun getFlightsBetweenDates(startDate: Date, endDate: Date): List<FlightEntity>

    /**
     * Deletes all flight records from the database.
     */
    @Query("DELETE FROM flights")
    suspend fun clearFlights()

    /**
     * Calculates average total duration (including delays) grouped by route.
     * Emits results reactively as a Flow.
     */
    @Query("SELECT departureairport AS departureAirport, arrivalairport AS arrivalAirport, " +
            "AVG(actualduration + departuredelay + arrivaldelay) AS averageDuration " +
            "FROM flights GROUP BY departureairport, arrivalairport")
    fun getRouteStatistics(): Flow<List<RouteStatisticsResult>>
}

/**
 * Data class used to represent statistics about a route.
 */
data class RouteStatisticsResult(
    val departureAirport: String,
    val arrivalAirport: String,
    val averageDuration: Double
)
