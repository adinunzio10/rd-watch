package com.rdwatch.androidtv.test.mocks

import com.rdwatch.androidtv.data.entities.TorrentEntity
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.network.models.TorrentInfo
import com.rdwatch.androidtv.network.models.UnrestrictLink
import com.rdwatch.androidtv.network.models.UserInfo
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.test.factories.TestDataFactory
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Utility class for creating mocks and setting up common test scenarios
 */
object MockUtilities {

    /**
     * Creates a mock RealDebridApiService with common responses
     */
    fun createMockApiService(): RealDebridApiService {
        return mockk<RealDebridApiService>()
    }

    /**
     * Sets up successful getTorrents response
     */
    fun RealDebridApiService.setupSuccessfulTorrentsResponse(
        torrents: List<TorrentInfo> = TestDataFactory.createTorrentInfoList(20),
        offset: Int = 0,
        limit: Int = 20,
        filter: String? = null
    ) {
        coEvery { 
            getTorrents(offset = offset, limit = limit, filter = filter) 
        } returns Response.success(torrents)
    }

    /**
     * Sets up error response for getTorrents
     */
    fun RealDebridApiService.setupErrorTorrentsResponse(
        httpCode: Int = 401,
        errorMessage: String = "Unauthorized",
        offset: Int = 0,
        limit: Int = 20,
        filter: String? = null
    ) {
        val errorBody = ResponseBody.create(null, TestDataFactory.createErrorResponseJson())
        coEvery { 
            getTorrents(offset = offset, limit = limit, filter = filter) 
        } returns Response.error(httpCode, errorBody)
    }

    /**
     * Sets up IOException for getTorrents
     */
    fun RealDebridApiService.setupIOExceptionTorrentsResponse(
        message: String = "Network error",
        offset: Int = 0,
        limit: Int = 20,
        filter: String? = null
    ) {
        coEvery { 
            getTorrents(offset = offset, limit = limit, filter = filter) 
        } throws IOException(message)
    }

    /**
     * Sets up HttpException for getTorrents
     */
    fun RealDebridApiService.setupHttpExceptionTorrentsResponse(
        httpCode: Int = 500,
        offset: Int = 0,
        limit: Int = 20,
        filter: String? = null
    ) {
        val errorResponse = Response.error<List<TorrentInfo>>(
            httpCode, 
            ResponseBody.create(null, "Internal Server Error")
        )
        coEvery { 
            getTorrents(offset = offset, limit = limit, filter = filter) 
        } throws HttpException(errorResponse)
    }

    /**
     * Sets up successful getUserInfo response
     */
    fun RealDebridApiService.setupSuccessfulUserInfoResponse(
        userInfo: UserInfo = TestDataFactory.createUserInfo()
    ) {
        coEvery { getUserInfo() } returns Response.success(userInfo)
    }

    /**
     * Sets up successful unrestrictLink response
     */
    fun RealDebridApiService.setupSuccessfulUnrestrictLinkResponse(
        link: String = "https://example.com/video",
        unrestrictLink: UnrestrictLink = TestDataFactory.createUnrestrictLink()
    ) {
        coEvery { unrestrictLink(link) } returns Response.success(unrestrictLink)
    }

    /**
     * Sets up successful deleteTorrent response
     */
    fun RealDebridApiService.setupSuccessfulDeleteTorrentResponse(
        id: String = "test_id"
    ) {
        coEvery { deleteTorrent(id) } returns Response.success(Unit)
    }

    /**
     * Sets up successful deleteDownload response
     */
    fun RealDebridApiService.setupSuccessfulDeleteDownloadResponse(
        id: String = "test_id"
    ) {
        coEvery { deleteDownload(id) } returns Response.success(Unit)
    }

    /**
     * Creates a mock RealDebridContentRepository with common behaviors
     */
    fun createMockRepository(): RealDebridContentRepository {
        return mockk<RealDebridContentRepository>()
    }

    /**
     * Sets up successful getTorrents flow
     */
    fun RealDebridContentRepository.setupSuccessfulTorrentsFlow(
        entities: List<TorrentEntity> = TestDataFactory.createTorrentEntityList(20)
    ) {
        coEvery { getTorrents() } returns flowOf(Result.Success(entities))
    }

