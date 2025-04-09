package com.example.mc_assignment2.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Data Access Object for flight database operations
 */
@Dao
interface FlightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: FlightEntity)

    @Query("SELECT * FROM flights WHERE flightnumber = :flightNumber AND flightdate = :date")
    suspend fun getFlightByNumberAndDate(flightNumber: String, date: Date): FlightEntity?

    @Query("SELECT * FROM flights WHERE departureairport = :departureAirport AND arrivalairport = :arrivalAirport ORDER BY flightdate DESC")
    fun getFlightsByRoute(departureAirport: String, arrivalAirport: String): Flow<List<FlightEntity>>

    @Query("SELECT AVG(actualduration) FROM flights WHERE departureairport = :departureAirport AND arrivalairport = :arrivalAirport AND actualduration > 0")
    suspend fun getAverageFlightDuration(departureAirport: String, arrivalAirport: String): Double

    @Query("SELECT AVG(actualduration) + AVG(departuredelay) + AVG(arrivaldelay) FROM flights WHERE departureairport = :departureAirport AND arrivalairport = :arrivalAirport")
    suspend fun getAverageTimeTakenWithDelays(departureAirport: String, arrivalAirport: String): Double?

    @Query("SELECT * FROM flights GROUP BY departureairport, arrivalairport")
    fun getAllRoutes(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE flightdate BETWEEN :startDate AND :endDate")
    suspend fun getFlightsBetweenDates(startDate: Date, endDate: Date): List<FlightEntity>

    @Query("DELETE FROM flights")
    suspend fun clearFlights()

    @Query("SELECT departureairport AS departureAirport, arrivalairport AS arrivalAirport, " +
           "AVG(actualduration + departuredelay + arrivaldelay) AS averageDuration " +
           "FROM flights GROUP BY departureairport, arrivalairport")
    fun getRouteStatistics(): Flow<List<RouteStatisticsResult>>
}

data class RouteStatisticsResult(
    val departureAirport: String,
    val arrivalAirport: String,
    val averageDuration: Double
)
