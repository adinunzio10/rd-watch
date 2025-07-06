# Real Debrid Account File Browser Documentation

This document provides comprehensive documentation for the Real Debrid account file browser feature, including implementation details, API integration, UI components, testing strategies, and architectural decisions.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [API Integration](#api-integration)
- [Data Models](#data-models)
- [UI Components](#ui-components)
- [Android TV Considerations](#android-tv-considerations)
- [Testing Strategy](#testing-strategy)
- [User Interaction Guide](#user-interaction-guide)
- [Performance Optimization](#performance-optimization)
- [Error Handling](#error-handling)
- [Troubleshooting](#troubleshooting)
- [Development Guidelines](#development-guidelines)

## Overview

The Real Debrid Account File Browser is a dedicated interface that allows users to browse their Real Debrid account files directly, bypassing the traditional scraper search workflow. This feature provides direct access to the user's torrents and files with comprehensive management capabilities.

### Key Features

- **Direct Account Access**: Browse torrents and files directly from Real Debrid account
- **Comprehensive File Management**: View, sort, filter, and delete files
- **Pagination Support**: Handle large file collections efficiently
- **Offline Caching**: Cache file lists with TTL for improved performance
- **Bulk Operations**: Select and manage multiple files simultaneously
- **Android TV Optimized**: D-pad navigation and 10-foot UI experience

### Dependencies

This feature depends on:
- **Task #4**: Configure Network Layer (OAuth2 authentication)
- **Task #7**: Build Home Screen with Navigation (Navigation infrastructure)

## Architecture

### Component Overview

```
Real Debrid File Browser
├── Presentation Layer
│   ├── BrowseAccountScreen.kt          # Main UI screen
│   ├── BrowseAccountViewModel.kt       # State management
│   ├── components/
│   │   ├── FileListItem.kt            # Individual file display
│   │   ├── FileSortingPanel.kt        # Sort controls
│   │   ├── FileFilterPanel.kt         # Filter controls
│   │   ├── BulkSelectionBar.kt        # Bulk operations
│   │   └── StorageUsageIndicator.kt   # Storage display
│   └── navigation/
│       └── AccountBrowserNavigation.kt # Navigation setup
├── Domain Layer
│   ├── models/
│   │   ├── AccountFile.kt             # File data model
│   │   ├── FileFilter.kt              # Filter criteria
│   │   ├── FileSorting.kt             # Sorting options
│   │   └── StorageInfo.kt             # Storage usage
│   ├── repository/
│   │   └── AccountFileRepository.kt   # Data access abstraction
│   └── usecases/
│       ├── GetAccountFilesUseCase.kt  # File retrieval
│       ├── DeleteFilesUseCase.kt      # File deletion
│       └── GetStorageInfoUseCase.kt   # Storage information
├── Data Layer
│   ├── api/
│   │   └── RealDebridAccountApi.kt    # API service
│   ├── cache/
│   │   ├── AccountFileCache.kt        # Local caching
│   │   └── FileCacheEntity.kt         # Cache entity
│   ├── mappers/
│   │   └── AccountFileMapper.kt       # Data mapping
│   └── paging/
│       └── AccountFilesPagingSource.kt # Pagination
```

### Data Flow

1. **User Action**: User navigates to account browser
2. **ViewModel**: Triggers file loading via use case
3. **Repository**: Coordinates between cache and API
4. **API Service**: Fetches data from Real Debrid API
5. **Cache**: Stores data locally with TTL
6. **Mapping**: Converts API response to domain models
7. **Pagination**: Handles large data sets
8. **UI Update**: Renders files in Compose UI

## API Integration

### Real Debrid API Endpoints

#### Torrents Endpoint

```kotlin
@GET("torrents")
suspend fun getTorrents(
    @Query("offset") offset: Int = 0,
    @Query("limit") limit: Int = 50,
    @Query("filter") filter: String? = null
): ApiResponse<TorrentsResponse>
```

**Parameters:**
- `offset`: Starting index for pagination (default: 0)
- `limit`: Maximum number of items to return (default: 50, max: 100)
- `filter`: Optional filter criteria ("active", "completed", "error")

**Response Model:**
```kotlin
data class TorrentsResponse(
    val torrents: List<TorrentInfo>,
    val total: Int,
    val offset: Int,
    val limit: Int
)

data class TorrentInfo(
    val id: String,
    val hash: String,
    val filename: String,
    val bytes: Long,
    val added: String, // ISO 8601 date
    val status: String, // "magnet_error", "magnet_conversion", "waiting_files_selection", "queued", "downloading", "downloaded", "error", "virus", "compressing", "uploading", "dead"
    val progress: Int, // 0-100
    val files: List<FileInfo>
)

data class FileInfo(
    val id: String,
    val path: String,
    val bytes: Long,
    val selected: Boolean
)
```

#### User Information Endpoint

```kotlin
@GET("user")
suspend fun getUserInfo(): ApiResponse<UserInfo>
```

**Response Model:**
```kotlin
data class UserInfo(
    val id: String,
    val username: String,
    val email: String,
    val points: Int,
    val locale: String,
    val avatar: String,
    val type: String, // "free", "premium"
    val premium: Long, // Premium expiration timestamp
    val expiration: String, // ISO 8601 date
    val storageUsed: Long, // Bytes used
    val storageMax: Long // Maximum storage in bytes
)
```

#### Delete Torrent Endpoint

```kotlin
@DELETE("torrents/delete/{id}")
suspend fun deleteTorrent(
    @Path("id") torrentId: String
): ApiResponse<Unit>
```

### API Service Implementation

```kotlin
@Singleton
class RealDebridAccountApi @Inject constructor(
    private val apiService: RealDebridApiService,
    private val errorHandler: ErrorHandler
) {
    
    suspend fun getTorrents(
        offset: Int = 0,
        limit: Int = 50,
        filter: String? = null
    ): Result<TorrentsResponse> {
        return try {
            val response = apiService.getTorrents(offset, limit, filter)
            when (response) {
                is ApiResponse.Success -> Result.Success(response.data)
                is ApiResponse.Error -> Result.Error(response.exception)
            }
        } catch (e: Exception) {
            Result.Error(errorHandler.handleException(e))
        }
    }
    
    suspend fun getUserInfo(): Result<UserInfo> {
        return try {
            val response = apiService.getUserInfo()
            when (response) {
                is ApiResponse.Success -> Result.Success(response.data)
                is ApiResponse.Error -> Result.Error(response.exception)
            }
        } catch (e: Exception) {
            Result.Error(errorHandler.handleException(e))
        }
    }
    
    suspend fun deleteTorrent(torrentId: String): Result<Unit> {
        return try {
            val response = apiService.deleteTorrent(torrentId)
            when (response) {
                is ApiResponse.Success -> Result.Success(Unit)
                is ApiResponse.Error -> Result.Error(response.exception)
            }
        } catch (e: Exception) {
            Result.Error(errorHandler.handleException(e))
        }
    }
}
```

## Data Models

### Domain Models

#### AccountFile
```kotlin
data class AccountFile(
    val id: String,
    val name: String,
    val size: Long,
    val addedDate: LocalDateTime,
    val status: FileStatus,
    val progress: Int,
    val hash: String,
    val files: List<FileItem>,
    val isSelected: Boolean = false
) {
    val sizeFormatted: String
        get() = formatFileSize(size)
    
    val addedDateFormatted: String
        get() = formatDate(addedDate)
    
    val isDownloaded: Boolean
        get() = status == FileStatus.DOWNLOADED
    
    val isInProgress: Boolean
        get() = status in listOf(FileStatus.DOWNLOADING, FileStatus.QUEUED)
    
    val hasError: Boolean
        get() = status in listOf(FileStatus.ERROR, FileStatus.VIRUS, FileStatus.DEAD)
}

enum class FileStatus {
    MAGNET_ERROR,
    MAGNET_CONVERSION,
    WAITING_FILES_SELECTION,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    ERROR,
    VIRUS,
    COMPRESSING,
    UPLOADING,
    DEAD
}

data class FileItem(
    val id: String,
    val path: String,
    val size: Long,
    val selected: Boolean,
    val type: FileType
) {
    val name: String
        get() = path.substringAfterLast("/")
    
    val extension: String
        get() = name.substringAfterLast(".", "")
}

enum class FileType {
    VIDEO,
    AUDIO,
    ARCHIVE,
    DOCUMENT,
    IMAGE,
    OTHER;
    
    companion object {
        fun fromExtension(extension: String): FileType {
            return when (extension.lowercase()) {
                in videoExtensions -> VIDEO
                in audioExtensions -> AUDIO
                in archiveExtensions -> ARCHIVE
                in documentExtensions -> DOCUMENT
                in imageExtensions -> IMAGE
                else -> OTHER
            }
        }
        
        private val videoExtensions = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp"
        )
        
        private val audioExtensions = setOf(
            "mp3", "flac", "wav", "aac", "ogg", "wma", "m4a", "opus"
        )
        
        private val archiveExtensions = setOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz"
        )
        
        private val documentExtensions = setOf(
            "pdf", "doc", "docx", "txt", "rtf", "odt"
        )
        
        private val imageExtensions = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp"
        )
    }
}
```

#### Filtering and Sorting
```kotlin
data class FileFilter(
    val fileTypes: Set<FileType> = emptySet(),
    val sizeRange: SizeRange? = null,
    val dateRange: DateRange? = null,
    val status: Set<FileStatus> = emptySet(),
    val searchQuery: String = ""
) {
    fun matches(file: AccountFile): Boolean {
        if (fileTypes.isNotEmpty() && !fileTypes.contains(file.primaryFileType)) {
            return false
        }
        
        if (sizeRange != null && !sizeRange.contains(file.size)) {
            return false
        }
        
        if (dateRange != null && !dateRange.contains(file.addedDate)) {
            return false
        }
        
        if (status.isNotEmpty() && !status.contains(file.status)) {
            return false
        }
        
        if (searchQuery.isNotEmpty() && !file.name.contains(searchQuery, ignoreCase = true)) {
            return false
        }
        
        return true
    }
}

data class SizeRange(
    val minSize: Long,
    val maxSize: Long
) {
    fun contains(size: Long): Boolean = size in minSize..maxSize
}

data class DateRange(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
) {
    fun contains(date: LocalDateTime): Boolean = date in startDate..endDate
}

enum class FileSorting {
    DATE_NEWEST,
    DATE_OLDEST,
    NAME_A_Z,
    NAME_Z_A,
    SIZE_LARGEST,
    SIZE_SMALLEST,
    STATUS;
    
    fun comparator(): Comparator<AccountFile> {
        return when (this) {
            DATE_NEWEST -> compareByDescending { it.addedDate }
            DATE_OLDEST -> compareBy { it.addedDate }
            NAME_A_Z -> compareBy { it.name.lowercase() }
            NAME_Z_A -> compareByDescending { it.name.lowercase() }
            SIZE_LARGEST -> compareByDescending { it.size }
            SIZE_SMALLEST -> compareBy { it.size }
            STATUS -> compareBy { it.status }
        }
    }
}
```

#### Storage Information
```kotlin
data class StorageInfo(
    val usedBytes: Long,
    val totalBytes: Long,
    val freeBytes: Long = totalBytes - usedBytes
) {
    val usagePercentage: Float
        get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes.toFloat() else 0f
    
    val usedFormatted: String
        get() = formatFileSize(usedBytes)
    
    val totalFormatted: String
        get() = formatFileSize(totalBytes)
    
    val freeFormatted: String
        get() = formatFileSize(freeBytes)
}
```

### Cache Models

#### Database Entity
```kotlin
@Entity(tableName = "account_files_cache")
data class AccountFileCacheEntity(
    @PrimaryKey val id: String,
    val name: String,
    val size: Long,
    val addedDate: Long, // Unix timestamp
    val status: String,
    val progress: Int,
    val hash: String,
    val filesJson: String, // JSON serialized files
    val cacheTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface AccountFileCacheDao {
    @Query("SELECT * FROM account_files_cache WHERE cacheTimestamp > :validAfter ORDER BY addedDate DESC")
    suspend fun getValidCachedFiles(validAfter: Long): List<AccountFileCacheEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<AccountFileCacheEntity>)
    
    @Query("DELETE FROM account_files_cache WHERE cacheTimestamp < :expiredBefore")
    suspend fun clearExpiredFiles(expiredBefore: Long)
    
    @Query("DELETE FROM account_files_cache WHERE id = :fileId")
    suspend fun deleteFile(fileId: String)
    
    @Query("DELETE FROM account_files_cache")
    suspend fun clearAllFiles()
}
```

## UI Components

### Main Screen Component

```kotlin
@Composable
fun BrowseAccountScreen(
    viewModel: BrowseAccountViewModel = hiltViewModel(),
    onNavigateToPlayer: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadFiles()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with title and storage info
        AccountBrowserHeader(
            storageInfo = uiState.storageInfo,
            onNavigateBack = onNavigateBack
        )
        
        // Controls row
        ControlsRow(
            sortingOption = uiState.sortingOption,
            filterOptions = uiState.filterOptions,
            isSelectionMode = uiState.isSelectionMode,
            selectedCount = uiState.selectedFiles.size,
            onSortingChange = viewModel::updateSorting,
            onFilterChange = viewModel::updateFilter,
            onToggleSelectionMode = viewModel::toggleSelectionMode,
            onDeleteSelected = viewModel::deleteSelectedFiles,
            onRefresh = viewModel::refresh
        )
        
        // Files list
        when (uiState.loadingState) {
            LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            LoadingState.Error -> {
                ErrorState(
                    message = uiState.errorMessage ?: "Failed to load files",
                    onRetry = viewModel::retry
                )
            }
            
            LoadingState.Success -> {
                FilesList(
                    files = uiState.files,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedFiles = uiState.selectedFiles,
                    onFileClick = { file ->
                        if (uiState.isSelectionMode) {
                            viewModel.toggleFileSelection(file)
                        } else {
                            onNavigateToPlayer(file.id)
                        }
                    },
                    onFileLongClick = viewModel::toggleFileSelection,
                    onLoadMore = viewModel::loadMoreFiles,
                    hasMoreFiles = uiState.hasMoreFiles,
                    isLoadingMore = uiState.isLoadingMore
                )
            }
        }
    }
}
```

### File List Component

```kotlin
@Composable
fun FilesList(
    files: List<AccountFile>,
    isSelectionMode: Boolean,
    selectedFiles: Set<String>,
    onFileClick: (AccountFile) -> Unit,
    onFileLongClick: (AccountFile) -> Unit,
    onLoadMore: () -> Unit,
    hasMoreFiles: Boolean,
    isLoadingMore: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            items = files,
            key = { it.id }
        ) { file ->
            FileListItem(
                file = file,
                isSelectionMode = isSelectionMode,
                isSelected = selectedFiles.contains(file.id),
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) }
            )
        }
        
        if (hasMoreFiles) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator()
                    } else {
                        LaunchedEffect(Unit) {
                            onLoadMore()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    file: AccountFile,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.DirectionCenter -> {
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            onClick()
                            true
                        } else false
                    }
                    Key.Menu -> {
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            onLongClick()
                            true
                        } else false
                    }
                    else -> false
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isFocused -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isFocused) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            
            // File type icon
            Icon(
                imageVector = getFileTypeIcon(file.primaryFileType),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 12.dp),
                tint = getFileTypeColor(file.primaryFileType)
            )
            
            // File info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = file.sizeFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = file.addedDateFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                if (file.status != FileStatus.DOWNLOADED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicator(
                            status = file.status,
                            progress = file.progress
                        )
                        
                        if (file.progress > 0) {
                            Text(
                                text = "${file.progress}%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

### Controls Components

```kotlin
@Composable
fun ControlsRow(
    sortingOption: FileSorting,
    filterOptions: FileFilter,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onSortingChange: (FileSorting) -> Unit,
    onFilterChange: (FileFilter) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onDeleteSelected: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort button
        SortButton(
            currentSorting = sortingOption,
            onSortingChange = onSortingChange,
            enabled = !isSelectionMode
        )
        
        // Filter button
        FilterButton(
            currentFilter = filterOptions,
            onFilterChange = onFilterChange,
            enabled = !isSelectionMode
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Selection mode controls
        if (isSelectionMode) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (selectedCount > 0) {
                IconButton(
                    onClick = onDeleteSelected
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete selected files"
                    )
                }
            }
        }
        
        // Selection mode toggle
        IconButton(
            onClick = onToggleSelectionMode
        ) {
            Icon(
                imageVector = if (isSelectionMode) {
                    Icons.Default.Close
                } else {
                    Icons.Default.CheckCircle
                },
                contentDescription = if (isSelectionMode) {
                    "Exit selection mode"
                } else {
                    "Enter selection mode"
                }
            )
        }
        
        // Refresh button
        IconButton(
            onClick = onRefresh,
            enabled = !isSelectionMode
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh files"
            )
        }
    }
}
```

## Android TV Considerations

### Focus Management

The file browser implements comprehensive focus management for D-pad navigation:

```kotlin
@Composable
fun TVFocusManagement(
    files: List<AccountFile>,
    lazyListState: LazyListState
) {
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(files) {
        if (files.isNotEmpty()) {
            // Auto-focus first item when files load
            focusManager.moveFocus(FocusDirection.Down)
        }
    }
    
    // Handle focus traversal at list edges
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex == 0) {
                    // At top of list - focus header controls
                    focusManager.moveFocus(FocusDirection.Up)
                }
            }
    }
}
```

### Key Event Handling

```kotlin
@Composable
fun FileListItem(
    file: AccountFile,
    // ... other parameters
) {
    Card(
        modifier = Modifier
            .onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.DirectionCenter -> {
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            onClick()
                            true
                        } else false
                    }
                    Key.Menu -> {
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            onLongClick()
                            true
                        } else false
                    }
                    Key.Back -> {
                        if (isSelectionMode) {
                            onExitSelectionMode()
                            true
                        } else false
                    }
                    else -> false
                }
            }
    ) {
        // Content
    }
}
```

### TV-Specific Styling

```kotlin
object TVFileListTheme {
    val itemMinHeight = 80.dp
    val itemPadding = 16.dp
    val iconSize = 32.dp
    val titleTextSize = 18.sp
    val bodyTextSize = 14.sp
    val focusBorderWidth = 3.dp
    val cornerRadius = 8.dp
    
