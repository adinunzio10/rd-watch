package com.rdwatch.androidtv.network.api

import com.rdwatch.androidtv.network.models.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class RealDebridApiServiceTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: RealDebridApiService
    private lateinit var moshi: Moshi
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        
        apiService = retrofit.create(RealDebridApiService::class.java)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `getUserInfo returns user information`() = runBlocking {
        // Given
        val jsonResponse = """
            {
                "id": 12345,
                "username": "testuser",
                "email": "test@example.com",
                "points": 1000,
                "locale": "en",
                "avatar": "https://example.com/avatar.jpg",
                "type": "premium",
                "premium": 1,
                "expiration": "2024-12-31T23:59:59Z"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
        )
        
        // When
        val response = apiService.getUserInfo()
        
        // Then
        assertTrue(response.isSuccessful)
        val userInfo = response.body()
        assertNotNull(userInfo)
        assertEquals(12345L, userInfo?.id)
        assertEquals("testuser", userInfo?.username)
        assertEquals("test@example.com", userInfo?.email)
        assertEquals(1000, userInfo?.points)
        
        val request = mockWebServer.takeRequest()
        assertEquals("/user", request.path)
        assertEquals("GET", request.method)
    }
    
    @Test
    fun `unrestrictLink returns unrestricted link`() = runBlocking {
        // Given
        val jsonResponse = """
            {
                "id": "ABC123",
                "filename": "video.mp4",
                "mimeType": "video/mp4",
                "filesize": 1073741824,
                "link": "https://example.com/original",
                "host": "example.com",
                "chunks": 8,
                "crc": 12345,
                "download": "https://download.real-debrid.com/video.mp4",
                "streamable": 1
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
        )
        
        // When
        val response = apiService.unrestrictLink("https://example.com/video")
        
        // Then
        assertTrue(response.isSuccessful)
        val unrestrictedLink = response.body()
        assertNotNull(unrestrictedLink)
        assertEquals("ABC123", unrestrictedLink?.id)
        assertEquals("video.mp4", unrestrictedLink?.filename)
        assertEquals(1073741824L, unrestrictedLink?.filesize)
        assertEquals("https://download.real-debrid.com/video.mp4", unrestrictedLink?.download)
        assertEquals(1, unrestrictedLink?.streamable)
        
        val request = mockWebServer.takeRequest()
        assertEquals("/unrestrict/link", request.path)
        assertEquals("POST", request.method)
    }
    
    @Test
    fun `getTorrents returns list of torrents`() = runBlocking {
        // Given
        val jsonResponse = """
            [
                {
                    "id": "TORRENT1",
                    "filename": "Movie.2023.1080p.mkv",
                    "hash": "1234567890abcdef",
                    "bytes": 2147483648,
                    "original_filename": "Original.Movie.mkv",
                    "host": "real-debrid.com",
                    "split": 100,
                    "progress": 100.0,
                    "status": "downloaded",
                    "added": "2023-01-01T12:00:00Z",
                    "ended": "2023-01-01T12:30:00Z",
                    "speed": 0,
                    "seeders": 10,
                    "links": ["link1", "link2"]
                }
            ]
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
        )
        
        // When
        val response = apiService.getTorrents()
        
        // Then
        assertTrue(response.isSuccessful)
        val torrents = response.body()
        assertNotNull(torrents)
        assertEquals(1, torrents?.size)
        
        val torrent = torrents?.first()
        assertEquals("TORRENT1", torrent?.id)
        assertEquals("Movie.2023.1080p.mkv", torrent?.filename)
        assertEquals(100f, torrent?.progress)
        assertEquals("downloaded", torrent?.status)
        
        val request = mockWebServer.takeRequest()
        assertEquals("/torrents", request.path)
        assertEquals("GET", request.method)
    }
    
    @Test
    fun `handles error responses correctly`() = runBlocking {
        // Given
        val errorResponse = """
            {
                "error": "bad_token",
                "error_code": 401,
                "error_details": "Invalid authentication token"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
        )
        
        // When
        val response = apiService.getUserInfo()
        
        // Then
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
        
        val errorBody = response.errorBody()?.string()
        assertNotNull(errorBody)
        assertTrue(errorBody!!.contains("bad_token"))
    }
}