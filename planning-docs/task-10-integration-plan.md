# Direct Account File Browser - Integration Plan

## Overview

This document outlines how the Direct Account File Browser feature will integrate with existing components and features in the RD Watch Android TV application.

## Integration Points

### 1. Navigation Integration

#### 1.1 Screen.kt Updates

```kotlin
// Add to existing Screen.kt
@Serializable
data object AccountFileBrowser : Screen()

@Serializable
data class AccountFileDetails(val fileId: String) : Screen()

@Serializable
data class AccountFilePreview(val fileId: String, val fileUrl: String) : Screen()

// Update Routes object
object Routes {
    // ... existing routes
    const val ACCOUNT_FILE_BROWSER = "account_file_browser"
    const val ACCOUNT_FILE_DETAILS = "account_file_details/{fileId}"
    const val ACCOUNT_FILE_PREVIEW = "account_file_preview/{fileId}/{fileUrl}"
    
    object Args {
        // ... existing args
        const val FILE_ID = "fileId"
        const val FILE_URL = "fileUrl"
    }
}
```

#### 1.2 AppNavigation.kt Updates

```kotlin
// Add to existing AppNavigation.kt
composable<Screen.AccountFileBrowser>(
    enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(300)
        )
    },
    popExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(300)
        )
    }
) {
    AccountFileBrowserScreen(
        navController = navController,
        onNavigateToDetails = { fileId ->
            navController.navigate(Screen.AccountFileDetails(fileId))
        },
        onNavigateToPreview = { fileId, fileUrl ->
            navController.navigate(Screen.AccountFilePreview(fileId, fileUrl))
        },
        onPlayFile = { fileUrl, title ->
            navController.navigate(Screen.VideoPlayer(fileUrl, title))
        }
    )
}

composable<Screen.AccountFileDetails>(
    enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(300)
        )
    },
    popExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(300)
        )
    }
) { backStackEntry ->
    val fileDetails = backStackEntry.toRoute<Screen.AccountFileDetails>()
    AccountFileDetailsScreen(
        fileId = fileDetails.fileId,
        onNavigateBack = { navController.popBackStack() },
        onPlayFile = { fileUrl, title ->
            navController.navigate(Screen.VideoPlayer(fileUrl, title))
        }
    )
}
```

#### 1.3 Home Screen Integration

```kotlin
// Update TVHomeScreen.kt to include Account Files tab
@Composable
fun TVHomeScreen(
    onNavigateToScreen: (Screen) -> Unit,
    onMovieClick: (Movie) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail
        NavigationRail(
            modifier = Modifier.width(200.dp)
        ) {
            NavigationRailItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                selected = state.selectedTab == HomeTab.HOME,
                onClick = { viewModel.selectTab(HomeTab.HOME) }
            )
            
            NavigationRailItem(
                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                label = { Text("Search") },
                selected = state.selectedTab == HomeTab.SEARCH,
                onClick = { onNavigateToScreen(Screen.Search) }
            )
            
            // NEW: Account Files tab
            NavigationRailItem(
                icon = { Icon(Icons.Default.Folder, contentDescription = "My Files") },
                label = { Text("My Files") },
                selected = state.selectedTab == HomeTab.ACCOUNT_FILES,
                onClick = { onNavigateToScreen(Screen.AccountFileBrowser) }
            )
            
            NavigationRailItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = state.selectedTab == HomeTab.SETTINGS,
                onClick = { onNavigateToScreen(Screen.Settings) }
            )
        }
        
        // Content area
        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedTab) {
                HomeTab.HOME -> HomeContent(
                    movies = state.movies,
                    onMovieClick = onMovieClick
                )
                HomeTab.ACCOUNT_FILES -> AccountFileBrowserContent(
                    onNavigateToDetails = { fileId ->
                        onNavigateToScreen(Screen.AccountFileDetails(fileId))
                    }
                )
                // ... other tabs
            }
        }
    }
}
```

### 2. Database Integration