    @Composable
    fun focusedCardColors() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    )
    
    @Composable
    fun selectedCardColors() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    )
}
```

### Overscan Handling

```kotlin
@Composable
fun BrowseAccountScreen(
    // ... parameters
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 48.dp, // TV overscan safe area
                end = 48.dp,
                top = 24.dp,
                bottom = 24.dp
            )
    ) {
        // Content
    }
}
```

## Testing Strategy

### Unit Tests

#### ViewModel Tests
```kotlin
@HiltAndroidTest
class BrowseAccountViewModelTest : HiltTestBase() {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Inject
    lateinit var repository: AccountFileRepository
    
    private lateinit var viewModel: BrowseAccountViewModel
    
    @Before
    fun setUp() {
        viewModel = BrowseAccountViewModel(repository)
    }
    
    @Test
    fun `when loadFiles called, should fetch files from repository`() = runTest {
        // Given
        val mockFiles = listOf(
            createMockAccountFile("1", "Test Movie.mp4"),
            createMockAccountFile("2", "Test Audio.mp3")
        )
        coEvery { repository.getFiles(any(), any()) } returns Result.Success(mockFiles)
        
        // When
        viewModel.loadFiles()
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals(LoadingState.Success, uiState.loadingState)
        assertEquals(mockFiles, uiState.files)
    }
    
