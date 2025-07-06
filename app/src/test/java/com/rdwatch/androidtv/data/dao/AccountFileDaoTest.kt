package com.rdwatch.androidtv.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.entities.AccountFileEntity
import com.rdwatch.androidtv.ui.filebrowser.models.FileSource
import com.rdwatch.androidtv.ui.filebrowser.models.FileTypeCategory
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

@RunWith(AndroidJUnit4::class)
class AccountFileDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var accountFileDao: AccountFileDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        accountFileDao = database.accountFileDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetFile() = runTest {
        val testFile = createTestAccountFile("test1")
        
        accountFileDao.insertFile(testFile)
        val retrieved = accountFileDao.getFileById("test1")
        
        assertNotNull(retrieved)
        assertEquals(testFile.id, retrieved.id)
        assertEquals(testFile.filename, retrieved.filename)
    }

    @Test
    fun deleteFile() = runTest {
        val testFile = createTestAccountFile("test2")
        
        accountFileDao.insertFile(testFile)
        accountFileDao.deleteFileById("test2")
        val retrieved = accountFileDao.getFileById("test2")
        
        assertNull(retrieved)
    }

    @Test
    fun getAllFiles() = runTest {
        val testFiles = listOf(
            createTestAccountFile("test3"),
            createTestAccountFile("test4"),
            createTestAccountFile("test5")
        )
        
        accountFileDao.insertFiles(testFiles)
        val allFiles = accountFileDao.getAllFiles().first()
        
        assertEquals(3, allFiles.size)
    }

    @Test
    fun getFilesBySource() = runTest {
        val downloadFile = createTestAccountFile("download1", source = FileSource.DOWNLOAD)
        val torrentFile = createTestAccountFile("torrent1", source = FileSource.TORRENT)
        
        accountFileDao.insertFiles(listOf(downloadFile, torrentFile))
        
        val downloadFiles = accountFileDao.getFilesBySource(FileSource.DOWNLOAD).first()
        val torrentFiles = accountFileDao.getFilesBySource(FileSource.TORRENT).first()
        
        assertEquals(1, downloadFiles.size)
        assertEquals(1, torrentFiles.size)
        assertEquals("download1", downloadFiles.first().id)
        assertEquals("torrent1", torrentFiles.first().id)
    }

    @Test
    fun getFilesByType() = runTest {
        val videoFile = createTestAccountFile("video1", fileType = FileTypeCategory.VIDEO)
        val audioFile = createTestAccountFile("audio1", fileType = FileTypeCategory.AUDIO)
        
        accountFileDao.insertFiles(listOf(videoFile, audioFile))
        
        val videoFiles = accountFileDao.getFilesByType(FileTypeCategory.VIDEO).first()
        val audioFiles = accountFileDao.getFilesByType(FileTypeCategory.AUDIO).first()
        
        assertEquals(1, videoFiles.size)
        assertEquals(1, audioFiles.size)
        assertEquals("video1", videoFiles.first().id)
        assertEquals("audio1", audioFiles.first().id)
    }

    @Test
    fun searchFiles() = runTest {
        val testFiles = listOf(
            createTestAccountFile("test1", filename = "movie.mp4"),
            createTestAccountFile("test2", filename = "song.mp3"),
            createTestAccountFile("test3", filename = "another_movie.mkv")
        )
        
        accountFileDao.insertFiles(testFiles)
        val searchResults = accountFileDao.searchFiles("movie").first()
        
        assertEquals(2, searchResults.size)
    }

    @Test
    fun getFileCount() = runTest {
        val testFiles = listOf(
            createTestAccountFile("test1"),
            createTestAccountFile("test2"),
            createTestAccountFile("test3")
        )
        
        accountFileDao.insertFiles(testFiles)
        val count = accountFileDao.getFileCount()
        
        assertEquals(3, count)
    }

    private fun createTestAccountFile(
        id: String,
        filename: String = "test_file.mp4",
        source: FileSource = FileSource.DOWNLOAD,
        fileType: FileTypeCategory = FileTypeCategory.VIDEO
    ): AccountFileEntity {
        return AccountFileEntity(
            id = id,
            filename = filename,
            filesize = 1024L,
            source = source,
            mimeType = "video/mp4",
            downloadUrl = "https://example.com/download",
            streamUrl = null,
            host = "example.com",
            dateAdded = System.currentTimeMillis(),
            isStreamable = true,
            parentTorrentId = null,
            parentTorrentName = null,
            torrentProgress = null,
            torrentStatus = null,
            fileTypeCategory = fileType,
            fileExtension = "mp4",
            lastUpdated = System.currentTimeMillis(),
            alternativeUrls = emptyList()
        )
    }
}