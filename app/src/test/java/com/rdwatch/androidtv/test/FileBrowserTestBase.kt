package com.rdwatch.androidtv.test

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.PagingConfig
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdwatch.androidtv.data.paging.RealDebridPagingSource
import com.rdwatch.androidtv.data.paging.RealDebridPagingSourceFactory
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.test.factories.TestDataFactory
import com.rdwatch.androidtv.test.mocks.MockUtilities
import io.mockk.MockKAnnotations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for file browser related tests
 * Provides common setup, test utilities, and coroutine management
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
abstract class FileBrowserTestBase {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Test coroutine dispatcher
    protected val testDispatcher = StandardTestDispatcher()
    protected val testScope = TestScope(testDispatcher)

    // Mock dependencies
    protected lateinit var mockApiService: RealDebridApiService
    protected lateinit var mockRepository: RealDebridContentRepository
    protected lateinit var pagingSourceFactory: RealDebridPagingSourceFactory

    // Test data
    protected val testTorrents = TestDataFactory.createTorrentInfoList(20)
    protected val testTorrentEntities = TestDataFactory.createTorrentEntityList(20)
    protected val testContentEntities = TestDataFactory.createContentEntityList(10)

    @Before
    open fun setUp() {
        MockKAnnotations.init(this)
        
        // Set main dispatcher to test dispatcher
        Dispatchers.setMain(testDispatcher)
        
        // Initialize mocks
        mockApiService = MockUtilities.createMockApiService()
        mockRepository = MockUtilities.createMockRepository()
        pagingSourceFactory = RealDebridPagingSourceFactory(mockApiService)
        
        // Setup common mock behaviors
        setupCommonMockBehaviors()
    }

    @After
    open fun tearDown() {
        // Reset main dispatcher
        Dispatchers.resetMain()
    }

    /**
     * Sets up common mock behaviors that most tests will need
     */
    protected open fun setupCommonMockBehaviors() {
        MockUtilities.Scenarios.setupCompletelyWorkingApiService(mockApiService)
        mockRepository.apply {
            MockUtilities.setupSuccessfulTorrentsFlow(this, testTorrentEntities)
            MockUtilities.setupSuccessfulDownloadsFlow(this, testContentEntities)
            MockUtilities.setupSuccessfulSyncContent(this)
        }
    }

    /**
     * Creates a PagingSource for testing
     */
    protected fun createTestPagingSource(filter: String? = null): RealDebridPagingSource {
        return pagingSourceFactory.create(filter)
    }

    /**
     * Default paging config for testing
     */
    protected val testPagingConfig = PagingConfig(
        pageSize = 20,
        enablePlaceholders = false,
        initialLoadSize = 20
    )

    /**
     * Advances the test dispatcher and waits for all pending coroutines
     */
    protected fun advanceUntilIdle() {
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /**
     * Runs a test block in the test scope
     */
    protected fun runTestScope(block: suspend TestScope.() -> Unit) {
        testScope.runTest(block)
    }

    /**
     * Common assertions for pagination testing
     */
    protected object PaginationAssertions {
        
        fun assertFirstPage(
            result: androidx.paging.PagingSource.LoadResult<Int, *>,
            expectedSize: Int = 20
        ) {
            assert(result is androidx.paging.PagingSource.LoadResult.Page)
            val page = result as androidx.paging.PagingSource.LoadResult.Page
            assert(page.data.size == expectedSize)
            assert(page.prevKey == null)
            assert(page.nextKey == if (expectedSize == 20) 1 else null)
        }
        
        fun assertMiddlePage(
            result: androidx.paging.PagingSource.LoadResult<Int, *>,
            expectedSize: Int = 20,
            expectedPrevKey: Int,
            expectedNextKey: Int?
        ) {
            assert(result is androidx.paging.PagingSource.LoadResult.Page)
            val page = result as androidx.paging.PagingSource.LoadResult.Page
            assert(page.data.size == expectedSize)
            assert(page.prevKey == expectedPrevKey)
            assert(page.nextKey == expectedNextKey)
        }
        
        fun assertLastPage(
            result: androidx.paging.PagingSource.LoadResult<Int, *>,
            expectedSize: Int,
            expectedPrevKey: Int
        ) {
            assert(result is androidx.paging.PagingSource.LoadResult.Page)
            val page = result as androidx.paging.PagingSource.LoadResult.Page
            assert(page.data.size == expectedSize)
            assert(page.prevKey == expectedPrevKey)
            assert(page.nextKey == null)
        }
        
        fun assertErrorResult(
            result: androidx.paging.PagingSource.LoadResult<Int, *>,
            expectedErrorType: Class<out Throwable>? = null
        ) {
            assert(result is androidx.paging.PagingSource.LoadResult.Error)
            val error = result as androidx.paging.PagingSource.LoadResult.Error
            if (expectedErrorType != null) {
                assert(expectedErrorType.isInstance(error.throwable))
            }
        }
    }

    /**
     * Common test scenarios for file browser functionality
     */
    protected object TestScenarios {
        
        /**
         * Tests basic pagination flow
         */
        suspend fun testBasicPagination(
            pagingSource: RealDebridPagingSource,
            assertions: (List<androidx.paging.PagingSource.LoadResult<Int, *>>) -> Unit
        ) {
            val results = mutableListOf<androidx.paging.PagingSource.LoadResult<Int, *>>()
            
            // Load first page
            results.add(pagingSource.load(
                androidx.paging.PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 20,
                    placeholdersEnabled = false
                )
            ))
            
            // Load second page
            results.add(pagingSource.load(
                androidx.paging.PagingSource.LoadParams.Append(
                    key = 1,
                    loadSize = 20,
                    placeholdersEnabled = false
                )
            ))
            
            // Load third page (potentially empty)
            results.add(pagingSource.load(
                androidx.paging.PagingSource.LoadParams.Append(
                    key = 2,
                    loadSize = 20,
                    placeholdersEnabled = false
                )
            ))
            
            assertions(results)
        }
        
        /**
         * Tests error handling scenarios
         */
        suspend fun testErrorHandling(
            pagingSource: RealDebridPagingSource,
            expectedErrorType: Class<out Exception>
        ) {
            val result = pagingSource.load(
                androidx.paging.PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 20,
                    placeholdersEnabled = false
                )
            )
            
            PaginationAssertions.assertErrorResult(result, expectedErrorType)
        }
        
