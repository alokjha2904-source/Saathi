package com.saathi.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saathi.app.data.model.EmergencyEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyEventDao {
    @Query("SELECT * FROM emergency_events ORDER BY timestamp DESC")
    fun getAllEmergencyEvents(): Flow<List<EmergencyEvent>>

    @Query("SELECT * FROM emergency_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EmergencyEvent>>

    @Query("SELECT * FROM emergency_events WHERE id = :id")
    suspend fun getEmergencyEventById(id: Int): EmergencyEvent?

    @Query("SELECT * FROM emergency_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEmergencyEventByEventId(eventId: String): EmergencyEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyEvent(event: EmergencyEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EmergencyEvent)

    @Update
    suspend fun updateEmergencyEvent(event: EmergencyEvent)

    @Update
    suspend fun updateEvent(event: EmergencyEvent)

    @Delete
    suspend fun deleteEmergencyEvent(event: EmergencyEvent)

    @Delete
    suspend fun deleteEvent(event: EmergencyEvent)

    @Query("DELETE FROM emergency_events")
    suspend fun deleteAllEmergencyEvents()

    @Query("DELETE FROM emergency_events")
    suspend fun clearAll()
}
