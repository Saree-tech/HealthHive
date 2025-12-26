package com.example.healthhive.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEventDao {

    @Query("SELECT * FROM health_events WHERE userId = :uid")
    fun getAllEventsFlow(uid: String): Flow<List<HealthEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: HealthEvent)

    @Update
    suspend fun updateEvent(event: HealthEvent)

    @Query("DELETE FROM health_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("SELECT * FROM health_events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): HealthEvent?

    @Query("DELETE FROM health_events")
    suspend fun deleteAll()
}