package com.example.mc_assignment2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room Database implementation for storing flight data locally.
 * Uses a single entity: [FlightEntity].
 */
@Database(entities = [FlightEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FlightDatabase : RoomDatabase() {

    /**
     * Returns the DAO (Data Access Object) for performing database operations.
     */
    abstract fun flightDao(): FlightDao

    companion object {
        @Volatile
        private var INSTANCE: FlightDatabase? = null

        /**
         * Returns a singleton instance of [FlightDatabase].
         * Ensures only one instance of the database is created across the entire app.
         */
        fun getDatabase(context: Context): FlightDatabase {
            // If INSTANCE is already initialized, return it.
            return INSTANCE ?: synchronized(this) {
                // Otherwise, build the database.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                    "flight_database" // Name of the local database file
                )
                    .fallbackToDestructiveMigration() // Rebuilds the DB if version changes (use with care!)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