    @Test
    fun `when sorting changed, should update UI state and reorder files`() = runTest {
        // Given
        val files = listOf(
            createMockAccountFile("1", "B Movie.mp4"),
            createMockAccountFile("2", "A Movie.mp4")
        )
        coEvery { repository.getFiles(any(), any()) } returns Result.Success(files)
        viewModel.loadFiles()
        
        // When
        viewModel.updateSorting(FileSorting.NAME_A_Z)
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals(FileSorting.NAME_A_Z, uiState.sortingOption)
        assertEquals("A Movie.mp4", uiState.files.first().name)
    }
    
    @Test
    fun `when filter applied, should show only matching files`() = runTest {
        // Given
        val files = listOf(
            createMockAccountFile("1", "Movie.mp4", FileType.VIDEO),
            createMockAccountFile("2", "Song.mp3", FileType.AUDIO)
        )
        coEvery { repository.getFiles(any(), any()) } returns Result.Success(files)
        viewModel.loadFiles()
        
        // When
        viewModel.updateFilter(FileFilter(fileTypes = setOf(FileType.VIDEO)))
        
        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.files.size)
        assertEquals("Movie.mp4", uiState.files.first().name)
    }
}
```

#### Repository Tests
```kotlin
@HiltAndroidTest
class AccountFileRepositoryTest : HiltTestBase() {
    
