package com.example.mc_assignment2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mc_assignment2.api.FlightApiService
import com.example.mc_assignment2.api.FlightData
import com.example.mc_assignment2.databinding.ActivityFlightTrackingBinding
import com.example.mc_assignment2.utils.FlightTimeCalculator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class FlightTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlightTrackingBinding
    private lateinit var flightNumber: String
    private lateinit var apiService: FlightApiService
    private lateinit var map: GoogleMap
    private var currentFlightData: FlightData? = null

    // Handler for minute-by-minute updates
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            refreshFlightData()
            // Schedule next update in 1 minute
            handler.postDelayed(this, 60 * 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFlightTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get flight number from intent
        flightNumber = intent.getStringExtra("FLIGHT_NUMBER") ?: run {
            finish()
            return
        }

        // Set up API service
        apiService = Retrofit.Builder()
            .baseUrl(FlightApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FlightApiService::class.java)

        // Set up map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            // Initial flight data load
            refreshFlightData()
        }

        // Display flight number
        binding.flightNumberText.text = flightNumber
    }

    override fun onResume() {
        super.onResume()
        // Start periodic updates
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Stop periodic updates
        handler.removeCallbacks(updateRunnable)
    }

    private fun refreshFlightData() {
        lifecycleScope.launch {
            try {
                val response = apiService.getFlightByNumber(flightNumber = flightNumber)

                if (response.isSuccessful) {
                    val flightResponse = response.body()
                    val flightData = flightResponse?.data?.firstOrNull()

                    if (flightData != null) {
                        currentFlightData = flightData
                        updateUI(flightData)
                        updateMap(flightData)
                    } else {
                        binding.flightStatusText.text = getString(R.string.error_flight_not_found)
                    }
                } else {
                    binding.flightStatusText.text = getString(R.string.error_network)
                }
            } catch (e: Exception) {
                binding.flightStatusText.text = getString(R.string.error_network)
            }
        }
    }

    private fun updateUI(flightData: FlightData) {
        // Update flight status
        binding.flightStatusText.text = flightData.flight_status ?: "Unknown"

        // Format dates for display
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Update departure info
        binding.departureAirportText.text = flightData.departure.iata
        binding.departureTimeText.text = flightData.departure.scheduled?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)
        }?.let { dateFormat.format(it) } ?: "N/A"

        // Update arrival info
        binding.arrivalAirportText.text = flightData.arrival.iata
        binding.arrivalTimeText.text = flightData.arrival.scheduled?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)
        }?.let { dateFormat.format(it) } ?: "N/A"


        // Calculate and display flight duration
        val scheduledDeparture = flightData.departure.scheduled?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)
        } ?: throw IllegalArgumentException("Invalid departure date")

        val scheduledArrival = flightData.arrival.scheduled?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)
        } ?: throw IllegalArgumentException("Invalid arrival date")

        val durationMinutes = FlightTimeCalculator.calculateDurationInMinutes(scheduledDeparture, scheduledArrival)
        binding.flightDurationText.text = FlightTimeCalculator.formatDuration(durationMinutes)

        // Display delay information
        binding.delayText.text = FlightTimeCalculator.formatDelay(flightData.departure.delay)
    }

    private fun updateMap(flightData: FlightData) {
        // Check if we have live flight data
        val liveData = flightData.live

        if (liveData != null) {
            // We have live position data
            val currentPosition = LatLng(liveData.latitude, liveData.longitude)

            // Clear previous markers and lines
            map.clear()

            // Add current position marker
            map.addMarker(
                MarkerOptions()
                    .position(currentPosition)
                    .title(flightData.flight.iata)
            )

            // Animate camera to current position
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 6f))

            // Draw flight path if we have departure and arrival coordinates
            val departureAirport = getAirportLocation(flightData.departure.iata)
            val arrivalAirport = getAirportLocation(flightData.arrival.iata)

            if (departureAirport != null && arrivalAirport != null) {
                // Add departure and arrival markers
                map.addMarker(
                    MarkerOptions()
                        .position(departureAirport)
                        .title(flightData.departure.iata)
                )

                map.addMarker(
                    MarkerOptions()
                        .position(arrivalAirport)
                        .title(flightData.arrival.iata)
                )

                // Draw flight path
                map.addPolyline(
                    PolylineOptions()
                        .add(departureAirport, currentPosition, arrivalAirport)
                        .color(resources.getColor(R.color.colorPrimary, theme))
                        .width(5f)
                )
            }
        } else {
            // No live data, just show departure and arrival airports
            val departureAirport = getAirportLocation(flightData.departure.iata)
            val arrivalAirport = getAirportLocation(flightData.arrival.iata)

            if (departureAirport != null && arrivalAirport != null) {
                // Clear previous markers and lines
                map.clear()

                // Add departure and arrival markers
                map.addMarker(
                    MarkerOptions()
                        .position(departureAirport)
                        .title(flightData.departure.iata)
                )

                map.addMarker(
                    MarkerOptions()
                        .position(arrivalAirport)
                        .title(flightData.arrival.iata)
                )

                // Draw flight path
                map.addPolyline(
                    PolylineOptions()
                        .add(departureAirport, arrivalAirport)
                        .color(resources.getColor(R.color.colorPrimary, theme))
                        .width(5f)
                )

                // Show both airports in view
                val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.builder()
                boundsBuilder.include(departureAirport)
                boundsBuilder.include(arrivalAirport)

                val padding = resources.getDimensionPixelSize(R.dimen.map_padding)
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), padding))
            }
        }
    }

    // Airport location lookup
    private fun getAirportLocation(iataCode: String): LatLng? {
        // This would typically be fetched from a database
        return when (iataCode) {
            "JFK" -> LatLng(40.6413, -73.7781)
            "LAX" -> LatLng(33.9416, -118.4085)
            "ORD" -> LatLng(41.9742, -87.9073)
            "LHR" -> LatLng(51.4700, -0.4543)
            "CDG" -> LatLng(49.0097, 2.5479)
            "SFO" -> LatLng(37.6213, -122.3790)
            "ATL" -> LatLng(33.6407, -84.4277)
            "DFW" -> LatLng(32.8998, -97.0403)
            "DEN" -> LatLng(39.8561, -104.6737)
            "MIA" -> LatLng(25.7959, -80.2871)
            "SEA" -> LatLng(47.4502, -122.3088)
            else -> null
        }
    }
}