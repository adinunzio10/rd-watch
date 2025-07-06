# Direct Account File Browser Architecture Design

## Overview

This document outlines the architecture for implementing a Direct Account File Browser feature in the RD Watch Android TV application. The feature will allow users to browse their Real Debrid account files directly, bypassing the scraper search functionality.

## Architecture Components

### 1. Data Layer

#### 1.1 Database Entities

**AccountFileEntity.kt**
```kotlin
@Entity(
    tableName = "account_files",
    indices = [
        Index(value = ["torrentId"]),
        Index(value = ["fileType"]),
        Index(value = ["addedDate"]),
        Index(value = ["size"])
    ]
)
data class AccountFileEntity(
    @PrimaryKey val id: String,
    val torrentId: String,
    val filename: String,
    val size: Long,
    val fileType: String, // VIDEO, AUDIO, OTHER
    val addedDate: Date,
    val status: String,
    val downloadUrl: String? = null,
    val cached: Boolean = false,
    val lastSync: Date = Date()
)
```

**FileBrowserCacheEntity.kt**
```kotlin
@Entity(
    tableName = "file_browser_cache",
    indices = [Index(value = ["cacheKey"]), Index(value = ["lastUpdated"])]
)
data class FileBrowserCacheEntity(
    @PrimaryKey val id: String,
    val cacheKey: String,
    val pageNumber: Int,
    val totalPages: Int,
    val data: String, // JSON serialized file list
    val lastUpdated: Date,
    val ttl: Long = 3600000 // 1 hour TTL
)
```

#### 1.2 Database Access Objects

**AccountFileDao.kt**
```kotlin
@Dao
interface AccountFileDao {
    @Query("SELECT * FROM account_files ORDER BY addedDate DESC")
    fun getAllFiles(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files WHERE fileType = :type ORDER BY addedDate DESC")
    fun getFilesByType(type: String): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files WHERE filename LIKE '%' || :query || '%' ORDER BY addedDate DESC")
    fun searchFiles(query: String): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files ORDER BY :orderBy ASC")
    fun getFilesSortedBy(orderBy: String): Flow<List<AccountFileEntity>>
    
    @Query("SELECT COUNT(*) FROM account_files")
    suspend fun getFileCount(): Int
    
    @Query("SELECT SUM(size) FROM account_files")
    suspend fun getTotalSize(): Long
    
    @Upsert
    suspend fun upsertFiles(files: List<AccountFileEntity>)
    
    @Delete
    suspend fun deleteFiles(files: List<AccountFileEntity>)
}
```

**FileBrowserCacheDao.kt**
```kotlin
@Dao
interface FileBrowserCacheDao {
    @Query("SELECT * FROM file_browser_cache WHERE cacheKey = :key AND lastUpdated > :ttl ORDER BY pageNumber ASC")
    suspend fun getCachedData(key: String, ttl: Long): List<FileBrowserCacheEntity>
    
    @Upsert
    suspend fun upsertCache(cache: FileBrowserCacheEntity)
    
    @Query("DELETE FROM file_browser_cache WHERE lastUpdated < :expiredTime")
    suspend fun cleanExpiredCache(expiredTime: Long)
}
```

#### 1.3 API Models

**AccountFileResponse.kt**
```kotlin
@JsonClass(generateAdapter = true)
data class AccountFileResponse(
    @Json(name = "data") val data: List<AccountFileItem>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "per_page") val perPage: Int
)

@JsonClass(generateAdapter = true)
data class AccountFileItem(
    @Json(name = "id") val id: String,
    @Json(name = "torrent_id") val torrentId: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "size") val size: Long,
    @Json(name = "added") val added: String,
    @Json(name = "status") val status: String,
    @Json(name = "download") val downloadUrl: String? = null
)
```

#### 1.4 Repository Layer

**AccountFileRepository.kt**
```kotlin
interface AccountFileRepository {
    fun getFiles(page: Int, limit: Int, sortBy: SortOption, filter: FilterOption): Flow<PagingData<AccountFileEntity>>
    suspend fun refreshFiles(): Result<Unit>
    suspend fun deleteFiles(fileIds: List<String>): Result<Unit>
    suspend fun getStorageUsage(): Result<StorageUsage>
    fun getFilesByType(type: FileType): Flow<List<AccountFileEntity>>
    suspend fun searchFiles(query: String): Flow<List<AccountFileEntity>>
    suspend fun getCachedFiles(): Flow<List<AccountFileEntity>>
}
```