    /**
     * Sets up error getTorrents flow
     */
    fun RealDebridContentRepository.setupErrorTorrentsFlow(
        exception: Exception = IOException("Network error")
    ) {
        coEvery { getTorrents() } returns flowOf(Result.Error(exception))
    }

    /**
     * Sets up successful getDownloads flow
     */
    fun RealDebridContentRepository.setupSuccessfulDownloadsFlow(
        entities: List<com.rdwatch.androidtv.data.entities.ContentEntity> = TestDataFactory.createContentEntityList(10)
    ) {
        coEvery { getDownloads() } returns flowOf(Result.Success(entities))
    }

    /**
     * Sets up successful syncContent
     */
    fun RealDebridContentRepository.setupSuccessfulSyncContent() {
        coEvery { syncContent() } returns Result.Success(Unit)
    }

    /**
     * Sets up error syncContent
     */
    fun RealDebridContentRepository.setupErrorSyncContent(
        exception: Exception = IOException("Sync failed")
    ) {
        coEvery { syncContent() } returns Result.Error(exception)
    }

    /**
     * Sets up successful getTorrentInfo
     */
    fun RealDebridContentRepository.setupSuccessfulTorrentInfo(
        id: String = "test_id",
        entity: TorrentEntity? = TestDataFactory.createTorrentEntity(id = id)
    ) {
        coEvery { getTorrentInfo(id) } returns Result.Success(entity)
    }

    /**
     * Sets up successful unrestrictLink
     */
    fun RealDebridContentRepository.setupSuccessfulUnrestrictLink(
        link: String = "https://example.com/link",
        directUrl: String = "https://download.example.com/direct"
    ) {
        coEvery { unrestrictLink(link) } returns Result.Success(directUrl)
    }

    /**
     * Sets up successful deleteTorrent
     */
    fun RealDebridContentRepository.setupSuccessfulDeleteTorrent(
        id: String = "test_id"
    ) {
        coEvery { deleteTorrent(id) } returns Result.Success(Unit)
    }

    /**
     * Sets up successful deleteDownload
     */
    fun RealDebridContentRepository.setupSuccessfulDeleteDownload(
        id: String = "test_id"
    ) {
        coEvery { deleteDownload(id) } returns Result.Success(Unit)
    }

    /**
     * Sets up successful searchContent flow
     */
    fun RealDebridContentRepository.setupSuccessfulSearchContent(
        query: String = "test",
        entities: List<com.rdwatch.androidtv.data.entities.ContentEntity> = TestDataFactory.createContentEntityList(5)
    ) {
        coEvery { searchContent(query) } returns flowOf(Result.Success(entities))
    }

    /**
     * Common test scenarios for different states
     */
    object Scenarios {

        /**
         * Sets up a complete successful API service with all endpoints working
         */
        fun setupCompletelyWorkingApiService(apiService: RealDebridApiService) {
            apiService.apply {
                setupSuccessfulTorrentsResponse()
                setupSuccessfulUserInfoResponse()
                setupSuccessfulUnrestrictLinkResponse()
                setupSuccessfulDeleteTorrentResponse()
                setupSuccessfulDeleteDownloadResponse()
            }
        }

        /**
         * Sets up an API service with authentication errors
         */
        fun setupAuthenticationErrorApiService(apiService: RealDebridApiService) {
            apiService.apply {
                setupErrorTorrentsResponse(401, "Unauthorized")
                coEvery { getUserInfo() } returns Response.error(
                    401, 
                    ResponseBody.create(null, TestDataFactory.createErrorResponseJson("bad_token", 401))
                )
                coEvery { unrestrictLink(any()) } returns Response.error(
                    401, 
                    ResponseBody.create(null, TestDataFactory.createErrorResponseJson("bad_token", 401))
                )
            }
        }

        /**
         * Sets up an API service with network connectivity issues
         */
        fun setupNetworkErrorApiService(apiService: RealDebridApiService) {
            val networkError = IOException("Unable to resolve host")
            apiService.apply {
                coEvery { getTorrents(any(), any(), any()) } throws networkError
                coEvery { getUserInfo() } throws networkError
                coEvery { unrestrictLink(any()) } throws networkError
                coEvery { deleteTorrent(any()) } throws networkError
                coEvery { deleteDownload(any()) } throws networkError
            }
        }

