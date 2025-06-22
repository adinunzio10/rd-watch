package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.UserModelCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface UserModelCrossRefDao {
    
    @Query("SELECT * FROM user_model_cross_ref WHERE is_active = 1")
    fun getAllActiveUserModelRelations(): Flow<List<UserModelCrossRef>>
    
    @Query("SELECT * FROM user_model_cross_ref WHERE user_id = :userId AND is_active = 1")
    fun getModelsByUser(userId: Long): Flow<List<UserModelCrossRef>>
    
    @Query("SELECT * FROM user_model_cross_ref WHERE model_id = :modelId AND is_active = 1")
    fun getUsersByModel(modelId: Long): Flow<List<UserModelCrossRef>>
    
    @Query("SELECT * FROM user_model_cross_ref WHERE user_id = :userId AND model_id = :modelId")
    suspend fun getUserModelRelation(userId: Long, modelId: Long): UserModelCrossRef?
    
    @Query("SELECT * FROM user_model_cross_ref WHERE user_id = :userId AND permission_level = :permission AND is_active = 1")
    fun getModelsByUserAndPermission(userId: Long, permission: String): Flow<List<UserModelCrossRef>>
    
    @Query("SELECT * FROM user_model_cross_ref WHERE model_id = :modelId AND permission_level = :permission AND is_active = 1")
    fun getUsersByModelAndPermission(modelId: Long, permission: String): Flow<List<UserModelCrossRef>>
    
    @Query("SELECT DISTINCT permission_level FROM user_model_cross_ref WHERE user_id = :userId AND is_active = 1")
    suspend fun getUserPermissions(userId: Long): List<String>
    
    @Query("SELECT COUNT(*) FROM user_model_cross_ref WHERE user_id = :userId AND model_id = :modelId AND is_active = 1")
    suspend fun hasUserModelAccess(userId: Long, modelId: Long): Int
    
    @Query("SELECT COUNT(*) FROM user_model_cross_ref WHERE user_id = :userId AND permission_level = :permission AND is_active = 1")
    suspend fun getModelCountByUserAndPermission(userId: Long, permission: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM user_model_cross_ref 
        WHERE user_id = :userId AND model_id = :modelId 
        AND permission_level IN ('write', 'admin') AND is_active = 1
    """)
    suspend fun hasUserWriteAccess(userId: Long, modelId: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserModelRelation(userModelCrossRef: UserModelCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserModelRelations(userModelCrossRefs: List<UserModelCrossRef>)
    
    @Update
    suspend fun updateUserModelRelation(userModelCrossRef: UserModelCrossRef)
    
    @Query("UPDATE user_model_cross_ref SET permission_level = :permission WHERE user_id = :userId AND model_id = :modelId")
    suspend fun updateUserModelPermission(userId: Long, modelId: Long, permission: String)
    
    @Query("UPDATE user_model_cross_ref SET is_active = :isActive WHERE user_id = :userId AND model_id = :modelId")
    suspend fun updateUserModelActiveStatus(userId: Long, modelId: Long, isActive: Boolean)
    
    @Delete
    suspend fun deleteUserModelRelation(userModelCrossRef: UserModelCrossRef)
    
    @Query("DELETE FROM user_model_cross_ref WHERE user_id = :userId AND model_id = :modelId")
    suspend fun deleteUserModelRelation(userId: Long, modelId: Long)
    
    @Query("DELETE FROM user_model_cross_ref WHERE user_id = :userId")
    suspend fun deleteAllRelationsForUser(userId: Long)
    
    @Query("DELETE FROM user_model_cross_ref WHERE model_id = :modelId")
    suspend fun deleteAllRelationsForModel(modelId: Long)
    
    @Query("DELETE FROM user_model_cross_ref WHERE is_active = 0")
    suspend fun deleteInactiveRelations()
}