**AccountFileRepositoryImpl.kt**
```kotlin
@Singleton
class AccountFileRepositoryImpl @Inject constructor(
    private val apiService: RealDebridApiService,
    private val accountFileDao: AccountFileDao,
    private val cacheDao: FileBrowserCacheDao,
    private val mapper: AccountFileMapper,
    private val cachingStrategy: FileBrowserCachingStrategy
) : AccountFileRepository {
    
    override fun getFiles(page: Int, limit: Int, sortBy: SortOption, filter: FilterOption): Flow<PagingData<AccountFileEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = limit,
                enablePlaceholders = false,
                prefetchDistance = 2
            ),
            pagingSourceFactory = {
                AccountFilePagingSource(apiService, accountFileDao, mapper, sortBy, filter)
            }
        ).flow
    }
    
    override suspend fun refreshFiles(): Result<Unit> {
        return try {
            val response = apiService.getAccountFiles(page = 1, limit = 50)
            if (response.isSuccessful) {
                val files = response.body()?.data?.map { mapper.mapToEntity(it) } ?: emptyList()
                accountFileDao.upsertFiles(files)
                Result.success(Unit)
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Implementation of other methods...
}
```

### 2. Domain Layer

#### 2.1 Use Cases

**GetAccountFilesUseCase.kt**
```kotlin
class GetAccountFilesUseCase @Inject constructor(
    private val repository: AccountFileRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    operator fun invoke(
        sortBy: SortOption = SortOption.DATE_DESC,
        filter: FilterOption = FilterOption.ALL,
        pageSize: Int = 20
    ): Flow<PagingData<AccountFileEntity>> = withContext(dispatcher) {
        repository.getFiles(
            page = 1,
            limit = pageSize,
            sortBy = sortBy,
            filter = filter
        )
    }
}
```

**DeleteAccountFilesUseCase.kt**
```kotlin
class DeleteAccountFilesUseCase @Inject constructor(
    private val repository: AccountFileRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(fileIds: List<String>): Result<Unit> = withContext(dispatcher) {
        repository.deleteFiles(fileIds)
    }
}
```

#### 2.2 Domain Models

**FileType.kt**
```kotlin
enum class FileType(val displayName: String, val extensions: List<String>) {
    VIDEO("Video", listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm")),
    AUDIO("Audio", listOf("mp3", "flac", "wav", "aac", "ogg", "m4a")),
    OTHER("Other", emptyList())
}
```

**SortOption.kt**
```kotlin
enum class SortOption(val displayName: String, val apiParam: String) {
    DATE_DESC("Newest First", "date_desc"),
    DATE_ASC("Oldest First", "date_asc"),
    NAME_ASC("Name A-Z", "name_asc"),
    NAME_DESC("Name Z-A", "name_desc"),
    SIZE_DESC("Largest First", "size_desc"),
    SIZE_ASC("Smallest First", "size_asc")
}
```

**FilterOption.kt**
```kotlin
data class FilterOption(
    val fileType: FileType? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val dateRange: DateRange? = null
)
```

### 3. Presentation Layer

#### 3.1 Navigation

**Screen.kt** (Addition)
```kotlin
@Serializable
data object AccountFileBrowser : Screen()

@Serializable
data class AccountFileDetails(val fileId: String) : Screen()
```

#### 3.2 UI State Management

**AccountFileBrowserState.kt**
```kotlin
data class AccountFileBrowserState(
    val files: LazyPagingItems<AccountFileEntity>? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val filterOption: FilterOption = FilterOption(),
    val selectedFiles: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val storageUsage: StorageUsage? = null,
    val isRefreshing: Boolean = false
)
```

