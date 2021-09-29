package com.raywenderlich.placebook.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import com.raywenderlich.placebook.model.Bookmark

/* Defines CRUD database operations
* Create new objects in the database
* Read objects from the database
* Update objects in the database
* Delete objects in the database */

// Data access object
@Dao
interface BookmarkDao {

    // Read all of the bookmarks from the database and return them as a List of bookmarks
    // LiveData objects can be observed by another object
    @Query("SELECT * FROM Bookmark")
    fun loadAll(): LiveData<List<Bookmark>>

    // Return a single Bookmark object
    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadBookmark(bookmarkId: Long): Bookmark
    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadLiveBookmark(bookmarkId: Long): LiveData<Bookmark>

    // Save a single Bookmark to the db and return the new primary key ID
    @Insert(onConflict = IGNORE)
    fun insertBookmark(bookmark: Bookmark): Long

    @Update(onConflict = REPLACE)
    fun updateBookmark(bookmark: Bookmark)

    @Delete
    fun deleteBookmark(bookmark: Bookmark)
}