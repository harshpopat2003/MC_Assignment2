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
import com.example.mc_assignment2.database.FlightDatabase
import com.example.mc_assignment2.database.FlightEntity

private const val TAG = "FlightTrackingActivity"

/**
 * This activity displays real-time flight tracking information including:
 * - Flight status and details
 * - Departure and arrival times
 * - Flight duration and delay information
 * - Map visualization of the flight route
 *
 * The activity fetches flight data from an API and updates it every minute.
 * It also stores flight information in a local database for offline access.
 */
class FlightTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlightTrackingBinding
    private lateinit var flightNumber: String
    private lateinit var apiService: FlightApiService
    private var map: GoogleMap? = null
    private var mapReady = false
    private var currentFlightData: FlightData? = null

    // Handler for minute-by-minute updates
    private val handler = Handler(Looper.getMainLooper())

    // Runnable that will be executed periodically to refresh flight data
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

        // Get flight number from intent - finish activity if not provided
        flightNumber = intent.getStringExtra("FLIGHT_NUMBER") ?: run {
            finish()
            return
        }

        // Set up API service with timeout configuration to handle slow network connections
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

        // Set up Google Maps fragment for flight route visualization
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

        // Display flight number in the UI
        binding.flightNumberText.text = flightNumber

        // Show loading indicator while fetching data
        showLoading(true)

        // Initial data load after checking for internet permission
        checkInternetPermissionAndRefresh()
    }

    /**
     * When activity becomes visible, start periodic updates
     * to keep flight information current
     */
    override fun onResume() {
        super.onResume()
        // Start periodic updates every minute
        handler.post(updateRunnable)
    }

    /**
     * When activity is no longer visible, stop periodic updates
     * to conserve system resources
     */
    override fun onPause() {
        super.onPause()
        // Stop periodic updates
        handler.removeCallbacks(updateRunnable)
    }

    /**
     * Checks if internet permission is granted and requests it if needed.
     * Once permission is confirmed, initiates the data refresh.
     */
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

    /**
     * Handles the result of permission requests.
     * If internet permission is granted, proceeds with data refresh,
     * otherwise informs the user that the permission is required.
     */
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

    /**
     * Fetches the latest flight data from the API.
     * This is the core function that retrieves flight information and updates the UI.
     * It's called initially and then every minute when the activity is visible.
     */
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

                        // Update UI with the new flight data
                        updateUI(flightData)

                        // Store flight details in the local database for offline access
                        storeFlightData(flightData)

                        // Only update map if it's ready
                        if (mapReady) {
                            updateMap(flightData)
                        } else {
                            Log.d(TAG, "Map not ready yet, will update when ready")
                        }
                    } else {
                        // Handle case when no flight data is found
                        Log.e(TAG, "Flight data not found")
                        binding.flightStatusText.text = getString(R.string.error_flight_not_found)
                        binding.errorMessage.visibility = View.VISIBLE
                        binding.errorMessage.text = getString(R.string.error_flight_not_found)
                        // Show map error
                        binding.mapErrorText.visibility = View.VISIBLE
                        binding.mapErrorText.text = "No flight data available to display on map"
                    }
                } else {
                    // Handle API error responses (e.g., 404, 500)
                    Log.e(TAG, "API error: ${response.code()}, ${response.message()}")
                    binding.flightStatusText.text = getString(R.string.error_network)
                    binding.errorMessage.visibility = View.VISIBLE
                    binding.errorMessage.text = "API Error: ${response.code()} - Please check your network connection"
                }
            } catch (e: Exception) {
                // Handle network exceptions and other unforeseen errors
                Log.e(TAG, "Network error", e)
                binding.flightStatusText.text = getString(R.string.error_network)
                binding.errorMessage.visibility = View.VISIBLE
                binding.errorMessage.text = "Network Error: ${e.message ?: "Unknown error"}"
            } finally {
                // Always hide the loading indicator when operation completes
                showLoading(false)
            }
        }
    }

    /**
     * Stores the flight data in the local Room database.
     * This allows the app to show flight history and maintain data when offline.
     *
     * @param flightData The flight data object received from the API
     */
    private suspend fun storeFlightData(flightData: com.example.mc_assignment2.api.FlightData) {
        val flightDao = FlightDatabase.getDatabase(this).flightDao()
        val dateFormatFull = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val dateFormatShort = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            // Parse dates from API format to Java Date objects
            val scheduledDeparture = flightData.departure.scheduled?.let { dateFormatFull.parse(it) } ?: Date()
            val actualDeparture = flightData.departure.actual?.let { dateFormatFull.parse(it) }
            val scheduledArrival = flightData.arrival.scheduled?.let { dateFormatFull.parse(it) } ?: Date()
            val actualArrival = flightData.arrival.actual?.let { dateFormatFull.parse(it) }

            // Create a FlightEntity object to store in the database
            val flightEntity = FlightEntity(
                flightnumber = flightData.flight.iata,
                flightdate = flightData.flight_date?.let { dateFormatShort.parse(it) } ?: Date(),
                departureairport = flightData.departure.iata,
                arrivalairport = flightData.arrival.iata,
                scheduleddeparture = scheduledDeparture,
                actualdeparture = actualDeparture,
                scheduledarrival = scheduledArrival,
                actualarrival = actualArrival,
                departuredelay = flightData.departure.delay,
                arrivaldelay = flightData.arrival.delay,
                scheduledduration = com.example.mc_assignment2.utils.FlightTimeCalculator.calculateDurationInMinutes(scheduledDeparture, scheduledArrival),
                actualduration = if (actualDeparture != null && actualArrival != null)
                    com.example.mc_assignment2.utils.FlightTimeCalculator.calculateDurationInMinutes(actualDeparture, actualArrival) else null,
                flightstatus = flightData.flight_status ?: "unknown"
            )

            // Insert the flight into the database
            flightDao.insertFlight(flightEntity)
            Log.d(TAG, "Stored flight in database: ${flightEntity.flightnumber}")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing flight data", e)
        }
    }

    /**
     * Controls the visibility of the loading indicator.
     * Also hides error messages when loading is in progress.
     *
     * @param isLoading Whether the loading indicator should be shown
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.errorMessage.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }

    /**
     * Updates all UI elements with the latest flight information.
     * This includes status, times, duration, and delay information.
     *
     * @param flightData The flight data object containing all flight information
     */
    private fun updateUI(flightData: FlightData) {
        binding.errorMessage.visibility = View.GONE

        // Update flight status (e.g., "scheduled", "active", "landed")
        binding.flightStatusText.text = flightData.flight_status ?: "Unknown"

        // Format dates for display in a user-friendly format (HH:MM)
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Update departure airport code and scheduled time
        binding.departureAirportText.text = flightData.departure.iata
        binding.departureTimeText.text = flightData.departure.scheduled?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)
        }?.let { dateFormat.format(it) } ?: "N/A"

        // Update arrival airport code and scheduled time
        binding.arrivalAirportText.text = flightData.arrival.iata
        binding.arrivalTimeText.text = flightData.arrival.scheduled?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)
        }?.let { dateFormat.format(it) } ?: "N/A"

        // Calculate and display flight duration in hours and minutes
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

        // Display delay information if available
        val delayText = when {
            flightData.departure.delay != null -> FlightTimeCalculator.formatDelay(flightData.departure.delay)
            flightData.arrival.delay != null -> FlightTimeCalculator.formatDelay(flightData.arrival.delay)
            else -> "No delay information"
        }
        binding.delayText.text = delayText
    }

    /**
     * Updates the Google Map with flight route visualization.
     * Shows departure and arrival airports, connects them with a line,
     * and displays the current aircraft position if available.
     *
     * @param flightData The flight data object containing coordinates and status
     */
    private fun updateMap(flightData: FlightData) {
        try {
            val googleMap = map ?: run {
                Log.e(TAG, "Google Map is null")
                binding.mapErrorText.visibility = View.VISIBLE
                binding.mapErrorText.text = "Map not initialized"
                return
            }

            // Clear previous markers and routes
            googleMap.clear()
            binding.mapErrorText.visibility = View.GONE

            // Log airport info for debugging coordinate issues
            Log.d(TAG, "Departure airport_info: ${flightData.departure.airport_info}")
            Log.d(TAG, "Arrival airport_info: ${flightData.arrival.airport_info}")

            // Get departure and arrival coordinates from airport_info
            val depLat = flightData.departure.airport_info?.latitude
            val depLng = flightData.departure.airport_info?.longitude
            val arrLat = flightData.arrival.airport_info?.latitude
            val arrLng = flightData.arrival.airport_info?.longitude

            // Fall back to hardcoded coordinates if API doesn't provide them
            if (depLat == null || depLng == null || arrLat == null || arrLng == null) {
                Log.e(TAG, "Missing airport coordinates - using fallback coordinates")
                // This is a resilience measure in case the API doesn't provide coordinates
                handleMissingCoordinates(flightData, googleMap)
                return
            }

            val departurePosition = LatLng(depLat, depLng)
            val arrivalPosition = LatLng(arrLat, arrLng)

            // Add markers for departure and arrival airports
            googleMap.addMarker(MarkerOptions().position(departurePosition).title(flightData.departure.iata))
            googleMap.addMarker(MarkerOptions().position(arrivalPosition).title(flightData.arrival.iata))

            // Draw a line representing the flight route
            googleMap.addPolyline(
                PolylineOptions()
                    .add(departurePosition, arrivalPosition)
                    .width(5f)
                    .color(ContextCompat.getColor(this, R.color.colorPrimary))
            )

            // If the flight is in the air and we have live position, show the aircraft location
            val flightLat = flightData.live?.latitude
            val flightLng = flightData.live?.longitude

            if (flightLat != null && flightLng != null && flightData.flight_status == "active") {
                // Create a marker for the current aircraft position
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
                // If no live data, show the entire route by creating a boundary that includes both airports
                try {
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                        .include(departurePosition)
                        .include(arrivalPosition)
                        .build()

                    // Add padding around the bounds for better visibility
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting map bounds", e)
                    // Fallback: center between the two points if boundary calculation fails
                    val midLat = (depLat + arrLat) / 2
                    val midLng = (depLng + arrLng) / 2
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 4f))
                }
            }
        } catch (e: Exception) {
            // Handle any unexpected errors during map update
            Log.e(TAG, "Error updating map", e)
            binding.mapErrorText.visibility = View.VISIBLE
            binding.mapErrorText.text = "Map error: ${e.message}"
        }
    }

    /**
     * Fallback method for handling missing airport coordinates.
     * Uses a hardcoded database of common airport locations when the API
     * doesn't provide coordinate information for airports.
     *
     * @param flightData The flight data object with airport codes
     * @param googleMap The Google Map instance to update
     */
    private fun handleMissingCoordinates(flightData: FlightData, googleMap: GoogleMap) {
        val depCode = flightData.departure.iata
        val arrCode = flightData.arrival.iata

        Log.d(TAG, "Using fallback coordinates for flight: ${flightData.flight.iata} from $depCode to $arrCode")

        binding.mapErrorText.visibility = View.VISIBLE
        binding.mapErrorText.text = "Using approximate airport locations"

        // Create a map of common airport codes to coordinates
        // This is a resilience feature when the API doesn't provide coordinates
        val airportCoordinates = mapOf(
            // North America
            "JFK" to LatLng(40.6413, -73.7781),
            "LAX" to LatLng(33.9416, -118.4085),
            "SFO" to LatLng(37.6213, -122.3790),
            "ATL" to LatLng(33.6407, -84.4277),
            "ORD" to LatLng(41.9742, -87.9073),
            "MIA" to LatLng(25.7932, -80.2906),
            "DFW" to LatLng(32.8998, -97.0403),
            "DEN" to LatLng(39.8561, -104.6737),
            "SEA" to LatLng(47.4502, -122.3088),
            "YYZ" to LatLng(43.6777, -79.6248),

            // Europe
            "LHR" to LatLng(51.4700, -0.4543),
            "CDG" to LatLng(49.0097, 2.5479),
            "FRA" to LatLng(50.0379, 8.5622),
            "AMS" to LatLng(52.3105, 4.7683),
            "MAD" to LatLng(40.4983, -3.5676),
            "FCO" to LatLng(41.8045, 12.2508),
            "ZRH" to LatLng(47.4582, 8.5555),

            // Asia
            "DEL" to LatLng(28.5562, 77.1000),  // Delhi
            "BOM" to LatLng(19.0896, 72.8656),  // Mumbai
            "MAA" to LatLng(12.9941, 80.1709),  // Chennai
            "CCU" to LatLng(22.6520, 88.4463),  // Kolkata
            "BLR" to LatLng(13.1986, 77.7066),  // Bangalore
            "HYD" to LatLng(17.2403, 78.4294),  // Hyderabad
            "DXB" to LatLng(25.2528, 55.3644),  // Dubai
            "SIN" to LatLng(1.3644, 103.9915),  // Singapore
            "HKG" to LatLng(22.3080, 113.9185), // Hong Kong
            "NRT" to LatLng(35.7647, 140.3864), // Tokyo Narita
            "HND" to LatLng(35.5494, 139.7798), // Tokyo Haneda
            "PEK" to LatLng(40.0799, 116.6031), // Beijing
            "PVG" to LatLng(31.1443, 121.8083), // Shanghai Pudong

            // Australia/Pacific
            "SYD" to LatLng(-33.9399, 151.1753), // Sydney
            "MEL" to LatLng(-37.6690, 144.8410), // Melbourne
            "AKL" to LatLng(-37.0082, 174.7850)  // Auckland
        )

        // Try to get coordinates from our map of common airports
        val depPos = airportCoordinates[depCode]
        val arrPos = airportCoordinates[arrCode]

        Log.d(TAG, "Departure airport $depCode coordinate found: ${depPos != null}")
        Log.d(TAG, "Arrival airport $arrCode coordinate found: ${arrPos != null}")

        // If we couldn't find either airport, show world map with error message
        if (depPos == null && arrPos == null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 1f))
            binding.mapErrorText.text = "Cannot display route map - unknown airport codes: $depCode and $arrCode"
            return
        }

        // Add markers for airports we found
        if (depPos != null) {
            googleMap.addMarker(MarkerOptions().position(depPos).title(flightData.departure.iata))
        }

        if (arrPos != null) {
            googleMap.addMarker(MarkerOptions().position(arrPos).title(flightData.arrival.iata))
        }

        // If we have both airports, draw a line and zoom to fit
        if (depPos != null && arrPos != null) {
            googleMap.addPolyline(
                PolylineOptions()
                    .add(depPos, arrPos)
                    .width(5f)
                    .color(ContextCompat.getColor(this, R.color.colorPrimary))
            )

            // Try to fit both airports in the view
            try {
                val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    .include(depPos)
                    .include(arrPos)
                    .build()

                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
            } catch (e: Exception) {
                Log.e(TAG, "Error setting fallback map bounds", e)
                // Center between the two points as a fallback
                val midLat = (depPos.latitude + arrPos.latitude) / 2
                val midLng = (depPos.longitude + arrPos.longitude) / 2
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(midLat, midLng), 3f))
            }
        } else {
            // If we only have one airport, just show that one
            val pos = depPos ?: arrPos!!
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 5f))
            binding.mapErrorText.text = if (depPos == null) {
                "Unknown departure airport: $depCode"
            } else {
                "Unknown arrival airport: $arrCode"
            }
        }
    }
}