**AccountFileBrowserViewModel.kt**
```kotlin
@HiltViewModel
class AccountFileBrowserViewModel @Inject constructor(
    private val getAccountFilesUseCase: GetAccountFilesUseCase,
    private val deleteAccountFilesUseCase: DeleteAccountFilesUseCase,
    private val getStorageUsageUseCase: GetStorageUsageUseCase
) : BaseViewModel<AccountFileBrowserState>() {
    
    override fun createInitialState(): AccountFileBrowserState = AccountFileBrowserState()
    
    val files = getAccountFilesUseCase(
        sortBy = uiState.value.sortOption,
        filter = uiState.value.filterOption
    ).cachedIn(viewModelScope)
    
    init {
        loadStorageUsage()
    }
    
    fun setSortOption(sortOption: SortOption) {
        updateState { copy(sortOption = sortOption) }
        refreshFiles()
    }
    
    fun setFilterOption(filterOption: FilterOption) {
        updateState { copy(filterOption = filterOption) }
        refreshFiles()
    }
    
    fun toggleFileSelection(fileId: String) {
        updateState {
            val newSelection = if (selectedFiles.contains(fileId)) {
                selectedFiles - fileId
            } else {
                selectedFiles + fileId
            }
            copy(selectedFiles = newSelection)
        }
    }
    
    fun toggleSelectionMode() {
        updateState {
            copy(
                isSelectionMode = !isSelectionMode,
                selectedFiles = if (isSelectionMode) emptySet() else selectedFiles
            )
        }
    }
    
    fun deleteSelectedFiles() {
        val selectedFileIds = uiState.value.selectedFiles.toList()
        if (selectedFileIds.isNotEmpty()) {
            launchSafely {
                updateState { copy(isLoading = true) }
                val result = deleteAccountFilesUseCase(selectedFileIds)
                updateState {
                    copy(
                        isLoading = false,
                        selectedFiles = emptySet(),
                        isSelectionMode = false,
                        error = if (result.isFailure) result.exceptionOrNull()?.message else null
                    )
                }
                if (result.isSuccess) {
                    refreshFiles()
                }
            }
        }
    }
    
    private fun refreshFiles() {
        // Trigger refresh of paging source
    }
    
    private fun loadStorageUsage() {
        launchSafely {
            val result = getStorageUsageUseCase()
            updateState {
                copy(storageUsage = result.getOrNull())
            }
        }
    }
}
```

#### 3.3 UI Components

**AccountFileBrowserScreen.kt**
```kotlin
@Composable
fun AccountFileBrowserScreen(
    navController: NavController,
    viewModel: AccountFileBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val files = viewModel.files.collectAsLazyPagingItems()
    
    LaunchedEffect(Unit) {
        files.refresh()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Storage usage header
        StorageUsageCard(
            usage = state.storageUsage,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toolbar with sorting and filtering
        FileBrowserToolbar(
            sortOption = state.sortOption,
            onSortChange = viewModel::setSortOption,
            filterOption = state.filterOption,
            onFilterChange = viewModel::setFilterOption,
            isSelectionMode = state.isSelectionMode,
            onToggleSelectionMode = viewModel::toggleSelectionMode,
            selectedCount = state.selectedFiles.size,
            onDeleteSelected = viewModel::deleteSelectedFiles,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // File list
        Box(modifier = Modifier.weight(1f)) {
            AccountFileList(
                files = files,
                selectedFiles = state.selectedFiles,
                isSelectionMode = state.isSelectionMode,
                onFileClick = { fileId ->
                    if (state.isSelectionMode) {
                        viewModel.toggleFileSelection(fileId)
                    } else {
                        navController.navigate(Screen.AccountFileDetails(fileId))
                    }
                },
                onFileLongClick = { fileId ->
                    viewModel.toggleFileSelection(fileId)
                    if (!state.isSelectionMode) {
                        viewModel.toggleSelectionMode()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Loading overlay
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    
    // Error handling
    state.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar or dialog
        }
    }
}
```

### 4. Dependency Injection

#### 4.1 Repository Module Update

**RepositoryModule.kt** (Addition)
```kotlin
@Binds
abstract fun bindAccountFileRepository(
    accountFileRepositoryImpl: AccountFileRepositoryImpl
): AccountFileRepository
```

#### 4.2 Database Module Update

**DatabaseModule.kt** (Addition)
```kotlin
@Provides
fun provideAccountFileDao(database: AppDatabase): AccountFileDao {
    return database.accountFileDao()
}

@Provides
fun provideFileBrowserCacheDao(database: AppDatabase): FileBrowserCacheDao {
    return database.fileBrowserCacheDao()
}
```

#### 4.3 Network Module Update

**NetworkModule.kt** (Addition)
```kotlin
// Add new endpoints to RealDebridApiService
@GET("account/files")
suspend fun getAccountFiles(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20,
    @Query("sort") sort: String? = null,
    @Query("filter") filter: String? = null
): Response<AccountFileResponse>

@DELETE("account/files/bulk")
suspend fun deleteAccountFiles(
    @Body fileIds: List<String>
): Response<Unit>
```

### 5. Caching Strategy

#### 5.1 Cache Implementation

