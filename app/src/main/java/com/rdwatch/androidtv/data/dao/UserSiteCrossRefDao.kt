package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.UserSiteCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSiteCrossRefDao {
    
    @Query("SELECT * FROM user_site_cross_ref WHERE is_active = 1")
    fun getAllActiveUserSiteRelations(): Flow<List<UserSiteCrossRef>>
    
    @Query("SELECT * FROM user_site_cross_ref WHERE user_id = :userId AND is_active = 1")
    fun getSitesByUser(userId: Long): Flow<List<UserSiteCrossRef>>
    
    @Query("SELECT * FROM user_site_cross_ref WHERE site_id = :siteId AND is_active = 1")
    fun getUsersBySite(siteId: Long): Flow<List<UserSiteCrossRef>>
    
    @Query("SELECT * FROM user_site_cross_ref WHERE user_id = :userId AND site_id = :siteId")
    suspend fun getUserSiteRelation(userId: Long, siteId: Long): UserSiteCrossRef?
    
    @Query("SELECT * FROM user_site_cross_ref WHERE user_id = :userId AND role = :role AND is_active = 1")
    fun getSitesByUserAndRole(userId: Long, role: String): Flow<List<UserSiteCrossRef>>
    
    @Query("SELECT * FROM user_site_cross_ref WHERE site_id = :siteId AND role = :role AND is_active = 1")
    fun getUsersBySiteAndRole(siteId: Long, role: String): Flow<List<UserSiteCrossRef>>
    
    @Query("SELECT DISTINCT role FROM user_site_cross_ref WHERE user_id = :userId AND is_active = 1")
    suspend fun getUserRoles(userId: Long): List<String>
    
    @Query("SELECT COUNT(*) FROM user_site_cross_ref WHERE user_id = :userId AND site_id = :siteId AND is_active = 1")
    suspend fun isUserAssignedToSite(userId: Long, siteId: Long): Int
    
    @Query("SELECT COUNT(*) FROM user_site_cross_ref WHERE user_id = :userId AND role = :role AND is_active = 1")
    suspend fun getSiteCountByUserAndRole(userId: Long, role: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSiteRelation(userSiteCrossRef: UserSiteCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSiteRelations(userSiteCrossRefs: List<UserSiteCrossRef>)
    
    @Update
    suspend fun updateUserSiteRelation(userSiteCrossRef: UserSiteCrossRef)
    
    @Query("UPDATE user_site_cross_ref SET role = :role WHERE user_id = :userId AND site_id = :siteId")
    suspend fun updateUserSiteRole(userId: Long, siteId: Long, role: String)
    
    @Query("UPDATE user_site_cross_ref SET is_active = :isActive WHERE user_id = :userId AND site_id = :siteId")
    suspend fun updateUserSiteActiveStatus(userId: Long, siteId: Long, isActive: Boolean)
    
    @Delete
    suspend fun deleteUserSiteRelation(userSiteCrossRef: UserSiteCrossRef)
    
    @Query("DELETE FROM user_site_cross_ref WHERE user_id = :userId AND site_id = :siteId")
    suspend fun deleteUserSiteRelation(userId: Long, siteId: Long)
    
    @Query("DELETE FROM user_site_cross_ref WHERE user_id = :userId")
    suspend fun deleteAllRelationsForUser(userId: Long)
    
    @Query("DELETE FROM user_site_cross_ref WHERE site_id = :siteId")
    suspend fun deleteAllRelationsForSite(siteId: Long)
    
    @Query("DELETE FROM user_site_cross_ref WHERE is_active = 0")
    suspend fun deleteInactiveRelations()
}