package com.rdwatch.androidtv.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.entities.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    
    private lateinit var userDao: UserDao
    private lateinit var db: AppDatabase
    
    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        userDao = db.userDao()
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
    
    @Test
    @Throws(Exception::class)
    fun insertAndGetUser() = runTest {
        val user = UserEntity(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            createdAt = Date(),
            updatedAt = Date()
        )
        
        val userId = userDao.insertUser(user)
        val byId = userDao.getUserById(userId)
        
        assert(byId != null)
        assert(byId!!.username == "testuser")
        assert(byId.email == "test@example.com")
    }
    
    @Test
    @Throws(Exception::class)
    fun getUserByUsername() = runTest {
        val user = UserEntity(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            createdAt = Date(),
            updatedAt = Date()
        )
        
        userDao.insertUser(user)
        val byUsername = userDao.getUserByUsername("testuser")
        
        assert(byUsername != null)
        assert(byUsername!!.email == "test@example.com")
    }
    
    @Test
    @Throws(Exception::class)
    fun getAllActiveUsers() = runTest {
        val user1 = UserEntity(
            username = "user1",
            email = "user1@example.com",
            passwordHash = "hash1",
            createdAt = Date(),
            updatedAt = Date(),
            isActive = true
        )
        
        val user2 = UserEntity(
            username = "user2",
            email = "user2@example.com",
            passwordHash = "hash2",
            createdAt = Date(),
            updatedAt = Date(),
            isActive = false
        )
        
        userDao.insertUser(user1)
        userDao.insertUser(user2)
        
        val activeUsers = userDao.getAllActiveUsers().first()
        assert(activeUsers.size == 1)
        assert(activeUsers[0].username == "user1")
    }
    
    @Test
    @Throws(Exception::class)
    fun uniqueConstraints() = runTest {
        val user1 = UserEntity(
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hash1",
            createdAt = Date(),
            updatedAt = Date()
        )
        
        val user2 = UserEntity(
            username = "testuser", // Duplicate username
            email = "different@example.com",
            passwordHash = "hash2",
            createdAt = Date(),
            updatedAt = Date()
        )
        
        userDao.insertUser(user1)
        
        try {
            userDao.insertUser(user2)
            assert(false) // Should not reach here
        } catch (e: Exception) {
            // Expected exception due to unique constraint violation
            assert(true)
        }
    }
}