#### 2.1 AppDatabase.kt Updates

```kotlin
// Add to existing AppDatabase.kt
@Database(
    entities = [
        // ... existing entities
        AccountFileEntity::class,
        FileBrowserCacheEntity::class
    ],
    version = 2, // Increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // ... existing DAO methods
    abstract fun accountFileDao(): AccountFileDao
    abstract fun fileBrowserCacheDao(): FileBrowserCacheDao
}
```

#### 2.2 Database Migration

```kotlin
// Add to Migrations.kt
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create account_files table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `account_files` (
                `id` TEXT NOT NULL,
                `torrentId` TEXT NOT NULL,
                `filename` TEXT NOT NULL,
                `size` INTEGER NOT NULL,
                `fileType` TEXT NOT NULL,
                `addedDate` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `downloadUrl` TEXT,
                `cached` INTEGER NOT NULL,
                `lastSync` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        
        // Create indices
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_account_files_torrentId` 
            ON `account_files` (`torrentId`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_account_files_fileType` 
            ON `account_files` (`fileType`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_account_files_addedDate` 
            ON `account_files` (`addedDate`)
        """.trimIndent())
        
        // Create file_browser_cache table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `file_browser_cache` (
                `id` TEXT NOT NULL,
                `cacheKey` TEXT NOT NULL,
                `pageNumber` INTEGER NOT NULL,
                `totalPages` INTEGER NOT NULL,
                `data` TEXT NOT NULL,
                `lastUpdated` INTEGER NOT NULL,
                `ttl` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        
        // Create cache indices
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_file_browser_cache_cacheKey` 
            ON `file_browser_cache` (`cacheKey`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_file_browser_cache_lastUpdated` 
            ON `file_browser_cache` (`lastUpdated`)
        """.trimIndent())
    }
}
```

### 3. API Integration

#### 3.1 RealDebridApiService Updates

```kotlin
// Add to existing RealDebridApiService.kt
interface RealDebridApiService {
    // ... existing methods
    
    // Account Files endpoints
    @GET("account/files")
    suspend fun getAccountFiles(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String? = null,
        @Query("filter") filter: String? = null
    ): Response<AccountFileResponse>
    
    @GET("account/files/{fileId}")
    suspend fun getAccountFileDetails(
        @Path("fileId") fileId: String
    ): Response<AccountFileDetails>
    
    @DELETE("account/files")
    suspend fun deleteAccountFiles(
        @Body request: DeleteFilesRequest
    ): Response<Unit>
    
    @GET("account/storage")
    suspend fun getStorageUsage(): Response<StorageUsage>
    
    @POST("account/files/{fileId}/download")
    suspend fun getDownloadLink(
        @Path("fileId") fileId: String
    ): Response<DownloadLinkResponse>
}
```

#### 3.2 Network Models

```kotlin
// Add new network models
@JsonClass(generateAdapter = true)
data class AccountFileResponse(
    @Json(name = "files") val files: List<AccountFileItem>,
    @Json(name = "pagination") val pagination: PaginationInfo
)

@JsonClass(generateAdapter = true)
data class AccountFileDetails(
    @Json(name = "id") val id: String,
    @Json(name = "torrent_id") val torrentId: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "size") val size: Long,
    @Json(name = "file_type") val fileType: String,
    @Json(name = "added_date") val addedDate: String,
    @Json(name = "status") val status: String,
    @Json(name = "download_url") val downloadUrl: String?,
    @Json(name = "metadata") val metadata: FileMetadata?
)

@JsonClass(generateAdapter = true)
data class StorageUsage(
    @Json(name = "used") val used: Long,
    @Json(name = "total") val total: Long,
    @Json(name = "files_count") val filesCount: Int
)

@JsonClass(generateAdapter = true)
data class DeleteFilesRequest(
    @Json(name = "file_ids") val fileIds: List<String>
)

