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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FlightStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Clear flight database on app restart
        lifecycleScope.launch {
            FlightDatabase.getDatabase(this@MainActivity).flightDao().clearFlights()
        }

        // Start the flight data collection service
        val serviceIntent = Intent(this, FlightDataCollectionService::class.java)
        startService(serviceIntent)

        // Set up RecyclerView
        adapter = FlightStatsAdapter()
        binding.flightStatsRecyclerView.adapter = adapter
        binding.flightStatsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Set up click listener for track flight button
        binding.trackFlightButton.setOnClickListener {
            val flightNumber = binding.flightNumberEditText.text.toString().trim().uppercase()

            if (ValidationUtils.isValidFlightNumber(flightNumber)) {
                val intent = Intent(this, FlightTrackingActivity::class.java).apply {
                    putExtra("FLIGHT_NUMBER", flightNumber)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.error_invalid_flight_number, Toast.LENGTH_SHORT).show()
            }
        }

        // Load flight statistics using new query
        loadFlightStats()
    }

    private fun loadFlightStats() {
        val flightDao = FlightDatabase.getDatabase(this).flightDao()
        lifecycleScope.launch {
            // Check if no route statistics exist; if empty, insert fake data
            if (flightDao.getRouteStatistics().first().isEmpty()) {
                insertFakeFlightData(flightDao)
            }
            flightDao.getRouteStatistics().collectLatest { routeStatsResult ->
                val routeStats = routeStatsResult.map {
                    RouteStatistics(
                        departureAirport = it.departureAirport,
                        arrivalAirport = it.arrivalAirport,
                        averageDuration = it.averageDuration.toInt()
                    )
                }
                adapter.submitList(routeStats)
            }
        }
    }

    private suspend fun insertFakeFlightData(flightDao: FlightDao) {
        val now = Date()
        val oneHourMillis = 60 * 60 * 1000
        val sampleFlights: List<FlightEntity> = listOf(
            FlightEntity(
                flightnumber = "FAKE001",
                flightdate = now,
                departureairport = "JFK",
                arrivalairport = "LAX",
                scheduleddeparture = now,
                actualdeparture = Date(now.time + 15 * 60 * 1000), // +15 mins
                scheduledarrival = Date(now.time + 6 * oneHourMillis),
                actualarrival = Date(now.time + 6 * oneHourMillis + 10 * 60 * 1000),
                departuredelay = 15,
                arrivaldelay = 10,
                scheduledduration = 6 * 60,
                actualduration = 6 * 60 + 10,
                flightstatus = "fake"
            ),
            FlightEntity(
                flightnumber = "FAKE002",
                flightdate = now,
                departureairport = "ORD",
                arrivalairport = "MIA",
                scheduleddeparture = now,
                actualdeparture = Date(now.time + 20 * 60 * 1000), // +20 mins
                scheduledarrival = Date(now.time + 3 * oneHourMillis),
                actualarrival = Date(now.time + 3 * oneHourMillis + 5 * 60 * 1000),
                departuredelay = 20,
                arrivaldelay = 5,
                scheduledduration = 3 * 60,
                actualduration = 3 * 60 + 5,
                flightstatus = "fake"
            )
        )
        // Explicitly specify the type of flight to remove ambiguity
        sampleFlights.forEach { flight: FlightEntity ->
            flightDao.insertFlight(flight)
        }
    }
}

/**
 * Data class to hold route statistics for display
 */
data class RouteStatistics(
    val departureAirport: String,
    val arrivalAirport: String,
    val averageDuration: Int
)

/**
 * RecyclerView adapter for flight statistics
 */
class FlightStatsAdapter : ListAdapter<RouteStatistics, FlightStatsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<RouteStatistics>() {
        override fun areItemsTheSame(oldItem: RouteStatistics, newItem: RouteStatistics): Boolean {
            return oldItem.departureAirport == newItem.departureAirport &&
                    oldItem.arrivalAirport == newItem.arrivalAirport
        }

        override fun areContentsTheSame(oldItem: RouteStatistics, newItem: RouteStatistics): Boolean {
            return oldItem == newItem
        }
    }
) {

    class ViewHolder(val binding: ItemFlightStatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFlightStatBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val routeStat = getItem(position)
        holder.binding.routeText.text = "${routeStat.departureAirport} → ${routeStat.arrivalAirport}"
        holder.binding.averageDurationText.text = FlightTimeCalculator.formatDuration(routeStat.averageDuration)
    }
}
