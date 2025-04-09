package com.example.mc_assignment2.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mc_assignment2.api.FlightApiService
import com.example.mc_assignment2.api.FlightData
import com.example.mc_assignment2.database.FlightDatabase
import com.example.mc_assignment2.database.FlightEntity
import com.example.mc_assignment2.utils.FlightTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "FlightDataCollection"

/**
 * Worker that performs periodic flight data collection using Retrofit and Room.
 * It either fetches real data from the AviationStack API or generates fake data for testing.
 */
class FlightDataWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Predefined routes to monitor and fetch data for
    private val routes = listOf(
        "JFK" to "LAX",
        "LAX" to "SFO",
        "ORD" to "MIA",
        "ATL" to "DEN",
        "DFW" to "SEA"
    )

    // Retrofit setup for API calls
    private val apiService = Retrofit.Builder()
        .baseUrl(FlightApiService.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FlightApiService::class.java)

    // Room database access
    private val database = FlightDatabase.getDatabase(appContext)
    private val flightDao = database.flightDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    /**
     * Executes the background task. For each route, attempts to fetch data via API.
     * If API fails or returns no data, generates fake flight data.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting flight data collection task")

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val startDate = calendar.time
            val endDate = Date()

            for ((departure, arrival) in routes) {
                Log.d(TAG, "Collecting data for route: $departure to $arrival")

                val response = apiService.getFlightsByRoute(
                    departureAirport = departure,
                    arrivalAirport = arrival
                )

                val flightData = if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else {
                    Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                    emptyList()
                }

                if (flightData.isEmpty()) {
                    Log.d(TAG, "No data found for route $departure to $arrival. Generating fake data.")
                    generateFakeData(departure, arrival, startDate, endDate)
                } else {
                    processFlightData(flightData)
                }
            }

            Log.d(TAG, "Flight data collection task completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in flight data collection: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * Processes API response and inserts valid flight data into the database.
     * Limits processing to 3 entries per route to avoid overloading the DB.
     */
    private suspend fun processFlightData(flightData: List<FlightData>) {
        for (flight in flightData.take(3)) {
            val defaultDate = Date()

            val scheduledDeparture = flight.departure.scheduled?.let { dateFormat.parse(it) } ?: defaultDate
            val actualDeparture = flight.departure.actual?.let { dateFormat.parse(it) }
            val scheduledArrival = flight.arrival.scheduled?.let { dateFormat.parse(it) } ?: defaultDate
            val actualArrival = flight.arrival.actual?.let { dateFormat.parse(it) }

            val scheduledDuration = FlightTimeCalculator.calculateDurationInMinutes(
                scheduledDeparture, scheduledArrival
            )

            val actualDuration = if (actualDeparture != null && actualArrival != null) {
                FlightTimeCalculator.calculateDurationInMinutes(actualDeparture, actualArrival)
            } else null

            val flightEntity = FlightEntity(
                flightnumber = flight.flight.iata,
                flightdate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(flight.flight_date) ?: Date(),
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
    }

    /**
     * Generates and inserts synthetic flight data for the given route and time range.
     * Useful when API data is unavailable for the route.
     */
    private suspend fun generateFakeData(departure: String, arrival: String, startDate: Date, endDate: Date) {
        val random = Random()
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        while (calendar.time.before(endDate)) {
            val scheduledDeparture = calendar.time
            calendar.add(Calendar.HOUR, random.nextInt(6) + 1)
            val scheduledArrival = calendar.time

            val departuredelay = random.nextInt(30)
            val arrivaldelay = random.nextInt(30)

            val actualDeparture = Date(scheduledDeparture.time + TimeUnit.MINUTES.toMillis(departuredelay.toLong()))
            val actualArrival = Date(scheduledArrival.time + TimeUnit.MINUTES.toMillis(arrivaldelay.toLong()))

            val scheduledDuration = FlightTimeCalculator.calculateDurationInMinutes(scheduledDeparture, scheduledArrival)
            val actualDuration = FlightTimeCalculator.calculateDurationInMinutes(actualDeparture, actualArrival)

            val flightEntity = FlightEntity(
                flightnumber = "FAKE${random.nextInt(1000)}",
                flightdate = scheduledDeparture,
                departureairport = departure,
                arrivalairport = arrival,
                scheduleddeparture = scheduledDeparture,
                actualdeparture = actualDeparture,
                scheduledarrival = scheduledArrival,
                actualarrival = actualArrival,
                departuredelay = departuredelay,
                arrivaldelay = arrivaldelay,
                scheduledduration = scheduledDuration,
                actualduration = actualDuration,
                flightstatus = "fake"
            )

            flightDao.insertFlight(flightEntity)
            Log.d(TAG, "Inserted fake flight ${flightEntity.flightnumber} into database")
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}
