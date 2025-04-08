package com.example.mc_assignment2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "FlightTrackingActivity"

class FlightTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlightTrackingBinding
    private lateinit var flightNumber: String
    private lateinit var apiService: FlightApiService
    private var map: GoogleMap? = null
    private var mapReady = false
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

        // Set up API service with timeout configuration
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(FlightApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FlightApiService::class.java)

        // Set up map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        if (mapFragment == null) {
            Log.e(TAG, "Map fragment not found in layout")
            binding.mapErrorText.visibility = View.VISIBLE
            binding.mapErrorText.text = "Error: Map fragment not found"
        } else {
            mapFragment.getMapAsync { googleMap ->
                map = googleMap
                mapReady = true
                Log.d(TAG, "Map is ready")
                // If we already have flight data, update the map
                currentFlightData?.let { updateMap(it) }
            }
        }

        // Display flight number
        binding.flightNumberText.text = flightNumber

        // Show loading indicator
        showLoading(true)

        // Initial data load
        checkInternetPermissionAndRefresh()
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

    private fun checkInternetPermissionAndRefresh() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                1001
            )
        } else {
            refreshFlightData()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshFlightData()
        } else {
            Toast.makeText(this, "Internet permission is required", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshFlightData() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching flight data for: $flightNumber")
                val response = apiService.getFlightByNumber(flightNumber = flightNumber)

                if (response.isSuccessful) {
                    val flightResponse = response.body()
                    val flightData = flightResponse?.data?.firstOrNull()

                    if (flightData != null) {
                        Log.d(TAG, "Flight data received: ${flightData.flight.iata}")
                        currentFlightData = flightData
                        updateUI(flightData)

                        // Only update map if it's ready
                        if (mapReady) {
                            updateMap(flightData)
                        } else {
                            Log.d(TAG, "Map not ready yet, will update when ready")
                        }
                    } else {
                        Log.e(TAG, "Flight data not found")
                        binding.flightStatusText.text = getString(R.string.error_flight_not_found)
                        binding.errorMessage.visibility = View.VISIBLE
                        binding.errorMessage.text = getString(R.string.error_flight_not_found)
                        // Show map error
                        binding.mapErrorText.visibility = View.VISIBLE
                        binding.mapErrorText.text = "No flight data available to display on map"
                    }
                } else {
                    Log.e(TAG, "API error: ${response.code()}, ${response.message()}")
                    binding.flightStatusText.text = getString(R.string.error_network)
                    binding.errorMessage.visibility = View.VISIBLE
                    binding.errorMessage.text = "API Error: ${response.code()} - Please check your network connection"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error", e)
                binding.flightStatusText.text = getString(R.string.error_network)
                binding.errorMessage.visibility = View.VISIBLE
                binding.errorMessage.text = "Network Error: ${e.message ?: "Unknown error"}"
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.errorMessage.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun updateUI(flightData: FlightData) {
        binding.errorMessage.visibility = View.GONE

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
        }
        val scheduledArrival = flightData.arrival.scheduled?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)
        }

        if (scheduledDeparture != null && scheduledArrival != null) {
            val durationMinutes = FlightTimeCalculator.calculateDurationInMinutes(
                scheduledDeparture, scheduledArrival
            )
            binding.flightDurationText.text = FlightTimeCalculator.formatDuration(durationMinutes)
        } else {
            binding.flightDurationText.text = "N/A"
        }

        // Display delay information
        val delayText = when {
            flightData.departure.delay != null -> FlightTimeCalculator.formatDelay(flightData.departure.delay)
            flightData.arrival.delay != null -> FlightTimeCalculator.formatDelay(flightData.arrival.delay)
            else -> "No delay information"
        }
        binding.delayText.text = delayText
    }

    private fun updateMap(flightData: FlightData) {
        try {
            val googleMap = map ?: run {
                Log.e(TAG, "Google Map is null")
                binding.mapErrorText.visibility = View.VISIBLE
                binding.mapErrorText.text = "Map not initialized"
                return
            }

            // Clear previous markers
            googleMap.clear()
            binding.mapErrorText.visibility = View.GONE

            // Log airport info for debugging
            Log.d(TAG, "Departure airport_info: ${flightData.departure.airport_info}")
            Log.d(TAG, "Arrival airport_info: ${flightData.arrival.airport_info}")

            // Get departure and arrival coordinates from airport_info
            val depLat = flightData.departure.airport_info?.latitude
            val depLng = flightData.departure.airport_info?.longitude
            val arrLat = flightData.arrival.airport_info?.latitude
            val arrLng = flightData.arrival.airport_info?.longitude

            // Check if we have coordinates
            if (depLat == null || depLng == null || arrLat == null || arrLng == null) {
                Log.e(TAG, "Missing airport coordinates - using fallback coordinates")
                // Use fallback coordinates based on common airport locations
                // This is just a workaround - in a real app you'd have a database of airport coordinates
                handleMissingCoordinates(flightData, googleMap)
                return
            }

            val departurePosition = LatLng(depLat, depLng)
            val arrivalPosition = LatLng(arrLat, arrLng)

            // Add markers for departure and arrival airports
            googleMap.addMarker(MarkerOptions().position(departurePosition).title(flightData.departure.iata))
            googleMap.addMarker(MarkerOptions().position(arrivalPosition).title(flightData.arrival.iata))

            // Draw a line between departure and arrival
            googleMap.addPolyline(
                PolylineOptions()
                    .add(departurePosition, arrivalPosition)
                    .width(5f)
                    .color(ContextCompat.getColor(this, R.color.colorPrimary))
            )

            // If the flight is in the air and we have live position, show it
            val flightLat = flightData.live?.latitude
            val flightLng = flightData.live?.longitude

            if (flightLat != null && flightLng != null && flightData.flight_status == "active") {
                val flightPosition = LatLng(flightLat, flightLng)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(flightPosition)
                        .title(flightData.flight.iata)
                        .snippet("Current Position")
                )

                // Center the map on the current flight position
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(flightPosition, 5f))
            } else {
                // If no live data, show the entire route
                try {
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                        .include(departurePosition)
                        .include(arrivalPosition)
                        .build()

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting map bounds", e)
                    // Fallback: center between the two points
                    val midLat = (depLat + arrLat) / 2
                    val midLng = (depLng + arrLng) / 2
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 4f))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating map", e)
            binding.mapErrorText.visibility = View.VISIBLE
            binding.mapErrorText.text = "Map error: ${e.message}"
        }
    }

    // Fallback method when coordinates are missing
    private fun handleMissingCoordinates(flightData: FlightData, googleMap: GoogleMap) {
        binding.mapErrorText.visibility = View.VISIBLE
        binding.mapErrorText.text = "Using approximate airport locations"

        // Create a map of common airport codes to coordinates
        val airportCoordinates = mapOf(
            "JFK" to LatLng(40.6413, -73.7781),
            "LAX" to LatLng(33.9416, -118.4085),
            "LHR" to LatLng(51.4700, -0.4543),
            "CDG" to LatLng(49.0097, 2.5479),
            "DXB" to LatLng(25.2528, 55.3644),
            "SFO" to LatLng(37.6213, -122.3790),
            "ATL" to LatLng(33.6407, -84.4277),
            "ORD" to LatLng(41.9742, -87.9073),
            "DEL" to LatLng(28.7041, 77.1025),
            // Add more as needed
        )

        // Try to get coordinates from our map
        val depPos = airportCoordinates[flightData.departure.iata] ?: LatLng(0.0, 0.0)
        val arrPos = airportCoordinates[flightData.arrival.iata] ?: LatLng(0.0, 0.0)

        // If we couldn't find either airport, show world map
        if (depPos == LatLng(0.0, 0.0) && arrPos == LatLng(0.0, 0.0)) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 1f))
            binding.mapErrorText.text = "Cannot display route map - missing airport coordinates"
            return
        }

        // Add markers for airports we found
        if (depPos != LatLng(0.0, 0.0)) {
            googleMap.addMarker(MarkerOptions().position(depPos).title(flightData.departure.iata))
        }

        if (arrPos != LatLng(0.0, 0.0)) {
            googleMap.addMarker(MarkerOptions().position(arrPos).title(flightData.arrival.iata))
        }

        // If we have both airports, draw a line and zoom to fit
        if (depPos != LatLng(0.0, 0.0) && arrPos != LatLng(0.0, 0.0)) {
            googleMap.addPolyline(
                PolylineOptions()
                    .add(depPos, arrPos)
                    .width(5f)
                    .color(ContextCompat.getColor(this, R.color.colorPrimary))
            )

            try {
                val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    .include(depPos)
                    .include(arrPos)
                    .build()

                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
            } catch (e: Exception) {
                Log.e(TAG, "Error setting fallback map bounds", e)
                // Center between the two points
                val midLat = (depPos.latitude + arrPos.latitude) / 2
                val midLng = (depPos.longitude + arrPos.longitude) / 2
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 3f))
            }
        } else {
            // Otherwise just show the one we have
            val pos = if (depPos != LatLng(0.0, 0.0)) depPos else arrPos
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 5f))
        }
    }
}
