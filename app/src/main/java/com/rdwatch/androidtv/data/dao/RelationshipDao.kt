package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.rdwatch.androidtv.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {
    
    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserWithSites(userId: Long): UserWithSites?
    
    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserWithModels(userId: Long): UserWithModels?
    
    @Transaction
    @Query("SELECT * FROM sites WHERE site_id = :siteId")
    suspend fun getSiteWithUsers(siteId: Long): SiteWithUsers?
    
    @Transaction
    @Query("SELECT * FROM sites WHERE site_id = :siteId")
    suspend fun getSiteWithObservations(siteId: Long): SiteWithObservations?
    
    @Transaction
    @Query("SELECT * FROM models WHERE model_id = :modelId")
    suspend fun getModelWithUsers(modelId: Long): ModelWithUsers?
    
    @Transaction
    @Query("SELECT * FROM models WHERE model_id = :modelId")
    suspend fun getModelWithObservations(modelId: Long): ModelWithObservations?
    
    @Transaction
    @Query("SELECT * FROM observations WHERE observation_id = :observationId")
    suspend fun getObservationWithSiteAndModel(observationId: Long): ObservationWithSiteAndModel?
    
    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserWithWatchProgress(userId: Long): Flow<UserWithWatchProgress?>
    
    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserWithLibrary(userId: Long): Flow<UserWithLibrary?>
    
    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserWithSearchHistory(userId: Long): Flow<UserWithSearchHistory?>
    
    @Transaction
    @Query("""
        SELECT u.* FROM users u
        INNER JOIN user_site_cross_ref usc ON u.user_id = usc.user_id
        WHERE usc.site_id = :siteId AND usc.is_active = 1
    """)
    fun getActiveUsersForSite(siteId: Long): Flow<List<UserEntity>>
    
    @Transaction
    @Query("""
        SELECT s.* FROM sites s
        INNER JOIN user_site_cross_ref usc ON s.site_id = usc.site_id
        WHERE usc.user_id = :userId AND usc.is_active = 1
    """)
    fun getActiveSitesForUser(userId: Long): Flow<List<SiteEntity>>
    
    @Transaction
    @Query("""
        SELECT m.* FROM models m
        INNER JOIN user_model_cross_ref umc ON m.model_id = umc.model_id
        WHERE umc.user_id = :userId AND umc.is_active = 1
    """)
    fun getAccessibleModelsForUser(userId: Long): Flow<List<ModelEntity>>
    
    @Transaction
    @Query("""
        SELECT o.* FROM observations o
        INNER JOIN sites s ON o.site_id = s.site_id
        INNER JOIN user_site_cross_ref usc ON s.site_id = usc.site_id
        WHERE usc.user_id = :userId AND usc.is_active = 1
        ORDER BY o.observation_date DESC
    """)
    fun getObservationsForUserSites(userId: Long): Flow<List<ObservationEntity>>
    
    @Transaction
    @Query("""
        SELECT COUNT(*) FROM observations o
        INNER JOIN sites s ON o.site_id = s.site_id
        INNER JOIN user_site_cross_ref usc ON s.site_id = usc.site_id
        WHERE usc.user_id = :userId AND usc.is_active = 1
    """)
    suspend fun getObservationCountForUser(userId: Long): Int
    
    @Transaction
    @Query("""
        SELECT DISTINCT s.* FROM sites s
        INNER JOIN observations o ON s.site_id = o.site_id
        WHERE o.model_id = :modelId
    """)
    fun getSitesWithModelObservations(modelId: Long): Flow<List<SiteEntity>>
}

data class UserWithSites(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "site_id",
        associateBy = androidx.room.Junction(
            value = UserSiteCrossRef::class,
            parentColumn = "user_id",
            entityColumn = "site_id"
        )
    )
    val sites: List<SiteEntity>
)

data class UserWithModels(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "model_id",
        associateBy = androidx.room.Junction(
            value = UserModelCrossRef::class,
            parentColumn = "user_id",
            entityColumn = "model_id"
        )
    )
    val models: List<ModelEntity>
)

data class SiteWithUsers(
    @Embedded val site: SiteEntity,
    @Relation(
        parentColumn = "site_id",
        entityColumn = "user_id",
        associateBy = androidx.room.Junction(
            value = UserSiteCrossRef::class,
            parentColumn = "site_id",
            entityColumn = "user_id"
        )
    )
    val users: List<UserEntity>
)

data class SiteWithObservations(
    @Embedded val site: SiteEntity,
    @Relation(
        parentColumn = "site_id",
        entityColumn = "site_id"
    )
    val observations: List<ObservationEntity>
)

data class ModelWithUsers(
    @Embedded val model: ModelEntity,
    @Relation(
        parentColumn = "model_id",
        entityColumn = "user_id",
        associateBy = androidx.room.Junction(
            value = UserModelCrossRef::class,
            parentColumn = "model_id",
            entityColumn = "user_id"
        )
    )
    val users: List<UserEntity>
)

data class ModelWithObservations(
    @Embedded val model: ModelEntity,
    @Relation(
        parentColumn = "model_id",
        entityColumn = "model_id"
    )
    val observations: List<ObservationEntity>
)

data class ObservationWithSiteAndModel(
    @Embedded val observation: ObservationEntity,
    @Relation(
        parentColumn = "site_id",
        entityColumn = "site_id"
    )
    val site: SiteEntity?,
    @Relation(
        parentColumn = "model_id",
        entityColumn = "model_id"
    )
    val model: ModelEntity?
)

data class UserWithWatchProgress(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id"
    )
    val watchProgress: List<WatchProgressEntity>
)

data class UserWithLibrary(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id"
    )
    val library: List<LibraryEntity>
)

data class UserWithSearchHistory(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id"
    )
    val searchHistory: List<SearchHistoryEntity>
)