@JsonClass(generateAdapter = true)
data class DownloadLinkResponse(
    @Json(name = "download_url") val downloadUrl: String,
    @Json(name = "expires_at") val expiresAt: String
)
```

### 4. Authentication Integration

#### 4.1 AuthGuard Integration

```kotlin
// Integrate with existing AuthGuard.kt
@Composable
fun AccountFileBrowserScreen(
    navController: NavController,
    viewModel: AccountFileBrowserViewModel = hiltViewModel()
) {
    AuthGuard(
        onAuthRequired = {
            navController.navigate(Screen.Authentication) {
                popUpTo(Screen.Home) { inclusive = false }
            }
        }
    ) {
        AccountFileBrowserContent(
            navController = navController,
            viewModel = viewModel
        )
    }
}
```

#### 4.2 Token Management

```kotlin
// Ensure API calls use existing token management
@Singleton
class AccountFileRepositoryImpl @Inject constructor(
    private val apiService: RealDebridApiService,
    private val tokenStorage: TokenStorage, // Existing token storage
    private val accountFileDao: AccountFileDao,
    private val mapper: AccountFileMapper
) : AccountFileRepository {
    
    override suspend fun getFiles(
        page: Int,
        limit: Int,
        sortBy: SortOption,
        filter: FilterOption
    ): Flow<PagingData<AccountFileEntity>> {
        // Token is automatically injected via AuthInterceptor
        return Pager(
            config = PagingConfig(pageSize = limit),
            pagingSourceFactory = {
                AccountFilePagingSource(apiService, accountFileDao, mapper, sortBy, filter)
            }
        ).flow
    }
}
```

### 5. Error Handling Integration

#### 5.1 Existing Error Handler Integration

```kotlin
// Use existing error handling patterns
@HiltViewModel
class AccountFileBrowserViewModel @Inject constructor(
    private val getAccountFilesUseCase: GetAccountFilesUseCase,
    private val errorHandler: ErrorHandler, // Existing error handler
    private val errorMessageProvider: ErrorMessageProvider // Existing error provider
) : BaseViewModel<AccountFileBrowserState>() {
    
    override fun handleError(exception: Throwable) {
        val errorMessage = errorMessageProvider.getErrorMessage(exception)
        updateState { copy(error = errorMessage) }
        
        // Log error using existing error handler
        errorHandler.handleError(exception, "AccountFileBrowserViewModel")
    }
}
```

#### 5.2 Retry Handler Integration

```kotlin
// Integrate with existing RetryHandler
class AccountFileRepositoryImpl @Inject constructor(
    private val apiService: RealDebridApiService,
    private val retryHandler: RetryHandler, // Existing retry handler
    // ... other dependencies
) : AccountFileRepository {
    
    override suspend fun refreshFiles(): Result<Unit> {
        return retryHandler.executeWithRetry {
            val response = apiService.getAccountFiles()
            if (response.isSuccessful) {
                // Process response
                Result.success(Unit)
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        }
    }
}
```

### 6. Settings Integration

#### 6.1 Settings Repository Integration

```kotlin
// Add file browser settings to existing SettingsRepository
data class FileBrowserSettings(
    val defaultSortOption: SortOption = SortOption.DATE_DESC,
    val defaultFileTypeFilter: FileType? = null,
    val enablePreviewThumbnails: Boolean = true,
    val cacheRetentionDays: Int = 7,
    val autoRefreshInterval: Long = 3600000L // 1 hour
)

// Add to existing SettingsRepository
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    
    // ... existing methods
    
    suspend fun getFileBrowserSettings(): FileBrowserSettings {
        return dataStore.data.map { preferences ->
            FileBrowserSettings(
                defaultSortOption = SortOption.valueOf(
                    preferences[PreferenceKeys.FILE_BROWSER_SORT] ?: SortOption.DATE_DESC.name
                ),
                defaultFileTypeFilter = preferences[PreferenceKeys.FILE_BROWSER_FILTER]?.let {
                    FileType.valueOf(it)
                },
                enablePreviewThumbnails = preferences[PreferenceKeys.ENABLE_THUMBNAILS] ?: true,
                cacheRetentionDays = preferences[PreferenceKeys.CACHE_RETENTION_DAYS] ?: 7,
                autoRefreshInterval = preferences[PreferenceKeys.AUTO_REFRESH_INTERVAL] ?: 3600000L
            )
        }.first()
    }
    
    suspend fun saveFileBrowserSettings(settings: FileBrowserSettings) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FILE_BROWSER_SORT] = settings.defaultSortOption.name
            settings.defaultFileTypeFilter?.let {
                preferences[PreferenceKeys.FILE_BROWSER_FILTER] = it.name
            }
            preferences[PreferenceKeys.ENABLE_THUMBNAILS] = settings.enablePreviewThumbnails
            preferences[PreferenceKeys.CACHE_RETENTION_DAYS] = settings.cacheRetentionDays
            preferences[PreferenceKeys.AUTO_REFRESH_INTERVAL] = settings.autoRefreshInterval
        }
    }
}
```

#### 6.2 Settings Screen Integration

```kotlin
// Add file browser settings to existing SettingsScreen
@Composable
fun FileBrowserSettingsSection(
    settings: FileBrowserSettings,
    onSettingsChange: (FileBrowserSettings) -> Unit
) {
    LazyColumn {
        item {
            SectionHeader("File Browser Settings")
        }
        
        item {
            SettingsDropdown(
                title = "Default Sort Order",
                selectedValue = settings.defaultSortOption,
                options = SortOption.values().toList(),
                onValueChange = { newSort ->
                    onSettingsChange(settings.copy(defaultSortOption = newSort))
                }
            )
        }
        
        item {
            SettingsSwitch(
                title = "Enable Thumbnails",
                subtitle = "Show preview thumbnails for video files",
                checked = settings.enablePreviewThumbnails,
                onCheckedChange = { enabled ->
                    onSettingsChange(settings.copy(enablePreviewThumbnails = enabled))
                }
            )
        }
        
        item {
            SettingsSlider(
                title = "Cache Retention",
                subtitle = "Keep cached files for ${settings.cacheRetentionDays} days",
                value = settings.cacheRetentionDays.toFloat(),
                valueRange = 1f..30f,
                onValueChange = { days ->
                    onSettingsChange(settings.copy(cacheRetentionDays = days.toInt()))
                }
            )
        }
    }
}
```

### 7. Search Integration

#### 7.1 Global Search Integration

```kotlin
// Integrate with existing SearchViewModel
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val searchAccountFilesUseCase: SearchAccountFilesUseCase, // NEW
    private val searchHistoryManager: SearchHistoryManager
) : BaseViewModel<SearchState>() {
    
    fun search(query: String) {
        updateState { copy(isLoading = true) }
        
        launchSafely {
            val movieResults = searchMoviesUseCase(query)
            val fileResults = searchAccountFilesUseCase(query) // NEW
            
            updateState {
                copy(
                    isLoading = false,
                    movieResults = movieResults,
                    accountFileResults = fileResults, // NEW
                    hasResults = movieResults.isNotEmpty() || fileResults.isNotEmpty()
                )
            }
        }
    }
}
```

#### 7.2 Search Results Integration

```kotlin
// Add account files to search results
@Composable
fun SearchResultsContent(
    searchResults: SearchState,
    onMovieClick: (String) -> Unit,
    onFileClick: (String) -> Unit // NEW
) {
    LazyColumn {
        if (searchResults.movieResults.isNotEmpty()) {
            item {
                SectionHeader("Movies & TV Shows")
            }
            
            items(searchResults.movieResults) { movie ->
                MovieResultCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.id) }
                )
            }
        }
        
        // NEW: Account files section
        if (searchResults.accountFileResults.isNotEmpty()) {
            item {
                SectionHeader("My Files")
            }
            
            items(searchResults.accountFileResults) { file ->
                FileResultCard(
                    file = file,
                    onClick = { onFileClick(file.id) }
                )
            }
        }
    }
}
```

### 8. Player Integration

#### 8.1 ExoPlayer Integration

```kotlin
// Direct integration with existing ExoPlayerManager
class AccountFilePlaybackManager @Inject constructor(
    private val exoPlayerManager: ExoPlayerManager,
    private val accountFileRepository: AccountFileRepository
) {
    
    suspend fun playAccountFile(fileId: String) {
        val downloadLink = accountFileRepository.getDownloadLink(fileId)
        downloadLink.fold(
            onSuccess = { url ->
                exoPlayerManager.playVideo(url)
            },
            onFailure = { error ->
                // Handle error
            }
        )
    }
}
```

#### 8.2 Continue Watching Integration

```kotlin
// Integrate with existing ContinueWatchingManager
class ContinueWatchingManager @Inject constructor(
    private val watchProgressRepository: PlaybackProgressRepository,
    private val accountFileRepository: AccountFileRepository // NEW
) {
    
    suspend fun getContinueWatchingItems(): List<ContinueWatchingItem> {
        val movieProgress = watchProgressRepository.getInProgressMovies()
        val fileProgress = watchProgressRepository.getInProgressAccountFiles() // NEW
        
        return (movieProgress + fileProgress).sortedByDescending { it.lastWatched }
    }
}
```

### 9. Download Manager Integration

#### 9.1 Existing Download Infrastructure

```kotlin
// Integrate with existing download system if available
class AccountFileDownloadManager @Inject constructor(
    private val downloadManager: DownloadManager, // Existing if available
    private val accountFileRepository: AccountFileRepository
) {
    
    suspend fun downloadAccountFile(fileId: String): Result<String> {
        return try {
            val downloadLink = accountFileRepository.getDownloadLink(fileId)
            downloadLink.fold(
                onSuccess = { url ->
                    val downloadId = downloadManager.enqueue(
                        DownloadRequest(url, fileId)
                    )
                    Result.success(downloadId)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Integration Testing Strategy

### 1. Integration Test Categories

#### 1.1 Navigation Integration Tests

```kotlin
@Test
fun navigateToAccountFileBrowser_fromHomeScreen() {
    // Test navigation from home screen to account file browser
    val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    
    composeTestRule.setContent {
        NavHost(navController = navController, startDestination = Screen.Home) {
            composable<Screen.Home> {
                TVHomeScreen(
                    onNavigateToScreen = { screen ->
                        navController.navigate(screen)
                    }
                )
            }
            composable<Screen.AccountFileBrowser> {
                AccountFileBrowserScreen(navController = navController)
            }
        }
    }
    
    // Click on "My Files" tab
    composeTestRule.onNodeWithText("My Files").performClick()
    
    // Verify navigation
    assertEquals(Screen.AccountFileBrowser, navController.currentDestination?.route)
}
```

#### 1.2 Database Integration Tests

```kotlin
@Test
fun accountFileDao_integrationWithExistingDatabase() {
    val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).build()
    
    val accountFileDao = database.accountFileDao()
    val existingUserDao = database.userDao()
    
    // Test that both DAOs work together
    runBlocking {
        // Insert user data
        existingUserDao.insertUser(testUser)
        
        // Insert account file data
        accountFileDao.upsertFiles(listOf(testAccountFile))
        
        // Verify both data types exist
        val user = existingUserDao.getUser()
        val files = accountFileDao.getAllFiles().first()
        
        assertNotNull(user)
        assertEquals(1, files.size)
    }
}
```

#### 1.3 API Integration Tests

```kotlin
@Test
fun realDebridApiService_accountFilesEndpoints() {
    val mockWebServer = MockWebServer()
    val apiService = createTestApiService(mockWebServer)
    
    // Mock successful response
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody(accountFilesJsonResponse)
    )
    
    runBlocking {
        val response = apiService.getAccountFiles()
        
        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertEquals(2, response.body()?.files?.size)
    }
}
```

### 2. End-to-End Integration Tests

#### 2.1 Complete User Flow Tests

```kotlin
@Test
fun completeAccountFileBrowserFlow() {
    // Test complete user flow from authentication to file playback
    composeTestRule.setContent {
        RDWatchApp()
    }
    
    // 1. Authentication
    performAuthentication()
    
    // 2. Navigate to file browser
    composeTestRule.onNodeWithText("My Files").performClick()
    
    // 3. Wait for files to load
    composeTestRule.waitForIdle()
    
    // 4. Select a file
    composeTestRule.onNodeWithText("sample_video.mp4").performClick()
    
    // 5. Play file
    composeTestRule.onNodeWithText("Play").performClick()
    
    // 6. Verify player is launched
    composeTestRule.onNodeWithContentDescription("Video Player").assertIsDisplayed()
}
```

## Migration Strategy

### 1. Phase 1: Core Infrastructure

1. **Database Schema Updates**
   - Add new tables with migration
   - Update existing DAOs
   - Test database migrations

2. **API Extensions**
   - Add new endpoints to existing service
   - Update network models
   - Test API integration

3. **Basic Repository Implementation**
   - Implement core repository functionality
   - Add basic caching
   - Unit tests for repository

### 2. Phase 2: UI Integration

1. **Navigation Updates**
   - Add new screens to navigation
   - Update existing screens with new navigation points
   - Test navigation flows

2. **Basic UI Components**
   - Implement file browser screen
   - Add basic file operations
   - Integrate with existing UI patterns

3. **ViewModel Integration**
   - Implement ViewModels
   - Integrate with existing state management
   - Test ViewModel logic

### 3. Phase 3: Advanced Features

1. **Search Integration**
   - Add file search to global search
   - Implement advanced filtering
   - Test search functionality

2. **Player Integration**
   - Direct file playback
   - Continue watching integration
   - Test playback flows

3. **Settings Integration**
   - Add file browser settings
   - Integrate with existing settings
   - Test settings persistence

### 4. Phase 4: Performance & Polish

1. **Performance Optimization**
   - Implement advanced caching
   - Optimize database queries
   - Test performance under load

2. **Error Handling**
   - Comprehensive error handling
   - Integration with existing error system
   - Test error scenarios

3. **Final Testing**
   - Complete integration testing
   - Performance testing
   - User acceptance testing

## Rollback Strategy

### 1. Feature Flags

```kotlin
// Add feature flag for account file browser
@Singleton
class FeatureFlags @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun isAccountFileBrowserEnabled(): Boolean {
        return settingsRepository.getFeatureFlag("account_file_browser", defaultValue = false)
    }
}
```

### 2. Conditional UI

```kotlin
// Conditionally show account file browser
@Composable
fun TVHomeScreen(
    onNavigateToScreen: (Screen) -> Unit,
    featureFlags: FeatureFlags = hiltViewModel()
) {
    val isAccountFileBrowserEnabled by featureFlags.isAccountFileBrowserEnabled()
        .collectAsState(initial = false)
    
    NavigationRail {
        // ... existing navigation items
        
        if (isAccountFileBrowserEnabled) {
            NavigationRailItem(
                icon = { Icon(Icons.Default.Folder, contentDescription = "My Files") },
                label = { Text("My Files") },
                selected = false,
                onClick = { onNavigateToScreen(Screen.AccountFileBrowser) }
            )
        }
    }
}
```

### 3. Database Rollback

```kotlin
// Implement database rollback capability
val MIGRATION_2_1 = object : Migration(2, 1) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Drop account file tables if rollback needed
        database.execSQL("DROP TABLE IF EXISTS account_files")
        database.execSQL("DROP TABLE IF EXISTS file_browser_cache")
    }
}
```

This comprehensive integration plan ensures that the Direct Account File Browser feature integrates seamlessly with all existing components while maintaining the ability to rollback if needed. The phased approach allows for gradual integration and testing at each stage.