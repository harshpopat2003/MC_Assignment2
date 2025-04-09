package com.example.mc_assignment2.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit interface to define API endpoints for the AviationStack service
interface FlightApiService {
    companion object {
        // Base URL of the AviationStack API
        const val BASE_URL = "https://api.aviationstack.com/v1/"

        // API key for authentication
        const val API_KEY = "1e55772bb9e23418e51a0ba91bd100f5"
    }

    // Fetch flight details by flight IATA number
    @GET("flights")
    suspend fun getFlightByNumber(
        @Query("access_key") apiKey: String = API_KEY,
        @Query("flight_iata") flightNumber: String
    ): Response<FlightResponse>

    // Fetch flights based on departure and arrival airport IATA codes
    @GET("flights")
    suspend fun getFlightsByRoute(
        @Query("access_key") apiKey: String = API_KEY,
        @Query("dep_iata") departureAirport: String,
        @Query("arr_iata") arrivalAirport: String
    ): Response<FlightResponse>
}

// Top-level response from the API
data class FlightResponse(
    val pagination: Pagination,          // Information about pagination (for handling multiple pages of results)
    val data: List<FlightData>           // List of flight records returned
)

// Pagination info returned by the API
data class Pagination(
    val limit: Int,
    val offset: Int,
    val count: Int,
    val total: Int
)

// Represents detailed data about a flight
data class FlightData(
    val flight_date: String,             // Scheduled flight date
    val flight_status: String?,          // Status (e.g., active, landed, cancelled)
    val departure: AirportInfo,          // Departure airport details
    val arrival: AirportInfo,            // Arrival airport details
    val airline: Airline,                // Airline operating the flight
    val flight: FlightInfo,              // Flight number and codes
    val aircraft: Aircraft?,             // Aircraft information (nullable)
    val live: LiveFlightData?            // Real-time flight data if available (nullable)
)

// Details about an airport including timings and delay info
data class AirportInfo(
    val airport: String,                 // Name of the airport
    val timezone: String,               // Timezone of the airport
    val iata: String,                   // IATA code (e.g., DEL)
    val icao: String,                   // ICAO code
    val terminal: String?,              // Terminal number (nullable)
    val gate: String?,                  // Gate number (nullable)
    val delay: Int?,                    // Delay in minutes (nullable)
    val scheduled: String,              // Scheduled departure/arrival time
    val estimated: String,              // Estimated time
    val actual: String?,                // Actual time if available (nullable)
    val estimated_runway: String?,      // Estimated runway time (nullable)
    val actual_runway: String?,         // Actual runway time (nullable)
    val airport_info: AirportLocationInfo? // Additional location data (nullable)
)

// Optional location metadata for airports
data class AirportLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val name: String?,                  // Airport name
    val country: String?,               // Country where airport is located
    val city: String?                   // City of the airport
)

// Airline operating the flight
data class Airline(
    val name: String,                   // Airline name
    val iata: String,                   // IATA code of airline
    val icao: String                    // ICAO code of airline
)

// Flight number and codes
data class FlightInfo(
    val number: String,                 // Flight number (e.g., 6E203)
    val iata: String,                   // IATA flight code
    val icao: String                    // ICAO flight code
)

// Aircraft details (can be null for some responses)
data class Aircraft(
    val registration: String?,          // Registration number (nullable)
    val iata: String?,                  // IATA aircraft code (nullable)
    val icao: String?,                  // ICAO aircraft code (nullable)
    val icao24: String?                 // ICAO24 hex code (nullable)
)

// Live flight tracking data, available if flight is in air or on ground
data class LiveFlightData(
    val updated: String,                // Last updated time
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,               // Altitude in meters
    val direction: Double,              // Heading in degrees
    val speed_horizontal: Double,       // Horizontal speed (km/h)
    val speed_vertical: Double,         // Vertical speed (m/s)
    val is_ground: Boolean              // Whether the aircraft is on the ground
)