**FileBrowserCachingStrategy.kt**
```kotlin
@Singleton
class FileBrowserCachingStrategy @Inject constructor(
    private val cacheDao: FileBrowserCacheDao
) {
    companion object {
        private const val CACHE_TTL = 3600000L // 1 hour
    }
    
    suspend fun getCachedData(key: String): List<FileBrowserCacheEntity>? {
        val currentTime = System.currentTimeMillis()
        val validTime = currentTime - CACHE_TTL
        
        return cacheDao.getCachedData(key, validTime).takeIf { it.isNotEmpty() }
    }
    
    suspend fun cacheData(key: String, pageNumber: Int, totalPages: Int, data: String) {
        val cacheEntity = FileBrowserCacheEntity(
            id = "$key-$pageNumber",
            cacheKey = key,
            pageNumber = pageNumber,
            totalPages = totalPages,
            data = data,
            lastUpdated = Date()
        )
        cacheDao.upsertCache(cacheEntity)
    }
    
    suspend fun cleanExpiredCache() {
        val expiredTime = System.currentTimeMillis() - CACHE_TTL
        cacheDao.cleanExpiredCache(expiredTime)
    }
}
```

### 6. Performance Optimization

#### 6.1 Pagination Strategy

**AccountFilePagingSource.kt**
```kotlin
class AccountFilePagingSource(
    private val apiService: RealDebridApiService,
    private val accountFileDao: AccountFileDao,
    private val mapper: AccountFileMapper,
    private val sortBy: SortOption,
    private val filter: FilterOption
) : PagingSource<Int, AccountFileEntity>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AccountFileEntity> {
        return try {
            val page = params.key ?: 1
            val loadSize = params.loadSize
            
            // Try to load from cache first
            val cachedData = loadFromCache(page, loadSize)
            if (cachedData.isNotEmpty()) {
                return LoadResult.Page(
                    data = cachedData,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (cachedData.size < loadSize) null else page + 1
                )
            }
            
            // Load from API
            val response = apiService.getAccountFiles(
                page = page,
                limit = loadSize,
                sort = sortBy.apiParam,
                filter = filter.toApiParam()
            )
            
            if (response.isSuccessful) {
                val files = response.body()?.data?.map { mapper.mapToEntity(it) } ?: emptyList()
                
                // Cache the data
                cacheData(page, files)
                
                LoadResult.Page(
                    data = files,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (files.size < loadSize) null else page + 1
                )
            } else {
                LoadResult.Error(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    private suspend fun loadFromCache(page: Int, loadSize: Int): List<AccountFileEntity> {
        // Implementation to load from local cache
        return emptyList()
    }
    
    private suspend fun cacheData(page: Int, files: List<AccountFileEntity>) {
        // Implementation to cache data locally
    }
}
```

### 7. Testing Strategy

#### 7.1 Unit Tests

- **Repository Tests**: Test caching logic, API integration, and data mapping
- **Use Case Tests**: Test business logic and error handling
- **ViewModel Tests**: Test state management and UI interactions
- **Paging Tests**: Test pagination logic and performance

#### 7.2 Integration Tests

- **Database Tests**: Test DAO operations and migrations
- **API Tests**: Test network calls and response handling
- **End-to-End Tests**: Test complete user workflows

### 8. Android TV Specific Considerations

#### 8.1 Focus Management

- Implement proper focus handling for D-pad navigation
- Use `FocusRequester` for custom focus flow
- Ensure all interactive elements are focusable

#### 8.2 TV-Optimized UI

- Large touch targets for remote control
- High contrast colors for TV displays
- Appropriate spacing for 10-foot viewing
- Keyboard shortcuts for power users

#### 8.3 Performance Optimization

- Lazy loading for large file lists
- Image caching for file thumbnails
- Memory management for long-running operations
- Background processing for file operations

### 9. Future Extensibility

#### 9.1 Planned Enhancements

- File preview functionality
- Batch operations (move, copy, rename)
- Advanced search with filters
- File sharing capabilities
- Offline access for downloaded files

#### 9.2 Architecture Scalability

- Modular design for easy feature addition
- Clean separation of concerns
- Extensible repository pattern
- Pluggable caching strategies

## Summary

This architecture provides a robust foundation for the Direct Account File Browser feature while maintaining consistency with the existing codebase patterns. The design emphasizes:

1. **Clean Architecture**: Clear separation between data, domain, and presentation layers
2. **MVVM Pattern**: Consistent with existing ViewModels and state management
3. **Dependency Injection**: Proper use of Hilt for dependency management
4. **Caching Strategy**: Efficient data caching with TTL management
5. **Performance**: Optimized for large file lists and Android TV constraints
6. **Testing**: Comprehensive testing strategy for all components
7. **TV-Specific**: Proper focus management and TV-optimized UI components

The architecture is designed to be extensible and maintainable, allowing for future enhancements while providing a solid foundation for the current requirements.