        /**
         * Sets up an API service with server errors
         */
        fun setupServerErrorApiService(apiService: RealDebridApiService) {
            val serverErrorBody = ResponseBody.create(null, "Internal Server Error")
            apiService.apply {
                coEvery { getTorrents(any(), any(), any()) } returns Response.error(500, serverErrorBody)
                coEvery { getUserInfo() } returns Response.error(500, serverErrorBody)
                coEvery { unrestrictLink(any()) } returns Response.error(500, serverErrorBody)
                coEvery { deleteTorrent(any()) } returns Response.error(500, serverErrorBody)
                coEvery { deleteDownload(any()) } returns Response.error(500, serverErrorBody)
            }
        }

        /**
         * Sets up a repository with mixed success/error scenarios
         */
        fun setupMixedScenarioRepository(repository: RealDebridContentRepository) {
            repository.apply {
                setupSuccessfulTorrentsFlow()
                setupSuccessfulDownloadsFlow()
                setupErrorSyncContent(IOException("Sync failed"))
                setupSuccessfulTorrentInfo()
                setupSuccessfulUnrestrictLink()
                coEvery { deleteTorrent(any()) } returns Result.Error(IOException("Delete failed"))
                setupSuccessfulDeleteDownload()
            }
        }

        /**
         * Sets up pagination scenarios for testing paging
         */
        fun setupPaginationScenarios(apiService: RealDebridApiService) {
            // First page - full page of 20 items
            apiService.setupSuccessfulTorrentsResponse(
                torrents = TestDataFactory.createTorrentInfoList(20, 0),
                offset = 0,
                limit = 20
            )
            
            // Second page - full page of 20 items
            apiService.setupSuccessfulTorrentsResponse(
                torrents = TestDataFactory.createTorrentInfoList(20, 20),
                offset = 20,
                limit = 20
            )
            
            // Third page - partial page of 10 items (last page)
            apiService.setupSuccessfulTorrentsResponse(
                torrents = TestDataFactory.createTorrentInfoList(10, 40),
                offset = 40,
                limit = 20
            )
            
            // Fourth page - empty (beyond last page)
            apiService.setupSuccessfulTorrentsResponse(
                torrents = emptyList(),
                offset = 60,
                limit = 20
            )
        }

        /**
         * Sets up filtering scenarios
         */
        fun setupFilteringScenarios(apiService: RealDebridApiService) {
            // No filter - all torrents
            apiService.setupSuccessfulTorrentsResponse(
                torrents = TestDataFactory.FileTypes.createVideoFiles() + 
                          TestDataFactory.FileTypes.createAudioFiles() + 
                          TestDataFactory.FileTypes.createOtherFiles(),
                filter = null
            )
            
            // Video filter
            apiService.setupSuccessfulTorrentsResponse(
                torrents = TestDataFactory.FileTypes.createVideoFiles(),
                filter = "video"
            )
            
            // Audio filter
            apiService.setupSuccessfulTorrentsResponse(
                torrents = TestDataFactory.FileTypes.createAudioFiles(),
                filter = "audio"
            )
        }
    }

    /**
     * Helper functions for test assertions
     */
    object Assertions {
        
        /**
         * Verifies that a list of torrents matches expected pagination
         */
        fun verifyPaginationData(
            data: List<TorrentEntity>,
            expectedSize: Int,
            startIndex: Int = 0
        ): Boolean {
            if (data.size != expectedSize) return false
            
            data.forEachIndexed { index, torrent ->
                val expectedId = "torrent_${startIndex + index}"
                if (torrent.id != expectedId) return false
            }
            
            return true
        }
        
        /**
         * Verifies that error results contain expected error types
         */
        fun verifyErrorType(result: Result<*>, expectedErrorType: Class<out Exception>): Boolean {
            return when (result) {
                is Result.Error -> expectedErrorType.isInstance(result.exception)
                else -> false
            }
        }
        
        /**
         * Verifies that success results contain expected data
         */
        fun <T> verifySuccessResult(result: Result<T>, validator: (T) -> Boolean): Boolean {
            return when (result) {
                is Result.Success -> validator(result.data)
                else -> false
            }
        }
    }
}