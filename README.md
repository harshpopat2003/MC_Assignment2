# MC_Assignment2

# Flight Tracker App

A comprehensive Android application for tracking and analyzing flight data in real-time, providing users with detailed flight information, route statistics, and visual mapping of flight paths. This app combines modern Android development practices with intuitive UI design to create a valuable tool for travelers and aviation enthusiasts.

## Overview

Flight Tracker is a robust mobile application that allows users to:
- Track flights in real-time using their flight number
- View departure and arrival information with precise timing details
- Monitor flight delays and status updates
- Visualize flight routes on an interactive map
- Analyze historical flight data and route statistics

The application leverages the AviationStack API for retrieving flight information and stores data locally for offline access and statistical analysis.

## Features

### Real-time Flight Tracking
- Search for flights using IATA flight numbers (e.g., AA123, DL456)
- View real-time flight status and position updates
- Automatic refresh of flight data every minute
- Map visualization of flight routes with departure and arrival points

### Flight Information Display
- Detailed information about departure and arrival airports
- Scheduled and actual departure/arrival times
- Flight duration calculation
- Delay status information

### Data Collection and Analysis
- Background collection of flight data for popular routes
- Local storage of flight information for offline access
- Statistical analysis of routes including average flight durations and delays
- Route comparison functionality

### Technical Features
- Persistent data storage using Room database
- Clean architecture with separate API, database, and UI layers
- Background data collection using WorkManager
- Modern Android development with Kotlin, Coroutines, and Flow

## Architecture

The application follows a modern Android architecture with the following components:

### API Layer
- `FlightApiService`: Interface for the AviationStack API
- Data models for API responses

### Database Layer
- `FlightDatabase`: Room database for storing flight information
- `FlightDao`: Data access object for database operations
- `FlightEntity`: Entity representing a flight in the database

### Service Layer
- `FlightDataCollectionService`: Background service to initialize data collection
- `FlightDataWorker`: WorkManager worker for performing data collection
- `BootReceiver`: BroadcastReceiver for auto-starting the service on device boot

### UI Layer
- `MainActivity`: Entry point for the application, displays route statistics
- `FlightTrackingActivity`: Detailed view for tracking a specific flight

### Utility Layer
- `FlightTimeCalculator`: Utility for flight time calculations and formatting
- `ValidationUtils`: Input validation for flight numbers and airport codes

## Getting Started

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- Android SDK 21 or higher
- Google Maps API key (for map functionality)

### Installation
1. Clone this repository
2. Open the project in Android Studio
3. Replace the Google Maps API key in the `AndroidManifest.xml` file with your own key
4. Build and run the application on your device or emulator

### API Key Configuration
The application uses the AviationStack API for retrieving flight data. The API key is hardcoded in the `FlightApiService.kt` file, but in a production environment, it should be stored securely using BuildConfig variables or other secure storage methods.

```kotlin
const val API_KEY = "1e55772bb9e23418e51a0ba91bd100f5" // Replace with your API key
```

## Usage

### Tracking a Flight
1. Enter a valid flight number (e.g., AA123) in the search field
2. Tap "Track Flight" button
3. View real-time flight information and map visualization

### Viewing Route Statistics
The main screen displays statistics for various flight routes, including:
- Departure and arrival airports
- Average flight duration
- Calculated with actual delays

## Background Data Collection

The application collects flight data for popular routes in the background to build a comprehensive database for statistical analysis. This is implemented using WorkManager, which schedules periodic data collection tasks that run every 8 hours.

The following routes are monitored by default:
- JFK to LAX (New York to Los Angeles)
- LAX to SFO (Los Angeles to San Francisco)
- ORD to MIA (Chicago to Miami)
- ATL to DEN (Atlanta to Denver)
- DFW to SEA (Dallas to Seattle)

## User Interface

The application features a clean, intuitive user interface built with modern Material Design components to provide a seamless user experience.

### Main Screen
The main screen (`activity_main.xml`) serves as the entry point to the application and includes:
- A title header identifying the application
- A flight number input field with Material Design styling
- A prominent "Track Flight" button
- A statistics section showing route information
- A RecyclerView displaying flight statistics in card format

### Flight Tracking Screen
The flight tracking screen (`activity_flight_tracking.xml`) provides detailed information about a specific flight:
- Flight number and status display at the top
- A card view containing departure and arrival information
- Clear presentation of flight duration and delay information
- An embedded Google Maps fragment showing the flight route
- Live update indicator at the bottom of the screen

### List Items
The statistics list items (`item_flight_stat.xml`) are presented as cards containing:
- Route information (departure → arrival)
- Average flight duration

The UI components are arranged using ConstraintLayout to ensure proper positioning and scaling across different device sizes and orientations.

## Technical Details

### Data Models

#### Flight Entity
The core data model stores comprehensive flight information:
- Flight number and date
- Departure and arrival airports
- Scheduled and actual departure/arrival times
- Delay information
- Flight duration and status

#### Route Statistics
Aggregated data for flight routes:
- Departure and arrival airports
- Average flight duration (including delays)
- Statistical reliability metrics

### Network Requests
The application handles network requests using Retrofit and Coroutines. API responses are processed and stored in the local database for offline access.

### Map Visualization
Flight routes are visualized on Google Maps with:
- Markers for departure and arrival airports
- Flight path polyline
- Real-time aircraft position (when available)
- Fallback coordinates for unknown airports

### Error Handling
The application includes comprehensive error handling for:
- Network connectivity issues
- Missing or invalid flight data
- Map loading failures
- API rate limiting

## Implementation Details

### XML Layout Design
The application uses three main XML layouts:

1. **activity_main.xml**
   - Uses ConstraintLayout as the root element for flexible positioning
   - Incorporates Material Design components like TextInputLayout for enhanced user experience
   - Implements a RecyclerView with a custom adapter for displaying flight statistics
   - Separates content sections with visual dividers for clarity

2. **activity_flight_tracking.xml**
   - Structured with a ConstraintLayout to accommodate varying content sizes
   - Features a CardView to display flight details with a clean, elevated appearance
   - Embeds a Google Maps SupportMapFragment for route visualization
   - Includes error message displays that appear conditionally
   - Uses nested LinearLayouts within the CardView to organize information logically

3. **item_flight_stat.xml**
   - Implements CardView for a consistent design language throughout the app
   - Uses a horizontal LinearLayout for simple content organization
   - Presents route information and duration statistics in a clear, readable format

### Code Integration with UI
The application's Kotlin code interfaces with these layouts through:
- ViewBinding for type-safe access to UI elements
- RecyclerView adapters with DiffUtil for efficient list updates
- CoroutineFlow for reactive UI updates based on database changes
- Google Maps API integration for interactive map experiences


## Acknowledgments

- AviationStack API for flight data
- Google Maps for map visualization
- Android Jetpack libraries for modern Android development

