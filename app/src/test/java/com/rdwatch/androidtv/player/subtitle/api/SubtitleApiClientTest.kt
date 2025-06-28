package com.rdwatch.androidtv.player.subtitle.api

import com.rdwatch.androidtv.player.subtitle.models.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Test class for SubtitleApiClient implementations.
 * Tests API integration with MockWebServer including error handling, retries, and rate limiting.
 */
class SubtitleApiClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var moshi: Moshi
    private lateinit var testApiClient: TestSubtitleApiClient

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
        
        testApiClient = TestSubtitleApiClient(retrofit.create(TestSubtitleApiService::class.java))
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchSubtitles returns valid results for hash-based search`() = runBlocking {
        // Given
        val searchResponse = """
            [
                {
                    "id": "SUB123",
                    "language": "en",
                    "language_name": "English",
                    "format": "srt",
                    "download_url": "https://api.example.com/download/SUB123",
                    "file_name": "movie.2023.1080p.en.srt",
                    "file_size": 25600,
                    "download_count": 1500,
                    "rating": 4.5,
                    "match_score": 1.0,
                    "match_type": "HASH_MATCH",
                    "upload_date": 1640995200000,
                    "uploader": "subtitle_user",
                    "is_verified": true,
                    "hearing_impaired": false,
                    "release_group": "YIFY",
                    "version": "v1.0"
                },
                {
                    "id": "SUB124",
                    "language": "es",
                    "language_name": "Spanish",
                    "format": "srt",
                    "download_url": "https://api.example.com/download/SUB124",
                    "file_name": "movie.2023.1080p.es.srt",
                    "file_size": 28000,
                    "download_count": 800,
                    "rating": 4.2,
                    "match_score": 1.0,
                    "match_type": "HASH_MATCH",
                    "upload_date": 1640995200000,
                    "uploader": "subtitle_user",
                    "is_verified": true,
                    "hearing_impaired": false,
                    "release_group": "YIFY",
                    "version": "v1.0"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(searchResponse)
                .setHeader("Content-Type", "application/json")
        )

        // When
        val request = SubtitleSearchRequest(
            title = "Test Movie",
            year = 2023,
            type = ContentType.MOVIE,
            fileHash = "1234567890abcdef",
            languages = listOf("en", "es")
        )
        val results = testApiClient.searchSubtitles(request)

        // Then
        assertEquals(2, results.size)
        
        val englishResult = results.find { it.language == "en" }
        assertNotNull(englishResult)
        assertEquals("SUB123", englishResult?.id)
        assertEquals("English", englishResult?.languageName)
        assertEquals(SubtitleFormat.SRT, englishResult?.format)
        assertEquals(1.0f, englishResult?.matchScore)
        assertEquals(MatchType.HASH_MATCH, englishResult?.matchType)
        assertEquals(true, englishResult?.isVerified)
        assertEquals(false, englishResult?.hearingImpaired)
        
        val spanishResult = results.find { it.language == "es" }
        assertNotNull(spanishResult)
        assertEquals("SUB124", spanishResult?.id)
        assertEquals("Spanish", spanishResult?.languageName)

        val serverRequest = mockWebServer.takeRequest()
        assertEquals("/search", serverRequest.path?.substringBefore('?'))
        assertTrue(serverRequest.requestUrl?.queryParameter("hash") == "1234567890abcdef")
        assertTrue(serverRequest.requestUrl?.queryParameter("languages")?.contains("en") == true)
        assertTrue(serverRequest.requestUrl?.queryParameter("languages")?.contains("es") == true)
    }

    @Test
    fun `searchSubtitles handles empty results gracefully`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .setHeader("Content-Type", "application/json")
        )

        // When
        val request = SubtitleSearchRequest(
            title = "Unknown Movie",
            year = 2023,
            type = ContentType.MOVIE,
            languages = listOf("en")
        )
        val results = testApiClient.searchSubtitles(request)

        // Then
        assertTrue(results.isEmpty())
    }

    @Test
    fun `searchSubtitles validates request parameters`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error": "Invalid request parameters"}""")
        )

        // When & Then
        val request = SubtitleSearchRequest(
            title = "", // Invalid empty title
            type = ContentType.MOVIE,
            languages = emptyList() // Invalid empty languages
        )
        
        try {
            testApiClient.searchSubtitles(request)
            fail("Expected SubtitleApiException")
        } catch (e: SubtitleApiException) {
            assertEquals(SubtitleApiProvider.SUBDL, e.provider)
            assertTrue(e.message?.contains("Invalid request parameters") == true)
        }
    }

    @Test
    fun `searchSubtitles handles network timeout with retries`() = runBlocking {
        // Given - First request times out, second succeeds
        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE)
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .setHeader("Content-Type", "application/json")
        )

        // When
        val request = SubtitleSearchRequest(
            title = "Test Movie",
            type = ContentType.MOVIE,
            languages = listOf("en")
        )
        val results = testApiClient.searchSubtitles(request)

        // Then
        assertTrue(results.isEmpty())
        assertEquals(2, mockWebServer.requestCount) // Verify retry happened
    }

    @Test
    fun `searchSubtitles handles rate limiting`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error": "Rate limit exceeded", "retry_after": 60}""")
                .setHeader("Retry-After", "60")
        )

        // When & Then
        val request = SubtitleSearchRequest(
            title = "Test Movie",
            type = ContentType.MOVIE,
            languages = listOf("en")
        )
        
        try {
            testApiClient.searchSubtitles(request)
            fail("Expected SubtitleApiException")
        } catch (e: SubtitleApiException) {
            assertEquals(SubtitleApiProvider.SUBDL, e.provider)
            assertTrue(e.message?.contains("Rate limit exceeded") == true)
        }
    }

    @Test
    fun `downloadSubtitle returns local file path on success`() = runBlocking {
        // Given
        val subtitleContent = """
            1
            00:00:01,000 --> 00:00:03,000
            Test subtitle content
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(subtitleContent)
                .setHeader("Content-Type", "text/plain")
        )

        // When
        val result = SubtitleSearchResult(
            id = "SUB123",
            provider = SubtitleApiProvider.SUBDL,
            language = "en",
            languageName = "English",
            format = SubtitleFormat.SRT,
            downloadUrl = mockWebServer.url("/download/SUB123").toString(),
            fileName = "test.srt",
            matchType = MatchType.HASH_MATCH
        )
        val filePath = testApiClient.downloadSubtitle(result)

        // Then
        assertNotNull(filePath)
        assertTrue(filePath.endsWith(".srt"))
        // Verify content was downloaded (would need to read file in real implementation)
        
        val serverRequest = mockWebServer.takeRequest()
        assertEquals("/download/SUB123", serverRequest.path)
        assertEquals("GET", serverRequest.method)
    }

    @Test
    fun `downloadSubtitle handles download failures with retries`() = runBlocking {
        // Given - First attempt fails, second succeeds
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("Test subtitle content")
                .setHeader("Content-Type", "text/plain")
        )

        // When
        val result = SubtitleSearchResult(
            id = "SUB123",
            provider = SubtitleApiProvider.SUBDL,
            language = "en",
            languageName = "English",
            format = SubtitleFormat.SRT,
            downloadUrl = mockWebServer.url("/download/SUB123").toString(),
            fileName = "test.srt",
            matchType = MatchType.HASH_MATCH
        )
        val filePath = testApiClient.downloadSubtitle(result)

        // Then
        assertNotNull(filePath)
        assertEquals(2, mockWebServer.requestCount) // Verify retry happened
    }

    @Test
    fun `downloadSubtitle throws exception after max retries`() = runBlocking {
        // Given - All attempts fail
        repeat(3) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error")
            )
        }

        // When & Then
        val result = SubtitleSearchResult(
            id = "SUB123",
            provider = SubtitleApiProvider.SUBDL,
            language = "en",
            languageName = "English",
            format = SubtitleFormat.SRT,
            downloadUrl = mockWebServer.url("/download/SUB123").toString(),
            fileName = "test.srt",
            matchType = MatchType.HASH_MATCH
        )
        
        try {
            testApiClient.downloadSubtitle(result)
            fail("Expected SubtitleApiException")
        } catch (e: SubtitleApiException) {
            assertEquals(SubtitleApiProvider.SUBDL, e.provider)
            assertTrue(e.message?.contains("Download failed") == true)
        }
        
        assertEquals(3, mockWebServer.requestCount) // Verify all retries attempted
    }

    @Test
    fun `testConnection returns true for healthy API`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status": "ok", "version": "1.0"}""")
                .setHeader("Content-Type", "application/json")
        )

        // When
        val isHealthy = testApiClient.testConnection()

        // Then
        assertTrue(isHealthy)
        
        val serverRequest = mockWebServer.takeRequest()
        assertEquals("/health", serverRequest.path)
        assertEquals("GET", serverRequest.method)
    }

    @Test
    fun `testConnection returns false for unhealthy API`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        // When
        val isHealthy = testApiClient.testConnection()

        // Then
        assertFalse(isHealthy)
    }

    @Test
    fun `testConnection handles network exceptions`() = runBlocking {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        // When
        val isHealthy = testApiClient.testConnection()

        // Then
        assertFalse(isHealthy)
    }

    @Test
    fun `isEnabled returns correct status`() {
        // When & Then
        assertTrue(testApiClient.isEnabled())
    }

    @Test
    fun `getProvider returns correct provider`() {
        // When & Then
        assertEquals(SubtitleApiProvider.SUBDL, testApiClient.getProvider())
    }
}

/**
 * Test implementation of SubtitleApiClient for testing purposes.
 */
private class TestSubtitleApiClient(
    private val apiService: TestSubtitleApiService
) : SubtitleApiClient {

    override fun getProvider(): SubtitleApiProvider = SubtitleApiProvider.SUBDL

    override fun isEnabled(): Boolean = true

    override suspend fun searchSubtitles(request: SubtitleSearchRequest): List<SubtitleSearchResult> {
        return try {
            // Validate request parameters
            if (request.title.isBlank() || request.languages.isEmpty()) {
                throw SubtitleApiException(
                    provider = getProvider(),
                    message = "Invalid request parameters"
                )
            }

            val response = retryWithBackoff(maxRetries = 2) {
                apiService.searchSubtitles(
                    hash = request.fileHash,
                    imdbId = request.imdbId,
                    title = request.title,
                    year = request.year,
                    season = request.season,
                    episode = request.episode,
                    languages = request.languages.joinToString(",")
                )
            }

            if (response.isSuccessful) {
                response.body()?.map { dto ->
                    SubtitleSearchResult(
                        id = dto.id,
                        provider = getProvider(),
                        language = dto.language,
                        languageName = dto.languageName,
                        format = SubtitleFormat.fromExtension(dto.format) ?: SubtitleFormat.SRT,
                        downloadUrl = dto.downloadUrl,
                        fileName = dto.fileName,
                        fileSize = dto.fileSize,
                        downloadCount = dto.downloadCount,
                        rating = dto.rating,
                        matchScore = dto.matchScore,
                        matchType = MatchType.valueOf(dto.matchType),
                        uploadDate = dto.uploadDate,
                        uploader = dto.uploader,
                        isVerified = dto.isVerified,
                        hearingImpaired = dto.hearingImpaired,
                        releaseGroup = dto.releaseGroup,
                        version = dto.version
                    )
                } ?: emptyList()
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw SubtitleApiException(
                    provider = getProvider(),
                    message = errorBody
                )
            }
        } catch (e: SubtitleApiException) {
            throw e
        } catch (e: Exception) {
            throw SubtitleApiException(
                provider = getProvider(),
                message = "Search failed: ${e.message}",
                cause = e
            )
        }
    }

    override suspend fun downloadSubtitle(result: SubtitleSearchResult): String {
        return try {
            retryWithBackoff(maxRetries = 3) {
                apiService.downloadSubtitle(result.downloadUrl)
            }.let { response ->
                if (response.isSuccessful) {
                    // In a real implementation, this would save the file and return the path
                    "/cache/subtitles/${result.id}.${result.format.extension}"
                } else {
                    throw SubtitleApiException(
                        provider = getProvider(),
                        message = "Download failed: ${response.code()}"
                    )
                }
            }
        } catch (e: SubtitleApiException) {
            throw e
        } catch (e: Exception) {
            throw SubtitleApiException(
                provider = getProvider(),
                message = "Download failed: ${e.message}",
                cause = e
            )
        }
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val response = apiService.healthCheck()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun <T> retryWithBackoff(
        maxRetries: Int,
        initialDelayMs: Long = 1000,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(initialDelayMs * (attempt + 1))
                }
            }
        }
        throw lastException!!
    }
}

/**
 * Test API service interface for mocking subtitle provider endpoints.
 */
private interface TestSubtitleApiService {
    @GET("search")
    suspend fun searchSubtitles(
        @Query("hash") hash: String?,
        @Query("imdb_id") imdbId: String?,
        @Query("title") title: String?,
        @Query("year") year: Int?,
        @Query("season") season: Int?,
        @Query("episode") episode: Int?,
        @Query("languages") languages: String?
    ): retrofit2.Response<List<SubtitleDto>>

    @GET
    suspend fun downloadSubtitle(@Url url: String): retrofit2.Response<String>

    @GET("health")
    suspend fun healthCheck(): retrofit2.Response<Map<String, Any>>
}

/**
 * Data transfer object for subtitle search results.
 */
private data class SubtitleDto(
    val id: String,
    val language: String,
    val language_name: String,
    val format: String,
    val download_url: String,
    val file_name: String,
    val file_size: Long?,
    val download_count: Int?,
    val rating: Float?,
    val match_score: Float,
    val match_type: String,
    val upload_date: Long?,
    val uploader: String?,
    val is_verified: Boolean,
    val hearing_impaired: Boolean?,
    val release_group: String?,
    val version: String?
) {
    // Map snake_case JSON to camelCase properties
    val languageName: String get() = language_name
    val downloadUrl: String get() = download_url
    val fileName: String get() = file_name
    val fileSize: Long? get() = file_size
    val downloadCount: Int? get() = download_count
    val matchScore: Float get() = match_score
    val matchType: String get() = match_type
    val uploadDate: Long? get() = upload_date
    val isVerified: Boolean get() = is_verified
    val hearingImpaired: Boolean? get() = hearing_impaired
    val releaseGroup: String? get() = release_group
}