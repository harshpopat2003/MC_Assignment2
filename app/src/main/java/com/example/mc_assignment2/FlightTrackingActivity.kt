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
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            // Initial flight data load
            checkInternetPermissionAndRefresh()
        }

        // Display flight number
        binding.flightNumberText.text = flightNumber
        
        // Show loading indicator
        showLoading(true)
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
                        updateMap(flightData)
                    } else {
                        Log.e(TAG, "Flight data not found")
                        binding.flightStatusText.text = getString(R.string.error_flight_not_found)
                        binding.errorMessage.visibility = View.VISIBLE
                        binding.errorMessage.text = getString(R.string.error_flight_not_found)
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
            // Clear previous markers
            map.clear()
            
            // Get departure and arrival coordinates from airport_info
            val depLat = flightData.departure.airport_info?.latitude
            val depLng = flightData.departure.airport_info?.longitude
            val arrLat = flightData.arrival.airport_info?.latitude
            val arrLng = flightData.arrival.airport_info?.longitude
            
            // Only continue if we have valid coordinates for both airports
            if (depLat != null && depLng != null && arrLat != null && arrLng != null) {
                val departurePosition = LatLng(depLat, depLng)
                val arrivalPosition = LatLng(arrLat, arrLng)
                
                // Add markers for departure and arrival airports
                map.addMarker(MarkerOptions().position(departurePosition).title(flightData.departure.iata))
                map.addMarker(MarkerOptions().position(arrivalPosition).title(flightData.arrival.iata))
                
                // Draw a line between departure and arrival
                map.addPolyline(
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
                    map.addMarker(
                        MarkerOptions()
                            .position(flightPosition)
                            .title(flightData.flight.iata)
                            .snippet("Current Position")
                    )
                    
                    // Center the map on the current flight position
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(flightPosition, 5f))
                } else {
                    // If no live data, show the entire route
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                        .include(departurePosition)
                        .include(arrivalPosition)
                        .build()
                    
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                }
            } else {
                Log.e(TAG, "Missing coordinates for airports")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating map", e)
        }
    }
}
