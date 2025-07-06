package com.rdwatch.androidtv.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.entities.FileBrowserPreferencesEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class FileBrowserPreferencesDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var preferencesDao: FileBrowserPreferencesDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        preferencesDao = database.fileBrowserPreferencesDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetPreferencesForUser() = runTest {
        val testPrefs = createTestPreferences("user1")
        
        preferencesDao.insertPreferences(testPrefs)
        val retrieved = preferencesDao.getPreferencesForUser("user1")
        
        assertNotNull(retrieved)
        assertEquals(testPrefs.userId, retrieved.userId)
        assertEquals(testPrefs.defaultSortOption, retrieved.defaultSortOption)
        assertEquals(testPrefs.defaultViewMode, retrieved.defaultViewMode)
    }

    @Test
    fun getPreferencesForUserFlow() = runTest {
        val testPrefs = createTestPreferences("user2")
        
        preferencesDao.insertPreferences(testPrefs)
        val flow = preferencesDao.getPreferencesForUserFlow("user2")
        val retrieved = flow.first()
        
        assertNotNull(retrieved)
        assertEquals(testPrefs.userId, retrieved.userId)
    }

    @Test
    fun updateSpecificPreferences() = runTest {
        val testPrefs = createTestPreferences("user3")
        preferencesDao.insertPreferences(testPrefs)
        
        preferencesDao.updateDefaultSortOption("user3", "SIZE_DESC")
        preferencesDao.updateDefaultViewMode("user3", "LIST")
        preferencesDao.updateShowHiddenFiles("user3", true)
        
        val retrieved = preferencesDao.getPreferencesForUser("user3")
        assertNotNull(retrieved)
        assertEquals("SIZE_DESC", retrieved.defaultSortOption)
        assertEquals("LIST", retrieved.defaultViewMode)
        assertTrue(retrieved.showHiddenFiles)
    }

    @Test
    fun getDefaultSortOption() = runTest {
        val testPrefs = createTestPreferences("user4", sortOption = "NAME_ASC")
        preferencesDao.insertPreferences(testPrefs)
        
        val sortOption = preferencesDao.getDefaultSortOption("user4")
        assertEquals("NAME_ASC", sortOption)
        
        val nonExistentUser = preferencesDao.getDefaultSortOption("nonexistent")
        assertNull(nonExistentUser)
    }

    @Test
    fun hasPreferencesForUser() = runTest {
        val testPrefs = createTestPreferences("user5")
        
        var hasPrefs = preferencesDao.hasPreferencesForUser("user5")
        assertEquals(false, hasPrefs)
        
        preferencesDao.insertPreferences(testPrefs)
        
        hasPrefs = preferencesDao.hasPreferencesForUser("user5")
        assertTrue(hasPrefs)
    }

    @Test
    fun getOrCreatePreferencesForUser() = runTest {
        // Test creating new preferences
        val newPrefs = preferencesDao.getOrCreatePreferencesForUser("newuser")
        assertNotNull(newPrefs)
        assertEquals("newuser", newPrefs.userId)
        assertEquals("DATE_DESC", newPrefs.defaultSortOption)
        
        // Test getting existing preferences
        val existingPrefs = preferencesDao.getOrCreatePreferencesForUser("newuser")
        assertNotNull(existingPrefs)
        assertEquals("newuser", existingPrefs.userId)
    }

    @Test
    fun updateFavoriteFileIds() = runTest {
        val testPrefs = createTestPreferences("user6")
        preferencesDao.insertPreferences(testPrefs)
        
        val favoriteIds = listOf("file1", "file2", "file3")
        preferencesDao.updateFavoriteFileIds("user6", favoriteIds)
        
        val retrieved = preferencesDao.getPreferencesForUser("user6")
        assertNotNull(retrieved)
        assertEquals(favoriteIds, retrieved.favoriteFileIds)
    }

    @Test
    fun getFavoriteFileIds() = runTest {
        val favoriteIds = listOf("file1", "file2", "file3")
        val testPrefs = createTestPreferences("user7", favoriteIds = favoriteIds)
        preferencesDao.insertPreferences(testPrefs)
        
        val retrievedIds = preferencesDao.getFavoriteFileIds("user7")
        assertEquals(favoriteIds, retrievedIds)
    }

    @Test
    fun updateAutoRefreshInterval() = runTest {
        val testPrefs = createTestPreferences("user8")
        preferencesDao.insertPreferences(testPrefs)
        
        preferencesDao.updateAutoRefreshInterval("user8", 600)
        
        val retrieved = preferencesDao.getPreferencesForUser("user8")
        assertNotNull(retrieved)
        assertEquals(600, retrieved.autoRefreshInterval)
    }

    @Test
    fun getAllUserIds() = runTest {
        val users = listOf("user1", "user2", "user3")
        users.forEach { userId ->
            val prefs = createTestPreferences(userId)
            preferencesDao.insertPreferences(prefs)
        }
        
        val allUserIds = preferencesDao.getAllUserIds()
        assertEquals(3, allUserIds.size)
        assertTrue(allUserIds.containsAll(users))
    }

    @Test
    fun deletePreferencesForUser() = runTest {
        val testPrefs = createTestPreferences("user9")
        preferencesDao.insertPreferences(testPrefs)
        
        preferencesDao.deletePreferencesForUser("user9")
        val retrieved = preferencesDao.getPreferencesForUser("user9")
        
        assertNull(retrieved)
    }

    @Test
    fun getPreferencesCount() = runTest {
        val testPrefs = listOf(
            createTestPreferences("user1"),
            createTestPreferences("user2"),
            createTestPreferences("user3")
        )
        
        preferencesDao.insertPreferences(testPrefs)
        val count = preferencesDao.getPreferencesCount()
        
        assertEquals(3, count)
    }

    @Test
    fun updateOrCreateSortOption() = runTest {
        // Test creating new preferences with sort option
        preferencesDao.updateOrCreateSortOption("newuser", "SIZE_ASC")
        
        val prefs = preferencesDao.getPreferencesForUser("newuser")
        assertNotNull(prefs)
        assertEquals("SIZE_ASC", prefs.defaultSortOption)
        
        // Test updating existing preferences
        preferencesDao.updateOrCreateSortOption("newuser", "NAME_DESC")
        
        val updatedPrefs = preferencesDao.getPreferencesForUser("newuser")
        assertNotNull(updatedPrefs)
        assertEquals("NAME_DESC", updatedPrefs.defaultSortOption)
    }

    @Test
    fun updateOrCreateViewMode() = runTest {
        // Test creating new preferences with view mode
        preferencesDao.updateOrCreateViewMode("newuser2", "LIST")
        
        val prefs = preferencesDao.getPreferencesForUser("newuser2")
        assertNotNull(prefs)
        assertEquals("LIST", prefs.defaultViewMode)
        
        // Test updating existing preferences
        preferencesDao.updateOrCreateViewMode("newuser2", "GRID")
        
        val updatedPrefs = preferencesDao.getPreferencesForUser("newuser2")
        assertNotNull(updatedPrefs)
        assertEquals("GRID", updatedPrefs.defaultViewMode)
    }

    private fun createTestPreferences(
        userId: String,
        sortOption: String = "DATE_DESC",
        viewMode: String = "GRID",
        showHiddenFiles: Boolean = false,
        autoRefreshInterval: Int = 300,
        favoriteIds: List<String> = emptyList()
    ): FileBrowserPreferencesEntity {
        return FileBrowserPreferencesEntity(
            userId = userId,
            defaultSortOption = sortOption,
            defaultViewMode = viewMode,
            showHiddenFiles = showHiddenFiles,
            autoRefreshInterval = autoRefreshInterval,
            defaultFilterTypes = emptyList(),
            favoriteFileIds = favoriteIds,
            lastUpdated = System.currentTimeMillis()
        )
    }
}