    @Inject
    lateinit var api: RealDebridAccountApi
    
    @Inject
    lateinit var cache: AccountFileCache
    
    @Inject
    lateinit var repository: AccountFileRepository
    
    @Test
    fun `when cache is valid, should return cached data`() = runTest {
        // Given
        val cachedFiles = listOf(createMockAccountFile("1", "Cached Movie.mp4"))
        coEvery { cache.getValidFiles() } returns cachedFiles
        
        // When
        val result = repository.getFiles(offset = 0, limit = 50)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(cachedFiles, result.data)
        coVerify(exactly = 0) { api.getTorrents(any(), any(), any()) }
    }
    
    @Test
    fun `when cache is invalid, should fetch from API and cache result`() = runTest {
        // Given
        val apiFiles = listOf(createMockAccountFile("1", "API Movie.mp4"))
        coEvery { cache.getValidFiles() } returns emptyList()
        coEvery { api.getTorrents(any(), any(), any()) } returns Result.Success(
            TorrentsResponse(
                torrents = listOf(createMockTorrentInfo("1", "API Movie.mp4")),
                total = 1,
                offset = 0,
                limit = 50
            )
        )
        
        // When
        val result = repository.getFiles(offset = 0, limit = 50)
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { api.getTorrents(0, 50, null) }
        coVerify { cache.saveFiles(any()) }
    }
}
```

### Integration Tests

#### Navigation Tests
```kotlin
@HiltAndroidTest
class AccountBrowserNavigationTest : HiltInstrumentedTestBase() {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `should navigate to player when file is clicked`() {
        var navigatedToPlayer = false
        var playerId = ""
        
