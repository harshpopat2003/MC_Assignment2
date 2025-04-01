package com.example.mc_assignment2.utils

/**
 * Utility class for input validation
 */
object ValidationUtils {

    /**
     * Validates if a given string is a valid flight number
     * Format examples: AA123, DL4567, UA987, etc.
     * Generally: 2 letter airline code followed by 3-4 digits
     */
    fun isValidFlightNumber(flightNumber: String): Boolean {
        // Basic regex for flight number validation
        // Two letters followed by 3-4 digits
        val flightNumberRegex = Regex("^[A-Z]{2}\\d{3,4}$")

        return flightNumber.matches(flightNumberRegex)
    }

    /**
     * Validates if a given string is a valid IATA airport code
     * IATA codes are 3 letters, e.g., JFK, LAX, SFO
     */
    fun isValidAirportCode(airportCode: String): Boolean {
        // IATA airport code validation - 3 uppercase letters
        val airportCodeRegex = Regex("^[A-Z]{3}$")

        return airportCode.matches(airportCodeRegex)
    }

    /**
     * Validates if the given departure and arrival airports are different
     * Prevents users from searching for flights between the same airport
     */
    fun isValidRoute(departureAirport: String, arrivalAirport: String): Boolean {
        // First validate that both are valid airport codes
        if (!isValidAirportCode(departureAirport) || !isValidAirportCode(arrivalAirport)) {
            return false
        }

        // Ensure departure and arrival are different
        return departureAirport != arrivalAirport
    }
}