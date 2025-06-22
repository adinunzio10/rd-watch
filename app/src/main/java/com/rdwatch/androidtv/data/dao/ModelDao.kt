package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    
    @Query("SELECT * FROM models WHERE is_active = 1 ORDER BY created_at DESC")
    fun getAllActiveModels(): Flow<List<ModelEntity>>
    
    @Query("SELECT * FROM models ORDER BY created_at DESC")
    fun getAllModels(): Flow<List<ModelEntity>>
    
    @Query("SELECT * FROM models WHERE model_id = :modelId")
    suspend fun getModelById(modelId: Long): ModelEntity?
    
    @Query("SELECT * FROM models WHERE model_name = :modelName")
    suspend fun getModelByName(modelName: String): ModelEntity?
    
    @Query("SELECT * FROM models WHERE model_name = :modelName AND model_version = :version")
    suspend fun getModelByNameAndVersion(modelName: String, version: String): ModelEntity?
    
    @Query("SELECT * FROM models WHERE model_type = :modelType AND is_active = 1 ORDER BY created_at DESC")
    fun getModelsByType(modelType: String): Flow<List<ModelEntity>>
    
    @Query("SELECT * FROM models WHERE author = :author ORDER BY created_at DESC")
    fun getModelsByAuthor(author: String): Flow<List<ModelEntity>>
    
    @Query("SELECT * FROM models WHERE model_name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchModels(query: String): Flow<List<ModelEntity>>
    
    @Query("SELECT DISTINCT model_type FROM models WHERE is_active = 1 ORDER BY model_type")
    suspend fun getAllModelTypes(): List<String>
    
    @Query("SELECT DISTINCT author FROM models WHERE author IS NOT NULL ORDER BY author")
    suspend fun getAllAuthors(): List<String>
    
    @Query("SELECT COUNT(*) FROM models WHERE model_name = :modelName")
    suspend fun isModelNameExists(modelName: String): Int
    
    @Query("SELECT COUNT(*) FROM models WHERE model_name = :modelName AND model_version = :version")
    suspend fun isModelVersionExists(modelName: String, version: String): Int
    
    @Query("SELECT * FROM models WHERE model_name = :modelName ORDER BY model_version DESC")
    fun getModelVersionsByName(modelName: String): Flow<List<ModelEntity>>
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertModel(model: ModelEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)
    
    @Update
    suspend fun updateModel(model: ModelEntity)
    
    @Query("UPDATE models SET is_active = :isActive WHERE model_id = :modelId")
    suspend fun updateModelActiveStatus(modelId: Long, isActive: Boolean)
    
    @Query("UPDATE models SET accuracy_metrics = :metrics WHERE model_id = :modelId")
    suspend fun updateModelMetrics(modelId: Long, metrics: String)
    
    @Delete
    suspend fun deleteModel(model: ModelEntity)
    
    @Query("DELETE FROM models WHERE model_id = :modelId")
    suspend fun deleteModelById(modelId: Long)
    
    @Query("DELETE FROM models WHERE is_active = 0")
    suspend fun deleteInactiveModels()
}