        composeTestRule.setContent {
            BrowseAccountScreen(
                onNavigateToPlayer = { id ->
                    navigatedToPlayer = true
                    playerId = id
                },
                onNavigateBack = { }
            )
        }
        
        // Wait for files to load
        composeTestRule.waitForIdle()
        
        // Click on first file
        composeTestRule
            .onNodeWithText("Test Movie.mp4")
            .performClick()
        
        assertTrue(navigatedToPlayer)
        assertEquals("1", playerId)
    }
}
```

#### Focus Management Tests
```kotlin
@HiltAndroidTest
class TVFocusManagementTest : HiltInstrumentedTestBase() {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `should handle D-pad navigation correctly`() {
        composeTestRule.setContent {
            BrowseAccountScreen(
                onNavigateToPlayer = { },
                onNavigateBack = { }
            )
        }
        
        // Wait for files to load
        composeTestRule.waitForIdle()
        
        // First item should be focused
        composeTestRule
            .onNodeWithText("Test Movie.mp4")
            .assertIsFocused()
        
        // Navigate down
        composeTestRule
            .onRoot()
            .performKeyInput { keyDown(Key.DirectionDown) }
        
        // Second item should be focused
        composeTestRule
            .onNodeWithText("Test Audio.mp3")
            .assertIsFocused()
    }
}
```

### Performance Tests

```kotlin
@Test
fun `should handle large file lists efficiently`() = runTest {
    // Given
    val largeFileList = (1..1000).map { index ->
        createMockAccountFile("$index", "File $index.mp4")
    }
    
    // When
    val startTime = System.currentTimeMillis()
    viewModel.loadFiles()
    advanceUntilIdle()
    val endTime = System.currentTimeMillis()
    
    // Then
    val loadTime = endTime - startTime
    assertTrue("Load time should be under 1 second", loadTime < 1000)
    
    val uiState = viewModel.uiState.value
    assertEquals(LoadingState.Success, uiState.loadingState)
    assertEquals(50, uiState.files.size) // First page
}
```

## User Interaction Guide

### Navigation Patterns

#### Entering the File Browser
1. From the main home screen, use D-pad to navigate to the "My Files" tab
2. Press center button to enter the account file browser
3. The screen will load with your Real Debrid account files

#### Browsing Files
1. Use D-pad Up/Down to navigate through the file list
2. Press center button to select a file for playback
3. Use D-pad Left/Right to navigate between control buttons
4. Press Menu button on any file to enter selection mode

#### Sorting Files
1. Navigate to the Sort button (top left)
2. Press center button to open sort menu
3. Use D-pad to select sort option:
   - Date (Newest First)
   - Date (Oldest First)
   - Name (A-Z)
   - Name (Z-A)
   - Size (Largest First)
   - Size (Smallest First)
   - Status
4. Press center button to apply sorting

#### Filtering Files
1. Navigate to the Filter button (next to Sort)
2. Press center button to open filter panel
3. Use D-pad to navigate filter options:
   - File Type (Video, Audio, Archive, etc.)
   - Size Range
   - Date Range
   - Status (Downloaded, Downloading, etc.)
4. Press center button to toggle filter options
5. Press Back button to close filter panel

#### Bulk Selection and Deletion
1. Press Menu button on any file to enter selection mode
2. Navigate with D-pad and press center button to select/deselect files
3. Selected files will be highlighted
4. Navigate to Delete button (top right) and press center button
5. Confirm deletion in the dialog
6. Press Back button to exit selection mode

#### Refreshing Files
1. Navigate to the Refresh button (top right)
2. Press center button to refresh the file list
3. The screen will show loading indicator while refreshing

### Error Handling

#### Network Errors
- **Symptom**: "Network error" message displayed
- **Solution**: Check internet connection and try refreshing
- **User Action**: Press center button on "Retry" button

#### Authentication Errors
- **Symptom**: "Authentication failed" message
- **Solution**: Re-authenticate with Real Debrid account
- **User Action**: Navigate to Settings > Account > Re-authenticate

#### Empty File List
- **Symptom**: "No files found" message
- **Solution**: Add torrents to your Real Debrid account
- **User Action**: Use web interface or torrent client to add files

## Performance Optimization

### Lazy Loading Implementation

```kotlin
class AccountFilesPagingSource(
    private val api: RealDebridAccountApi,
    private val filter: FileFilter? = null
) : PagingSource<Int, AccountFile>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AccountFile> {
        return try {
            val page = params.key ?: 0
            val response = api.getTorrents(
                offset = page * params.loadSize,
                limit = params.loadSize,
                filter = filter?.toApiFilter()
            )
            
            when (response) {
                is Result.Success -> {
                    val files = response.data.torrents.map { it.toAccountFile() }
                    LoadResult.Page(
                        data = files,
                        prevKey = if (page == 0) null else page - 1,
                        nextKey = if (files.size < params.loadSize) null else page + 1
                    )
                }
                is Result.Error -> {
                    LoadResult.Error(response.exception)
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, AccountFile>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
```

### Memory Management

```kotlin
class AccountFileCache @Inject constructor(
    private val dao: AccountFileCacheDao,
    private val memoryCache: LruCache<String, AccountFile>
) {
    
    companion object {
        private const val CACHE_TTL_MINUTES = 30
        private const val MAX_MEMORY_CACHE_SIZE = 100
    }
    
    private val memoryLruCache = LruCache<String, AccountFile>(MAX_MEMORY_CACHE_SIZE)
    
    suspend fun getValidFiles(): List<AccountFile> {
        val validAfter = System.currentTimeMillis() - (CACHE_TTL_MINUTES * 60 * 1000)
        return dao.getValidCachedFiles(validAfter).map { it.toAccountFile() }
    }
    
    suspend fun saveFiles(files: List<AccountFile>) {
        // Clear expired entries first
        val expiredBefore = System.currentTimeMillis() - (CACHE_TTL_MINUTES * 60 * 1000)
        dao.clearExpiredFiles(expiredBefore)
        
        // Save new files
        val entities = files.map { it.toCacheEntity() }
        dao.insertFiles(entities)
        
        // Update memory cache
        files.forEach { file ->
            memoryLruCache.put(file.id, file)
        }
    }
}
```

### Image Loading Optimization

```kotlin
@Composable
fun FileListItem(
    file: AccountFile,
    // ... other parameters
) {
    val imageLoader = LocalContext.current.imageLoader
    
    Card(/* ... */) {
        Row(/* ... */) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file.thumbnailUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .placeholder(R.drawable.ic_file_placeholder)
                    .error(R.drawable.ic_file_error)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            
            // ... rest of content
        }
    }
}
```

## Error Handling

### Error Types and Responses

```kotlin
sealed class AccountBrowserError : AppException {
    object NetworkError : AccountBrowserError()
    object AuthenticationError : AccountBrowserError()
    object ServerError : AccountBrowserError()
    object RateLimitError : AccountBrowserError()
    data class UnknownError(val message: String) : AccountBrowserError()
}

class AccountBrowserErrorHandler @Inject constructor(
    private val errorMessageProvider: ErrorMessageProvider
) {
    
    fun handleError(error: Throwable): AccountBrowserError {
        return when (error) {
            is HttpException -> {
                when (error.code()) {
                    401, 403 -> AccountBrowserError.AuthenticationError
                    429 -> AccountBrowserError.RateLimitError
                    in 500..599 -> AccountBrowserError.ServerError
                    else -> AccountBrowserError.UnknownError(error.message())
                }
            }
            is IOException -> AccountBrowserError.NetworkError
            else -> AccountBrowserError.UnknownError(error.message ?: "Unknown error")
        }
    }
    
    fun getErrorMessage(error: AccountBrowserError): String {
        return when (error) {
            AccountBrowserError.NetworkError -> 
                "Network connection error. Please check your internet connection and try again."
            AccountBrowserError.AuthenticationError -> 
                "Authentication failed. Please re-authenticate with your Real Debrid account."
            AccountBrowserError.ServerError -> 
                "Real Debrid server error. Please try again later."
            AccountBrowserError.RateLimitError -> 
                "Rate limit exceeded. Please wait a moment before trying again."
            is AccountBrowserError.UnknownError -> 
                "An unexpected error occurred: ${error.message}"
        }
    }
}
```

### Retry Logic

```kotlin
class RetryableAccountFileRepository @Inject constructor(
    private val api: RealDebridAccountApi,
    private val cache: AccountFileCache,
    private val retryHandler: RetryHandler
) : AccountFileRepository {
    
    override suspend fun getFiles(
        offset: Int,
        limit: Int,
        filter: FileFilter?
    ): Result<List<AccountFile>> {
        return retryHandler.retry(
            maxAttempts = 3,
            initialDelay = 1000L,
            maxDelay = 5000L,
            backoffMultiplier = 2.0
        ) {
            when (val cachedFiles = cache.getValidFiles()) {
                is Result.Success -> {
                    if (cachedFiles.data.isNotEmpty()) {
                        return@retry Result.Success(cachedFiles.data)
                    }
                }
            }
            
            // Fetch from API
            val apiResult = api.getTorrents(offset, limit, filter?.toApiFilter())
            when (apiResult) {
                is Result.Success -> {
                    val files = apiResult.data.torrents.map { it.toAccountFile() }
                    cache.saveFiles(files)
                    Result.Success(files)
                }
                is Result.Error -> {
                    Result.Error(apiResult.exception)
                }
            }
        }
    }
}
```

### Circuit Breaker Pattern

```kotlin
class AccountFileCircuitBreaker @Inject constructor() {
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var state = CircuitBreakerState.CLOSED
    
    private companion object {
        const val FAILURE_THRESHOLD = 5
        const val TIMEOUT_DURATION = 60000L // 1 minute
    }
    
    enum class CircuitBreakerState {
        CLOSED, OPEN, HALF_OPEN
    }
    
    suspend fun <T> execute(operation: suspend () -> T): T {
        when (state) {
            CircuitBreakerState.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > TIMEOUT_DURATION) {
                    state = CircuitBreakerState.HALF_OPEN
                } else {
                    throw CircuitBreakerOpenException()
                }
            }
            CircuitBreakerState.HALF_OPEN -> {
                // Allow one request through
            }
            CircuitBreakerState.CLOSED -> {
                // Normal operation
            }
        }
        
        return try {
            val result = operation()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }
    
    private fun onSuccess() {
        failureCount = 0
        state = CircuitBreakerState.CLOSED
    }
    
    private fun onFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        
        if (failureCount >= FAILURE_THRESHOLD) {
            state = CircuitBreakerState.OPEN
        }
    }
}
```

## Troubleshooting

### Common Issues

#### Files Not Loading
**Symptoms:**
- Empty file list
- Loading spinner never disappears
- Error message "Failed to load files"

**Possible Causes:**
1. Network connectivity issues
2. Real Debrid API authentication failure
3. Real Debrid service outage
4. App cache corruption

**Solutions:**
1. Check internet connection
2. Verify Real Debrid account authentication in Settings
3. Clear app cache: Settings > Apps > RD Watch > Storage > Clear Cache
4. Force refresh: Pull down on file list or tap Refresh button
5. Restart the app

#### Slow Loading Performance
**Symptoms:**
- Files take long time to load
- Scrolling is laggy
- App becomes unresponsive

**Possible Causes:**
1. Large number of files (1000+)
2. Slow internet connection
3. Insufficient device memory
4. Cache corruption

**Solutions:**
1. Enable file filtering to reduce visible files
2. Clear app cache and restart
3. Check available storage space
4. Close other apps to free memory
5. Use wired ethernet connection if available

#### Files Not Playing
**Symptoms:**
- File selected but player doesn't start
- "File not found" error
- Player starts but shows black screen

**Possible Causes:**
1. File is not fully downloaded on Real Debrid
2. File format not supported
3. Network connection issues
4. Real Debrid link expired

**Solutions:**
1. Check file status - ensure it shows "Downloaded"
2. Try a different file to test player functionality
3. Check Real Debrid account via web interface
4. Verify file is still available on Real Debrid

#### Selection Mode Issues
**Symptoms:**
- Cannot select files
- Selection checkboxes not visible
- Delete operation fails

**Possible Causes:**
1. UI focus issues
2. Network error during deletion
3. Permission issues

**Solutions:**
1. Exit and re-enter selection mode
2. Use Menu button to toggle selection
3. Check internet connection before deleting
4. Verify Real Debrid account permissions

### Debug Information

#### Logging
The app logs important events for debugging:

```kotlin
class AccountBrowserLogger @Inject constructor() {
    
    fun logFileLoad(count: Int, duration: Long) {
        Log.d("AccountBrowser", "Loaded $count files in ${duration}ms")
    }
    
    fun logError(error: Throwable) {
        Log.e("AccountBrowser", "Error occurred", error)
    }
    
    fun logUserAction(action: String, details: String = "") {
        Log.i("AccountBrowser", "User action: $action $details")
    }
}
```

#### Performance Monitoring
Key metrics to monitor:
- File loading time
- Memory usage
- Network request frequency
- Cache hit rate

### Developer Tools

#### Debug Menu
Access via Settings > Developer Options:
- Clear file cache
- Force API refresh
- View cache statistics
- Enable detailed logging

#### ADB Commands
```bash
# Clear app cache
adb shell pm clear com.rdwatch.androidtv

# View logs
adb logcat | grep AccountBrowser

# Check network connectivity
adb shell ping google.com
```

## Development Guidelines

### Code Style

Follow the existing project conventions:
- Use Kotlin coroutines for asynchronous operations
- Implement proper error handling with Result sealed class
- Use Jetpack Compose for UI components
- Follow Material Design guidelines for TV
- Implement proper focus management for Android TV

### Testing Requirements

- Unit tests for ViewModels and repositories
- Integration tests for API interactions
- UI tests for focus management
- Performance tests for large data sets
- Error handling tests for network failures

### Documentation Updates

When making changes to the account file browser:
1. Update this documentation file
2. Update API documentation if endpoints change
3. Add new error types to troubleshooting guide
4. Update user interaction guide for new features
5. Document performance considerations

### Version Control

- Use descriptive commit messages
- Create feature branches for new functionality
- Include tests with all changes
- Update documentation as part of feature development

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](../CLAUDE.md), [CLAUDE-architecture.md](../CLAUDE-architecture.md), [claude-tests.md](../claude-tests.md)