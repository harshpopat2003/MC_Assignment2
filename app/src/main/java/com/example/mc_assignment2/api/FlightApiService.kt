package com.example.mc_assignment2.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FlightApiService {
    companion object {
        const val BASE_URL = "https://api.aviationstack.com/v1/"
        const val API_KEY = "05002fe4c4848c239cea5d96748bf5ba" // In a real app, use BuildConfig or secure storage
    }

    @GET("flights")
    suspend fun getFlightByNumber(
        @Query("access_key") apiKey: String = API_KEY,
        @Query("flight_iata") flightNumber: String
    ): Response<FlightResponse>

    @GET("flights")
    suspend fun getFlightsByRoute(
        @Query("access_key") apiKey: String = API_KEY,
        @Query("dep_iata") departureAirport: String,
        @Query("arr_iata") arrivalAirport: String
    ): Response<FlightResponse>
}

// Data models for API responses
data class FlightResponse(
    val pagination: Pagination,
    val data: List<FlightData>
)

data class Pagination(
    val limit: Int,
    val offset: Int,
    val count: Int,
    val total: Int
)

data class FlightData(
    val flight_date: String,
    val flight_status: String?,
    val departure: AirportInfo,
    val arrival: AirportInfo,
    val airline: Airline,
    val flight: FlightInfo,
    val aircraft: Aircraft?,
    val live: LiveFlightData?
)

data class AirportInfo(
    val airport: String,
    val timezone: String,
    val iata: String,
    val icao: String,
    val terminal: String?,
    val gate: String?,
    val delay: Int?,
    val scheduled: String,
    val estimated: String,
    val actual: String?,
    val estimated_runway: String?,
    val actual_runway: String?,
    val airport_info: AirportLocationInfo?
)

data class AirportLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val country: String?,
    val city: String?
)

data class Airline(
    val name: String,
    val iata: String,
    val icao: String
)

data class FlightInfo(
    val number: String,
    val iata: String,
    val icao: String
)

data class Aircraft(
    val registration: String?,
    val iata: String?,
    val icao: String?,
    val icao24: String?
)

data class LiveFlightData(
    val updated: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val direction: Double,
    val speed_horizontal: Double,
    val speed_vertical: Double,
    val is_ground: Boolean
)