        /**
         * Tests filtering functionality
         */
        suspend fun testFiltering(
            pagingSourceFactory: RealDebridPagingSourceFactory,
            filters: List<String?>,
            assertions: (Map<String?, androidx.paging.PagingSource.LoadResult<Int, *>>) -> Unit
        ) {
            val results = mutableMapOf<String?, androidx.paging.PagingSource.LoadResult<Int, *>>()
            
            filters.forEach { filter ->
                val pagingSource = pagingSourceFactory.create(filter)
                val result = pagingSource.load(
                    androidx.paging.PagingSource.LoadParams.Refresh(
                        key = null,
                        loadSize = 20,
                        placeholdersEnabled = false
                    )
                )
                results[filter] = result
            }
            
            assertions(results)
        }
    }

    /**
     * Helper functions for test data validation
     */
    protected object DataValidation {
        
        fun validateTorrentData(entities: List<com.rdwatch.androidtv.data.entities.TorrentEntity>) {
            entities.forEach { entity ->
                assert(entity.id.isNotEmpty())
                assert(entity.filename.isNotEmpty())
                assert(entity.hash.isNotEmpty())
                assert(entity.bytes > 0)
                assert(entity.progress >= 0f && entity.progress <= 100f)
                assert(entity.status.isNotEmpty())
            }
        }
        
        fun validateContentData(entities: List<com.rdwatch.androidtv.data.entities.ContentEntity>) {
            entities.forEach { entity ->
                assert(entity.id.isNotEmpty())
                assert(entity.title.isNotEmpty())
                assert(entity.filename.isNotEmpty())
                assert(entity.fileSize > 0)
                assert(entity.downloadUrl.isNotEmpty())
                assert(entity.status.isNotEmpty())
            }
        }
        
        fun validateApiResponse(response: retrofit2.Response<*>) {
            assert(response.isSuccessful)
            assert(response.body() != null)
        }
        
        fun validateErrorResponse(response: retrofit2.Response<*>, expectedCode: Int) {
            assert(!response.isSuccessful)
            assert(response.code() == expectedCode)
        }
    }

    /**
     * Performance test helpers
     */
    protected object PerformanceHelpers {
        
        /**
         * Measures execution time of a suspend function
         */
        suspend fun <T> measureTime(block: suspend () -> T): Pair<T, Long> {
            val startTime = System.currentTimeMillis()
            val result = block()
            val endTime = System.currentTimeMillis()
            return Pair(result, endTime - startTime)
        }
        
        /**
         * Tests memory usage with large datasets
         */
        suspend fun testLargeDatasetPerformance(
            pagingSource: RealDebridPagingSource,
            maxPages: Int = 10
        ): List<Long> {
            val executionTimes = mutableListOf<Long>()
            
            repeat(maxPages) { pageIndex ->
                val (_, time) = measureTime {
                    pagingSource.load(
                        androidx.paging.PagingSource.LoadParams.Refresh(
                            key = pageIndex,
                            loadSize = 100, // Larger page size for performance testing
                            placeholdersEnabled = false
                        )
                    )
                }
                executionTimes.add(time)
            }
            
            return executionTimes
        }
    }
}