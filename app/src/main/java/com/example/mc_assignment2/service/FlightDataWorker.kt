package com.example.mc_assignment2.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mc_assignment2.api.FlightApiService
import com.example.mc_assignment2.database.FlightDatabase
import com.example.mc_assignment2.database.FlightEntity
import com.example.mc_assignment2.utils.FlightTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "FlightDataCollection"

/**
 * Worker that performs the actual flight data collection
 */
class FlightDataWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Sample routes to monitor
    private val routes = listOf(
        "JFK" to "LAX",
        "LAX" to "SFO",
        "ORD" to "MIA",
        "ATL" to "DEN",
        "DFW" to "SEA"
    )

    private val apiService = Retrofit.Builder()
        .baseUrl(FlightApiService.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FlightApiService::class.java)

    private val database = FlightDatabase.getDatabase(appContext)
    private val flightDao = database.flightDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting flight data collection task")

            // For each route, collect at least 3 flights
            for ((departure, arrival) in routes) {
                Log.d(TAG, "Collecting data for route: $departure to $arrival")

                val response = apiService.getFlightsByRoute(
                    departureAirport = departure,
                    arrivalAirport = arrival
                )

                if (response.isSuccessful) {
                    val flightData = response.body()?.data ?: emptyList()
                    Log.d(TAG, "Found ${flightData.size} flights for route $departure to $arrival")

                    // Take at most 3 flights per route
                    val flightsToProcess = flightData.take(3)

                    for (flight in flightsToProcess) {
                        val defaultDate = Date()

                        val scheduledDeparture = flight.departure.scheduled?.let { dateFormat.parse(it) } ?: defaultDate
                        val actualDeparture = flight.departure.actual?.let { dateFormat.parse(it) }
                        val scheduledArrival = flight.arrival.scheduled?.let { dateFormat.parse(it) } ?: defaultDate
                        val actualArrival = flight.arrival.actual?.let { dateFormat.parse(it) }

//                        val scheduledDeparture = dateFormat.parse(flight.departure.scheduled)
//                        val actualDeparture = flight.departure.actual?.let { dateFormat.parse(it) }
//                        val scheduledArrival = dateFormat.parse(flight.arrival.scheduled)
//                        val actualArrival = flight.arrival.actual?.let { dateFormat.parse(it) }

                        val scheduledDuration = FlightTimeCalculator.calculateDurationInMinutes(
                            scheduledDeparture, scheduledArrival
                        )

                        // Calculate actual duration with delays
                        val actualDuration = if (actualDeparture != null && actualArrival != null) {
                            FlightTimeCalculator.calculateDurationInMinutes(
                                actualDeparture, actualArrival
                            )
                        } else {
                            null
                        }

                        val flightEntity = FlightEntity(
                            flightnumber = flight.flight.iata,
                            flightdate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(flight.flight_date) ?: Date(),
//                        flightdate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(flight.flight_date),
                            departureairport = flight.departure.iata,
                            arrivalairport = flight.arrival.iata,
                            scheduleddeparture = scheduledDeparture,
                            actualdeparture = actualDeparture,
                            scheduledarrival = scheduledArrival,
                            actualarrival = actualArrival,
                            departuredelay = flight.departure.delay,
                            arrivaldelay = flight.arrival.delay,
                            scheduledduration = scheduledDuration,
                            actualduration = actualDuration,
                            flightstatus = flight.flight_status ?: "unknown"
                        )

                        flightDao.insertFlight(flightEntity)
                        Log.d(TAG, "Inserted flight ${flight.flight.iata} into database")
                    }
                } else {
                    Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                }
            }

            Log.d(TAG, "Flight data collection task completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in flight data collection: ${e.message}", e)
            Result.retry()
        }
    }
}