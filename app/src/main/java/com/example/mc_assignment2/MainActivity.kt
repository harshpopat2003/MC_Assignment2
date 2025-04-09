package com.example.mc_assignment2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mc_assignment2.database.FlightDatabase
import com.example.mc_assignment2.databinding.ActivityMainBinding
import com.example.mc_assignment2.databinding.ItemFlightStatBinding
import com.example.mc_assignment2.service.FlightDataCollectionService
import com.example.mc_assignment2.utils.FlightTimeCalculator
import com.example.mc_assignment2.utils.ValidationUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import com.example.mc_assignment2.database.FlightDao
import com.example.mc_assignment2.database.FlightEntity

/**
 * MainActivity serves as the entry point for the flight tracking application.
 * It provides:
 * 1. A search interface for users to enter flight numbers
 * 2. A recycler view displaying statistics about flight routes
 * 3. Navigation to the detailed flight tracking screen
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FlightStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding to access layout elements safely
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Clear flight database on app restart to prevent stale data accumulation
        // This is important for testing and development, but in production you might want
        // to maintain historical data with a cleanup strategy
        lifecycleScope.launch {
            FlightDatabase.getDatabase(this@MainActivity).flightDao().clearFlights()
        }

        // Start the background service that will collect flight data periodically
        // This allows the app to maintain updated flight information even when not in the foreground
        val serviceIntent = Intent(this, FlightDataCollectionService::class.java)
        startService(serviceIntent)

        // Set up RecyclerView to display flight route statistics
        setupRecyclerView()

        // Set up the flight tracking button with input validation
        setupTrackFlightButton()

        // Initialize flight statistics display
        loadFlightStats()
    }

    /**
     * Sets up the RecyclerView with proper adapter and layout
     */
    private fun setupRecyclerView() {
        adapter = FlightStatsAdapter()
        binding.flightStatsRecyclerView.adapter = adapter
        binding.flightStatsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Sets up the track flight button with validation and navigation
     */
    private fun setupTrackFlightButton() {
        binding.trackFlightButton.setOnClickListener {
            val flightNumber = binding.flightNumberEditText.text.toString().trim().uppercase()

            // Validate flight number format before proceeding
            if (ValidationUtils.isValidFlightNumber(flightNumber)) {
                // Navigate to flight tracking activity with the flight number
                val intent = Intent(this, FlightTrackingActivity::class.java).apply {
                    putExtra("FLIGHT_NUMBER", flightNumber)
                }
                startActivity(intent)
            } else {
                // Show error if flight number format is invalid
                Toast.makeText(this, R.string.error_invalid_flight_number, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Loads flight statistics from the database and updates the UI.
     * If no statistics exist, it inserts fake data for demonstration purposes.
     */
    private fun loadFlightStats() {
        val flightDao = FlightDatabase.getDatabase(this).flightDao()
        lifecycleScope.launch {
            // Check if the database is empty and insert demo data if needed
            // This ensures that users see example data even on first launch
            if (flightDao.getRouteStatistics().first().isEmpty()) {
                insertFakeFlightData(flightDao)
            }

            // Observe the flight statistics as a Flow to react to database changes
            // Using Flow with collectLatest ensures we only process the most recent data
            flightDao.getRouteStatistics().collectLatest { routeStatsResult ->
                // Convert database entities to UI model objects
                val routeStats = routeStatsResult.map {
                    RouteStatistics(
                        departureAirport = it.departureAirport,
                        arrivalAirport = it.arrivalAirport,
                        averageDuration = it.averageDuration.toInt()
                    )
                }
                // Update the RecyclerView with the new data
                adapter.submitList(routeStats)
            }
        }
    }

    /**
     * Inserts fake flight data into the database for demonstration purposes.
     * This is used when the app has no real flight data to display yet.
     *
     * @param flightDao The DAO used to access the flight database
     */
    private suspend fun insertFakeFlightData(flightDao: FlightDao) {
        val now = Date()
        val oneHourMillis = 60 * 60 * 1000

        // Create sample flight entities with realistic flight data
        // These represent common routes with reasonable flight durations and delays
        val sampleFlights: List<FlightEntity> = listOf(
            FlightEntity(
                flightnumber = "FAKE001",
                flightdate = now,
                departureairport = "JFK",  // New York
                arrivalairport = "LAX",    // Los Angeles
                scheduleddeparture = now,
                actualdeparture = Date(now.time + 15 * 60 * 1000), // 15 minute delay
                scheduledarrival = Date(now.time + 6 * oneHourMillis), // 6 hour flight
                actualarrival = Date(now.time + 6 * oneHourMillis + 10 * 60 * 1000), // 10 minute arrival delay
                departuredelay = 15,
                arrivaldelay = 10,
                scheduledduration = 6 * 60, // 6 hours in minutes
                actualduration = 6 * 60 + 10, // 6 hours 10 minutes
                flightstatus = "fake"
            ),
            FlightEntity(
                flightnumber = "FAKE002",
                flightdate = now,
                departureairport = "ORD",  // Chicago
                arrivalairport = "MIA",    // Miami
                scheduleddeparture = now,
                actualdeparture = Date(now.time + 20 * 60 * 1000), // 20 minute delay
                scheduledarrival = Date(now.time + 3 * oneHourMillis), // 3 hour flight
                actualarrival = Date(now.time + 3 * oneHourMillis + 5 * 60 * 1000), // 5 minute arrival delay
                departuredelay = 20,
                arrivaldelay = 5,
                scheduledduration = 3 * 60, // 3 hours in minutes
                actualduration = 3 * 60 + 5, // 3 hours 5 minutes
                flightstatus = "fake"
            )
        )

        // Insert each flight entity into the database
        // Type specification helps resolve any ambiguity in the lambda
        sampleFlights.forEach { flight: FlightEntity ->
            flightDao.insertFlight(flight)
        }
    }
}

/**
 * Data class to hold route statistics for display in the UI.
 * This is a presentation model that simplifies the database entity
 * to contain only the information needed for the statistics view.
 *
 * @property departureAirport The IATA code for the departure airport
 * @property arrivalAirport The IATA code for the arrival airport
 * @property averageDuration The average flight duration in minutes
 */
data class RouteStatistics(
    val departureAirport: String,
    val arrivalAirport: String,
    val averageDuration: Int
)

/**
 * RecyclerView adapter for flight statistics display.
 * Uses ListAdapter with DiffUtil for efficient updates when the data changes.
 */
class FlightStatsAdapter : ListAdapter<RouteStatistics, FlightStatsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<RouteStatistics>() {
        /**
         * Determines if two items represent the same route.
         * Routes are identified by their departure and arrival airports.
         */
        override fun areItemsTheSame(oldItem: RouteStatistics, newItem: RouteStatistics): Boolean {
            return oldItem.departureAirport == newItem.departureAirport &&
                    oldItem.arrivalAirport == newItem.arrivalAirport
        }

        /**
         * Checks if the contents of two items are the same.
         * This includes all properties that affect the display.
         */
        override fun areContentsTheSame(oldItem: RouteStatistics, newItem: RouteStatistics): Boolean {
            return oldItem == newItem // Uses data class equality (all properties)
        }
    }
) {
    /**
     * ViewHolder class that holds references to the views for each flight statistic item.
     * Uses view binding to safely access layout elements.
     */
    class ViewHolder(val binding: ItemFlightStatBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Creates new ViewHolder instances when the RecyclerView needs them.
     */
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFlightStatBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    /**
     * Binds flight statistics data to the views for display.
     * Formats the route as "DEP → ARR" and uses the FlightTimeCalculator
     * to format the duration in a human-readable way.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val routeStat = getItem(position)

        // Display the route with an arrow between departure and arrival
        holder.binding.routeText.text = "${routeStat.departureAirport} → ${routeStat.arrivalAirport}"

        // Format the duration using the utility class (e.g., "2h 15m")
        holder.binding.averageDurationText.text = FlightTimeCalculator.formatDuration(routeStat.averageDuration)
    }
}