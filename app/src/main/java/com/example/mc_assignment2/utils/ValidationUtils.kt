package com.example.mc_assignment2.utils

/**
 * Utility object for validating flight-related inputs.
 */
object ValidationUtils {

    /**
     * Checks if a given flight number matches the expected format.
     *
     * Expected format:
     * - Two uppercase letters (airline code) followed by 3 or 4 digits.
     * - Examples: AA123, DL4567, UA987
     *
     * @param flightNumber The flight number to validate.
     * @return True if the format is valid, false otherwise.
     */
    fun isValidFlightNumber(flightNumber: String): Boolean {
        val flightNumberRegex = Regex("^[A-Z]{2}\\d{3,4}$")
        return flightNumber.matches(flightNumberRegex)
    }

    /**
     * Checks if the input string is a valid IATA airport code.
     *
     * IATA codes:
     * - Consist of exactly 3 uppercase letters.
     * - Examples: JFK, LAX, SFO
     *
     * @param airportCode The airport code to validate.
     * @return True if the code is valid, false otherwise.
     */
    fun isValidAirportCode(airportCode: String): Boolean {
        val airportCodeRegex = Regex("^[A-Z]{3}$")
        return airportCode.matches(airportCodeRegex)
    }

    /**
     * Validates a route by ensuring both airport codes are valid and different.
     *
     * @param departureAirport The departure airport IATA code.
     * @param arrivalAirport The arrival airport IATA code.
     * @return True if the route is valid, false if it's invalid or both codes are the same.
     */
    fun isValidRoute(departureAirport: String, arrivalAirport: String): Boolean {
        return isValidAirportCode(departureAirport) &&
                isValidAirportCode(arrivalAirport) &&
                departureAirport != arrivalAirport
    }
}
