package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE is_active = 1")
    fun getAllActiveUsers(): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?
    
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE username = :username AND password_hash = :passwordHash")
    suspend fun authenticateUser(username: String, passwordHash: String): UserEntity?
    
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun isUsernameExists(username: String): Int
    
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun isEmailExists(email: String): Int
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("UPDATE users SET is_active = :isActive WHERE user_id = :userId")
    suspend fun updateUserActiveStatus(userId: Long, isActive: Boolean)
    
    @Query("UPDATE users SET password_hash = :newPasswordHash WHERE user_id = :userId")
    suspend fun updateUserPassword(userId: Long, newPasswordHash: String)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users WHERE user_id = :userId")
    suspend fun deleteUserById(userId: Long)
    
    @Query("DELETE FROM users WHERE is_active = 0")
    suspend fun deleteInactiveUsers()
}