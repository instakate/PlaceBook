package com.raywenderlich.placebook.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raywenderlich.placebook.model.Bookmark

// Database class - @database identifies class to Room
@Database(entities = arrayOf(Bookmark::class), version = 1)


abstract class PlaceBookDatabase : RoomDatabase() {

    // Return a DAO interface
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        private var instance: PlaceBookDatabase? = null

        // Take in a Context and return the single PlaceBookDatabase instance
        // If first time getInstance is called, create the instance. Room.databaseBuilder()
        // creates a Room Database based on the abstract PlaceBookDatabase class.

        fun getInstance(context: Context): PlaceBookDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaceBookDatabase::class.java,
                    "PlaceBook").build()
            }
            return instance as PlaceBookDatabase
        }
    }
}