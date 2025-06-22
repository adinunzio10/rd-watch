package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.ObservationEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ObservationDao {
    
    @Query("SELECT * FROM observations ORDER BY observation_date DESC")
    fun getAllObservations(): Flow<List<ObservationEntity>>
    
    @Query("SELECT * FROM observations WHERE observation_id = :observationId")
    suspend fun getObservationById(observationId: Long): ObservationEntity?
    
    @Query("SELECT * FROM observations WHERE site_id = :siteId ORDER BY observation_date DESC")
    fun getObservationsBySite(siteId: Long): Flow<List<ObservationEntity>>
    
    @Query("SELECT * FROM observations WHERE model_id = :modelId ORDER BY observation_date DESC")
    fun getObservationsByModel(modelId: Long): Flow<List<ObservationEntity>>
    
    @Query("SELECT * FROM observations WHERE sensor_type = :sensorType ORDER BY observation_date DESC")
    fun getObservationsBySensorType(sensorType: String): Flow<List<ObservationEntity>>
    
    @Query("SELECT * FROM observations WHERE processing_status = :status ORDER BY observation_date DESC")
    fun getObservationsByStatus(status: String): Flow<List<ObservationEntity>>
    
    @Query("""
        SELECT * FROM observations 
        WHERE observation_date BETWEEN :startDate AND :endDate 
        ORDER BY observation_date DESC
    """)
    fun getObservationsByDateRange(startDate: Date, endDate: Date): Flow<List<ObservationEntity>>
    
    @Query("""
        SELECT * FROM observations 
        WHERE site_id = :siteId 
        AND observation_date BETWEEN :startDate AND :endDate 
        ORDER BY observation_date DESC
    """)
    fun getObservationsBySiteAndDateRange(
        siteId: Long, 
        startDate: Date, 
        endDate: Date
    ): Flow<List<ObservationEntity>>
    
    @Query("""
        SELECT * FROM observations 
        WHERE confidence_score >= :minConfidence 
        ORDER BY confidence_score DESC, observation_date DESC
    """)
    fun getObservationsWithMinConfidence(minConfidence: Float): Flow<List<ObservationEntity>>
    
    @Query("SELECT * FROM observations WHERE cloud_cover_percentage <= :maxCloudCover ORDER BY observation_date DESC")
    fun getObservationsWithMaxCloudCover(maxCloudCover: Float): Flow<List<ObservationEntity>>
    
    @Query("SELECT DISTINCT sensor_type FROM observations ORDER BY sensor_type")
    suspend fun getAllSensorTypes(): List<String>
    
    @Query("SELECT DISTINCT processing_status FROM observations ORDER BY processing_status")
    suspend fun getAllProcessingStatuses(): List<String>
    
    @Query("SELECT COUNT(*) FROM observations WHERE site_id = :siteId")
    suspend fun getObservationCountBySite(siteId: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: ObservationEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservations(observations: List<ObservationEntity>)
    
    @Update
    suspend fun updateObservation(observation: ObservationEntity)
    
    @Query("UPDATE observations SET processing_status = :status WHERE observation_id = :observationId")
    suspend fun updateObservationStatus(observationId: Long, status: String)
    
    @Query("UPDATE observations SET confidence_score = :score WHERE observation_id = :observationId")
    suspend fun updateObservationConfidence(observationId: Long, score: Float)
    
    @Delete
    suspend fun deleteObservation(observation: ObservationEntity)
    
    @Query("DELETE FROM observations WHERE observation_id = :observationId")
    suspend fun deleteObservationById(observationId: Long)
    
    @Query("DELETE FROM observations WHERE processing_status = 'failed'")
    suspend fun deleteFailedObservations()
}