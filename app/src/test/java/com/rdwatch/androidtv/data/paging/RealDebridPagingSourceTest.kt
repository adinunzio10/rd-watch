package com.rdwatch.androidtv.data.paging

import androidx.paging.PagingSource
import com.rdwatch.androidtv.data.entities.TorrentEntity
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.network.models.TorrentInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.*

/**
 * Unit tests for RealDebridPagingSource
 * Tests pagination logic, error handling, and data mapping
 */
class RealDebridPagingSourceTest {

    private lateinit var apiService: RealDebridApiService
    private lateinit var pagingSource: RealDebridPagingSource

    @Before
    fun setup() {
        apiService = mockk()
        pagingSource = RealDebridPagingSource(apiService, null)
    }

    @Test
    fun `load first page successfully returns data`() = runTest {
        // Given
        val mockTorrents = createMockTorrentInfoList(20)
        val mockResponse = Response.success(mockTorrents)
        
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } returns mockResponse

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertEquals(20, pageResult.data.size)
        assertNull(pageResult.prevKey)
        assertEquals(1, pageResult.nextKey)
        
        // Verify data mapping
        val firstTorrent = pageResult.data.first()
        assertEquals("torrent_0", firstTorrent.id)
        assertEquals("Movie.0.mkv", firstTorrent.filename)
        assertEquals("hash_0", firstTorrent.hash)
        assertEquals(1000L, firstTorrent.bytes)
    }

    @Test
    fun `load second page successfully returns data with correct keys`() = runTest {
        // Given
        val mockTorrents = createMockTorrentInfoList(20)
        val mockResponse = Response.success(mockTorrents)
        
        coEvery { 
            apiService.getTorrents(offset = 20, limit = 20, filter = null) 
        } returns mockResponse

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertEquals(20, pageResult.data.size)
        assertEquals(0, pageResult.prevKey)
        assertEquals(2, pageResult.nextKey)
    }

    @Test
    fun `load last page returns data with null nextKey`() = runTest {
        // Given - return less data than page size to indicate last page
        val mockTorrents = createMockTorrentInfoList(10)
        val mockResponse = Response.success(mockTorrents)
        
        coEvery { 
            apiService.getTorrents(offset = 40, limit = 20, filter = null) 
        } returns mockResponse

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 2,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertEquals(10, pageResult.data.size)
        assertEquals(1, pageResult.prevKey)
        assertNull(pageResult.nextKey) // Last page should have null nextKey
    }

    @Test
    fun `load empty page returns empty data with null nextKey`() = runTest {
        // Given
        val mockResponse = Response.success(emptyList<TorrentInfo>())
        
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } returns mockResponse

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertTrue(pageResult.data.isEmpty())
        assertNull(pageResult.prevKey)
        assertNull(pageResult.nextKey) // Empty page should have null nextKey
    }

    @Test
    fun `load with filter parameter passes filter to API`() = runTest {
        // Given
        val filteredPagingSource = RealDebridPagingSource(apiService, "video")
        val mockTorrents = createMockTorrentInfoList(5)
        val mockResponse = Response.success(mockTorrents)
        
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = "video") 
        } returns mockResponse

        // When
        val result = filteredPagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertEquals(5, pageResult.data.size)
    }

    @Test
    fun `load handles HTTP error response`() = runTest {
        // Given
        val errorResponse = Response.error<List<TorrentInfo>>(
            401, 
            okhttp3.ResponseBody.create(null, """{"error": "unauthorized"}""")
        )
        
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } returns errorResponse

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertTrue(errorResult.throwable is HttpException)
        assertEquals(401, (errorResult.throwable as HttpException).code())
    }

    @Test
    fun `load handles IOException`() = runTest {
        // Given
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } throws IOException("Network error")

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertTrue(errorResult.throwable is IOException)
        assertEquals("Network error", errorResult.throwable.message)
    }

    @Test
    fun `load handles HttpException`() = runTest {
        // Given
        val httpException = HttpException(
            Response.error<List<TorrentInfo>>(
                500, 
                okhttp3.ResponseBody.create(null, "Internal Server Error")
            )
        )
        
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } throws httpException

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertTrue(errorResult.throwable is HttpException)
        assertEquals(500, (errorResult.throwable as HttpException).code())
    }

    @Test
    fun `load handles generic exception`() = runTest {
        // Given
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } throws RuntimeException("Unexpected error")

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Error)
        val errorResult = result as PagingSource.LoadResult.Error
        assertTrue(errorResult.throwable is RuntimeException)
        assertEquals("Unexpected error", errorResult.throwable.message)
    }

    @Test
    fun `load handles null response body gracefully`() = runTest {
        // Given
        val mockResponse = Response.success<List<TorrentInfo>>(null)
        
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } returns mockResponse

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertTrue(pageResult.data.isEmpty())
        assertNull(pageResult.nextKey)
    }

    @Test
    fun `getRefreshKey returns correct key for middle position`() {
        // Given
        val pages = listOf(
            PagingSource.LoadResult.Page(
                data = createMockTorrentEntityList(20),
                prevKey = null,
                nextKey = 1
            ),
            PagingSource.LoadResult.Page(
                data = createMockTorrentEntityList(20),
                prevKey = 0,
                nextKey = 2
            )
        )
        
        val pagingState = PagingSource.PagingState(
            pages = pages,
            anchorPosition = 25, // Middle of second page
            config = androidx.paging.PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )

        // When
        val refreshKey = pagingSource.getRefreshKey(pagingState)

        // Then
        assertEquals(1, refreshKey) // Should return the key for the page containing position 25
    }

    @Test
    fun `getRefreshKey returns null for null anchor position`() {
        // Given
        val pagingState = PagingSource.PagingState<Int, TorrentEntity>(
            pages = emptyList(),
            anchorPosition = null,
            config = androidx.paging.PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )

        // When
        val refreshKey = pagingSource.getRefreshKey(pagingState)

        // Then
        assertNull(refreshKey)
    }

    @Test
    fun `toTorrentEntity correctly maps TorrentInfo data`() = runTest {
        // Given
        val torrentInfo = TorrentInfo(
            id = "test_id",
            filename = "test_movie.mkv",
            hash = "test_hash",
            bytes = 2147483648L,
            originalFilename = "original.mkv",
            host = "real-debrid.com",
            split = 100,
            progress = 75.5f,
            status = "downloaded",
            added = "2023-01-01T12:00:00.000Z",
            ended = "2023-01-01T12:30:00.000Z",
            speed = 1000L,
            seeders = 50,
            links = listOf("link1", "link2")
        )

        val mockResponse = Response.success(listOf(torrentInfo))
        
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } returns mockResponse

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        val torrentEntity = pageResult.data.first()
        
        assertEquals("test_id", torrentEntity.id)
        assertEquals("test_movie.mkv", torrentEntity.filename)
        assertEquals("test_hash", torrentEntity.hash)
        assertEquals(2147483648L, torrentEntity.bytes)
        assertEquals(100, torrentEntity.split)
        assertEquals(75.5f, torrentEntity.progress)
        assertEquals("downloaded", torrentEntity.status)
        assertEquals(1000L, torrentEntity.speed)
        assertEquals(50, torrentEntity.seeders)
        assertEquals(listOf("link1", "link2"), torrentEntity.links)
        assertNotNull(torrentEntity.added)
        assertNotNull(torrentEntity.ended)
    }

    @Test
    fun `toTorrentEntity handles invalid date formats gracefully`() = runTest {
        // Given
        val torrentInfo = TorrentInfo(
            id = "test_id",
            filename = "test_movie.mkv",
            hash = "test_hash",
            bytes = 1000L,
            originalFilename = null,
            host = "real-debrid.com",
            split = 100,
            progress = 100f,
            status = "downloaded",
            added = "invalid-date-format",
            ended = "another-invalid-date",
            speed = null,
            seeders = null,
            links = emptyList()
        )

        val mockResponse = Response.success(listOf(torrentInfo))
        
        coEvery { 
            apiService.getTorrents(offset = 0, limit = 20, filter = null) 
        } returns mockResponse

        // When
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        // Then
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        val torrentEntity = pageResult.data.first()
        
        // Should use current date as fallback for invalid dates
        assertNotNull(torrentEntity.added)
        assertNull(torrentEntity.ended) // Should be null for invalid ended date
    }

    // Test factory class
    @Test
    fun `RealDebridPagingSourceFactory creates paging source with filter`() {
        // Given
        val factory = RealDebridPagingSourceFactory(apiService)

        // When
        val pagingSource1 = factory.create(null)
        val pagingSource2 = factory.create("video")

        // Then
        assertNotNull(pagingSource1)
        assertNotNull(pagingSource2)
        // We can't directly test the filter since it's private, but we can ensure
        // different instances are created
        assertNotSame(pagingSource1, pagingSource2)
    }

    /**
     * Helper function to create mock TorrentInfo list
     */
    private fun createMockTorrentInfoList(count: Int): List<TorrentInfo> {
        return (0 until count).map { index ->
            TorrentInfo(
                id = "torrent_$index",
                filename = "Movie.$index.mkv",
                hash = "hash_$index",
                bytes = 1000L + index,
                originalFilename = "original_$index.mkv",
                host = "real-debrid.com",
                split = 100,
                progress = 100f,
                status = "downloaded",
                added = "2023-01-0${index % 9 + 1}T12:00:00.000Z",
                ended = "2023-01-0${index % 9 + 1}T12:30:00.000Z",
                speed = 1000L,
                seeders = 10,
                links = listOf("link_${index}_1", "link_${index}_2")
            )
        }
    }

    /**
     * Helper function to create mock TorrentEntity list
     */
    private fun createMockTorrentEntityList(count: Int): List<TorrentEntity> {
        return (0 until count).map { index ->
            TorrentEntity(
                id = "torrent_$index",
                hash = "hash_$index",
                filename = "Movie.$index.mkv",
                bytes = 1000L + index,
                links = listOf("link_${index}_1", "link_${index}_2"),
                split = 100,
                progress = 100f,
                status = "downloaded",
                added = Date(),
                speed = 1000L,
                seeders = 10,
                ended = Date()
            )
        }
    }
}