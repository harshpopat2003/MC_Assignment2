package com.example.mc_assignment2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room Database implementation for storing flight data
 */
@Database(entities = [FlightEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FlightDatabase : RoomDatabase() {

    abstract fun flightDao(): FlightDao

    companion object {
        @Volatile
        private var INSTANCE: FlightDatabase? = null

        fun getDatabase(context: Context): FlightDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                    "flight_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}