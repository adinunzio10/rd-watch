# Direct Account File Browser - Performance Optimization Plan

## Overview

This document outlines the performance optimization strategies for the Direct Account File Browser feature, specifically designed for Android TV constraints and large file collections.

## Performance Targets

### 1. Key Performance Indicators (KPIs)

| Metric | Target | Acceptable | Poor |
|--------|--------|------------|------|
| Initial Load Time | < 500ms | < 1s | > 2s |
| Scroll Performance | 60 FPS | 45 FPS | < 30 FPS |
| Search Response | < 300ms | < 500ms | > 1s |
| Memory Usage | < 50MB | < 100MB | > 150MB |
| Battery Impact | Minimal | Low | Moderate |
| Network Efficiency | < 1MB/page | < 2MB/page | > 5MB/page |

### 2. Android TV Specific Constraints

- **Memory**: Limited to 1-2GB total system memory
- **Storage**: Limited internal storage for caching
- **Network**: Often WiFi with variable bandwidth
- **CPU**: ARM-based processors with thermal constraints
- **UI**: 10-foot interface with D-pad navigation

## Memory Optimization

### 1. Efficient Data Structures

#### 1.1 Optimized Entity Design

```kotlin
// Memory-efficient entity design
@Entity(tableName = "account_files")
data class AccountFileEntity(
    @PrimaryKey val id: String,
    val torrentId: String,
    val filename: String,
    val size: Long,
    val fileType: Byte, // Use byte instead of string enum
    val addedDate: Long, // Use timestamp instead of Date object
    val status: Byte, // Use byte instead of string
    val downloadUrl: String? = null,
    val cached: Boolean = false,
    val lastSync: Long = System.currentTimeMillis()
) {
    companion object {
        // File type constants
        const val TYPE_VIDEO: Byte = 1
        const val TYPE_AUDIO: Byte = 2
        const val TYPE_OTHER: Byte = 3
        
        // Status constants
        const val STATUS_DOWNLOADING: Byte = 1
        const val STATUS_COMPLETED: Byte = 2
        const val STATUS_ERROR: Byte = 3
    }
}
```

#### 1.2 Memory Pool for Frequently Used Objects

```kotlin
@Singleton
class AccountFileObjectPool @Inject constructor() {
    private val fileItemPool = object : Pools.SynchronizedPool<AccountFileItem>(20) {
        override fun createNewInstance(): AccountFileItem {
            return AccountFileItem()
        }
    }
    
    fun acquireFileItem(): AccountFileItem {
        return fileItemPool.acquire() ?: AccountFileItem()
    }
    
    fun releaseFileItem(item: AccountFileItem) {
        item.reset() // Clear all fields
        fileItemPool.release(item)
    }
}
```

#### 1.3 Lazy Loading Strategies

```kotlin
// Lazy loading for expensive operations
class AccountFileEntity {
    val fileExtension: String by lazy {
        filename.substringAfterLast('.', "")
    }
    
    val formattedSize: String by lazy {
        formatFileSize(size)
    }
    
    val humanReadableDate: String by lazy {
        DateUtils.getRelativeTimeSpanString(addedDate).toString()
    }
}
```

### 2. Bitmap and Image Optimization

#### 2.1 Smart Image Loading

```kotlin
@Singleton
class AccountFileImageLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageCache = LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt() // Use 1/8 of available memory
    )
    
    suspend fun loadThumbnail(
        fileId: String,
        targetWidth: Int = 200,
        targetHeight: Int = 112
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${fileId}_${targetWidth}x${targetHeight}"
        
        // Check memory cache
        imageCache[cacheKey]?.let { return@withContext it }
        
        // Load from storage with downsampling
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        // Decode bounds
        BitmapFactory.decodeFile(getThumbnailPath(fileId), options)
        
        // Calculate sample size
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
        
        val bitmap = BitmapFactory.decodeFile(getThumbnailPath(fileId), options)
        bitmap?.let { imageCache.put(cacheKey, it) }
        
        bitmap
    }
    
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}
```

### 3. Memory Leak Prevention

#### 3.1 WeakReference for Callbacks

```kotlin
class AccountFilePagingSource(
    private val apiService: RealDebridApiService,
    private val dao: AccountFileDao
) : PagingSource<Int, AccountFileEntity>() {
    
    private var refreshCallback: WeakReference<() -> Unit>? = null
    
    fun setRefreshCallback(callback: () -> Unit) {
        refreshCallback = WeakReference(callback)
    }
    
    override fun invalidate() {
        super.invalidate()
        refreshCallback?.get()?.invoke()
        refreshCallback = null // Clear reference
    }
}
```

#### 3.2 Proper Coroutine Cleanup

```kotlin
@HiltViewModel
class AccountFileBrowserViewModel @Inject constructor(
    private val getAccountFilesUseCase: GetAccountFilesUseCase
) : BaseViewModel<AccountFileBrowserState>() {
    
    private val refreshJob = SupervisorJob()
    private val refreshScope = CoroutineScope(Dispatchers.Main + refreshJob)
    
    override fun onCleared() {
        super.onCleared()
        refreshJob.cancel() // Cancel all child coroutines
    }
}
```

## Database Performance

### 1. Query Optimization

#### 1.1 Efficient Indexing Strategy

```kotlin
@Entity(
    tableName = "account_files",
    indices = [
        Index(value = ["fileType", "addedDate"], name = "idx_type_date"),
        Index(value = ["size"], name = "idx_size"),
        Index(value = ["filename"], name = "idx_filename"),
        Index(value = ["torrentId"], name = "idx_torrent"),
        Index(value = ["status", "addedDate"], name = "idx_status_date")
    ]
)
data class AccountFileEntity(...)
```

#### 1.2 Optimized Queries

```kotlin
@Dao
interface AccountFileDao {
    
    // Use LIMIT for pagination instead of OFFSET for better performance
    @Query("""
        SELECT * FROM account_files 
        WHERE (:lastId IS NULL OR id > :lastId)
        AND (:fileType IS NULL OR fileType = :fileType)
        ORDER BY addedDate DESC, id ASC
        LIMIT :limit
    """)
    suspend fun getFilesAfter(
        lastId: String?,
        fileType: Byte?,
        limit: Int
    ): List<AccountFileEntity>
    
    // Efficient count query with conditions
    @Query("""
        SELECT COUNT(*) FROM account_files 
        WHERE (:fileType IS NULL OR fileType = :fileType)
        AND (:searchQuery IS NULL OR filename LIKE '%' || :searchQuery || '%')
    """)
    suspend fun getFilteredCount(fileType: Byte?, searchQuery: String?): Int
    
    // Bulk operations for better performance
    @Transaction
    suspend fun replaceFiles(newFiles: List<AccountFileEntity>) {
        clearAllFiles()
        insertFiles(newFiles)
    }
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<AccountFileEntity>)
    
    @Query("DELETE FROM account_files")
    suspend fun clearAllFiles()
}
```

#### 1.3 Database Connection Optimization

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rd_watch_database"
        )
        .setQueryExecutor(Executors.newFixedThreadPool(2)) // Limit DB threads
        .setTransactionExecutor(Executors.newSingleThreadExecutor()) // Single thread for transactions
        .enableMultiInstanceInvalidation(false) // Disable if single process
        .setJournalMode(RoomDatabase.JournalMode.WAL) // Use WAL mode for better concurrency
        .build()
    }
}
```

### 2. Caching Strategy

#### 2.1 Multi-Level Caching

```kotlin
@Singleton
class AccountFileCacheManager @Inject constructor(
    private val memoryCache: AccountFileMemoryCache,
    private val diskCache: AccountFileDiskCache,
    private val databaseCache: FileBrowserCacheDao
) {
    
    suspend fun getFiles(cacheKey: String): List<AccountFileEntity>? {
        // Level 1: Memory cache (fastest)
        memoryCache.get(cacheKey)?.let { return it }
        
        // Level 2: Disk cache (fast)
        diskCache.get(cacheKey)?.let { files ->
            memoryCache.put(cacheKey, files)
            return files
        }
        
        // Level 3: Database cache (slower but persistent)
        val currentTime = System.currentTimeMillis()
        databaseCache.getCachedData(cacheKey, currentTime - CACHE_TTL)
            .takeIf { it.isNotEmpty() }
            ?.let { cacheEntities ->
                val files = deserializeFiles(cacheEntities)
                memoryCache.put(cacheKey, files)
                diskCache.put(cacheKey, files)
                return files
            }
        
        return null
    }
    
    suspend fun putFiles(cacheKey: String, files: List<AccountFileEntity>) {
        // Store in all cache levels
        memoryCache.put(cacheKey, files)
        diskCache.put(cacheKey, files)
        
        val serializedData = serializeFiles(files)
        val cacheEntity = FileBrowserCacheEntity(
            id = cacheKey,
            cacheKey = cacheKey,
            pageNumber = 1,
            totalPages = 1,
            data = serializedData,
            lastUpdated = Date()
        )
        databaseCache.upsertCache(cacheEntity)
    }
}
```

#### 2.2 Smart Cache Eviction

```kotlin
class AccountFileMemoryCache @Inject constructor() {
    private val cache = object : LruCache<String, List<AccountFileEntity>>(
        calculateCacheSize()
    ) {
        override fun sizeOf(key: String, value: List<AccountFileEntity>): Int {
            return value.size * ESTIMATED_ENTITY_SIZE_BYTES
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: List<AccountFileEntity>,
            newValue: List<AccountFileEntity>?
        ) {
            if (evicted) {
                // Log cache eviction for monitoring
                Log.d("AccountFileCache", "Evicted cache entry: $key")
            }
        }
    }
    
    private fun calculateCacheSize(): Int {
        val maxMemory = Runtime.getRuntime().maxMemory()
        return (maxMemory / 16).toInt() // Use 1/16 of available memory
    }
    
    companion object {
        private const val ESTIMATED_ENTITY_SIZE_BYTES = 200 // Estimated size per entity
    }
}
```

## Network Performance

### 1. Request Optimization

#### 1.1 Request Batching

```kotlin
class AccountFileApiOptimizer @Inject constructor(
    private val apiService: RealDebridApiService,
    private val requestQueue: RequestQueue
) {
    
    suspend fun batchedFileRequests(
        requests: List<FileRequest>
    ): List<AccountFileEntity> = withContext(Dispatchers.IO) {
        
        val batchedResults = mutableListOf<AccountFileEntity>()
        
        // Group requests by type for batching
        val groupedRequests = requests.groupBy { it.type }
        
        groupedRequests.forEach { (type, requestList) ->
            when (type) {
                RequestType.FILE_LIST -> {
                    val combinedRequest = combineFileListRequests(requestList)
                    val response = apiService.getAccountFiles(
                        page = combinedRequest.page,
                        limit = combinedRequest.limit,
                        sort = combinedRequest.sort
                    )
                    response.body()?.files?.let { files ->
                        batchedResults.addAll(files.map { mapToEntity(it) })
                    }
                }
                RequestType.FILE_DETAILS -> {
                    // Batch file detail requests
                    val fileIds = requestList.map { it.fileId }
                    val detailsResponse = apiService.getBatchFileDetails(fileIds)
                    detailsResponse.body()?.let { details ->
                        batchedResults.addAll(details.map { mapToEntity(it) })
                    }
                }
            }
        }
        
        batchedResults
    }
}
```

#### 1.2 Request Deduplication

```kotlin
@Singleton
class RequestDeduplicator @Inject constructor() {
    private val activeRequests = ConcurrentHashMap<String, Deferred<ApiResponse>>()
    
    suspend fun <T> deduplicate(
        key: String,
        request: suspend () -> T
    ): T {
        return activeRequests.getOrPut(key) {
            CoroutineScope(Dispatchers.IO).async {
                try {
                    request()
                } finally {
                    activeRequests.remove(key)
                }
            }
        }.await()
    }
}
```

#### 1.3 Connection Pool Optimization

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(
                maxIdleConnections = 5,
                keepAliveDuration = 30,
                timeUnit = TimeUnit.SECONDS
            ))
            .dispatcher(Dispatcher().apply {
                maxRequests = 20
                maxRequestsPerHost = 5
            })
            .cache(Cache(
                directory = File(context.cacheDir, "http_cache"),
                maxSize = 10L * 1024L * 1024L // 10 MB
            ))
            .addInterceptor(CacheInterceptor())
            .addNetworkInterceptor(NetworkCacheInterceptor())
            .build()
    }
}
```

### 2. Adaptive Loading

#### 2.1 Network-Aware Loading

```kotlin
@Singleton
class AdaptiveLoadingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor
) {
    
    fun getOptimalPageSize(): Int {
        return when (networkMonitor.getCurrentNetworkType()) {
            NetworkType.WIFI -> 50
            NetworkType.CELLULAR_4G -> 20
            NetworkType.CELLULAR_3G -> 10
            NetworkType.CELLULAR_2G -> 5
            NetworkType.NONE -> 0
        }
    }
    
    fun shouldPreloadThumbnails(): Boolean {
        return when (networkMonitor.getCurrentNetworkType()) {
            NetworkType.WIFI -> true
            NetworkType.CELLULAR_4G -> false
            else -> false
        }
    }
    
    fun getImageQuality(): ImageQuality {
        return when (networkMonitor.getCurrentNetworkType()) {
            NetworkType.WIFI -> ImageQuality.HIGH
            NetworkType.CELLULAR_4G -> ImageQuality.MEDIUM
            else -> ImageQuality.LOW
        }
    }
}
```

#### 2.2 Progressive Loading

```kotlin
class ProgressiveFileLoader @Inject constructor(
    private val repository: AccountFileRepository,
    private val adaptiveLoadingManager: AdaptiveLoadingManager
) {
    
    fun loadFilesProgressively(): Flow<List<AccountFileEntity>> = flow {
        var page = 1
        val pageSize = adaptiveLoadingManager.getOptimalPageSize()
        val allFiles = mutableListOf<AccountFileEntity>()
        
        while (true) {
            try {
                val files = repository.getFiles(
                    page = page,
                    limit = pageSize,
                    sortBy = SortOption.DATE_DESC,
                    filter = FilterOption()
                ).first()
                
                if (files.isEmpty()) break
                
                allFiles.addAll(files)
                emit(allFiles.toList()) // Emit progressive updates
                
                page++
                
                // Add delay based on network conditions
                delay(getLoadDelay())
                
            } catch (e: Exception) {
                // Handle error and break or retry
                break
            }
        }
    }
    
    private fun getLoadDelay(): Long {
        return when (adaptiveLoadingManager.getNetworkType()) {
            NetworkType.WIFI -> 100L
            NetworkType.CELLULAR_4G -> 300L
            NetworkType.CELLULAR_3G -> 500L
            else -> 1000L
        }
    }
}
```

## UI Performance

### 1. Compose Optimization

#### 1.1 Stable Classes for Performance

```kotlin
@Stable
data class AccountFileUiModel(
    val id: String,
    val filename: String,
    val size: String,
    val formattedDate: String,
    val fileType: FileType,
    val isSelected: Boolean = false,
    val thumbnailUrl: String? = null
) {
    // Implement equals and hashCode for stability
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountFileUiModel) return false
        return id == other.id && isSelected == other.isSelected
    }
    
    override fun hashCode(): Int {
        return id.hashCode() * 31 + isSelected.hashCode()
    }
}

@Immutable
data class FileBrowserUiState(
    val files: ImmutableList<AccountFileUiModel>,
    val isLoading: Boolean,
    val error: String?
)
```

#### 1.2 Optimized LazyColumn

```kotlin
@Composable
fun AccountFileList(
    files: LazyPagingItems<AccountFileEntity>,
    selectedFiles: Set<String>,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = files.itemCount,
            key = { index -> files[index]?.id ?: index }
        ) { index ->
            val file = files[index]
            if (file != null) {
                AccountFileItem(
                    file = file,
                    isSelected = selectedFiles.contains(file.id),
                    onClick = { onFileClick(file.id) },
                    modifier = Modifier.animateItemPlacement()
                )
            } else {
                AccountFileItemPlaceholder()
            }
        }
    }
}

@Composable
private fun AccountFileItem(
    file: AccountFileEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Memoize expensive calculations
    val formattedSize = remember(file.size) { 
        formatFileSize(file.size) 
    }
    val formattedDate = remember(file.addedDate) { 
        formatDate(file.addedDate) 
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Icon(
                imageVector = getFileTypeIcon(file.fileType),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.filename,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$formattedSize • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

#### 1.3 Deferred Composition

```kotlin
@Composable
fun DeferredAccountFileDetails(
    fileId: String,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Defer heavy composition until next frame
        delay(16) // One frame at 60fps
        showDetails = true
    }
    
    if (showDetails) {
        AccountFileDetailsContent(
            fileId = fileId,
            modifier = modifier
        )
    } else {
        AccountFileDetailsPlaceholder(modifier = modifier)
    }
}
```

### 2. Focus Management Optimization

#### 2.1 Efficient Focus Handling

```kotlin
@Composable
fun OptimizedAccountFileList(
    files: LazyPagingItems<AccountFileEntity>,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    
    // Optimize focus requests
    val focusRequesters = remember(files.itemCount) {
        mutableMapOf<Int, FocusRequester>()
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(
            count = files.itemCount,
            key = { index -> files[index]?.id ?: index }
        ) { index ->
            val file = files[index]
            if (file != null) {
                val focusRequester = remember(index) {
                    focusRequesters.getOrPut(index) { FocusRequester() }
                }
                
                AccountFileItem(
                    file = file,
                    focusRequester = focusRequester,
                    onFocusChange = { hasFocus ->
                        if (hasFocus) {
                            // Ensure item is visible
                            listState.animateScrollToItem(index)
                        }
                    }
                )
            }
        }
    }
}
```

## Background Processing

### 1. Efficient Background Sync

#### 1.1 WorkManager Integration

```kotlin
@HiltWorker
class AccountFileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AccountFileRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val result = repository.syncAccountFiles()
            
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): AccountFileSyncWorker
    }
}

// Schedule sync work
@Singleton
class AccountFileSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<AccountFileSyncWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
        .setConstraints(constraints)
        .setInitialDelay(5, TimeUnit.MINUTES)
        .build()
        
        workManager.enqueueUniquePeriodicWork(
            "account_file_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
```

### 2. Background Cache Management

#### 2.1 Intelligent Cache Cleanup

```kotlin
@Singleton
class AccountFileCacheCleanup @Inject constructor(
    private val cacheDao: FileBrowserCacheDao,
    private val diskCache: AccountFileDiskCache
) {
    
    suspend fun performCleanup() = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        
        // Clean expired database cache
        cacheDao.cleanExpiredCache(currentTime - CACHE_TTL)
        
        // Clean LRU disk cache
        diskCache.evictExpired()
        
        // Clean unused thumbnails
        cleanUnusedThumbnails()
        
        // Compact database if needed
        compactDatabaseIfNeeded()
    }
    
    private suspend fun cleanUnusedThumbnails() {
        val thumbnailDir = File(context.cacheDir, "thumbnails")
        if (thumbnailDir.exists()) {
            val currentFileIds = cacheDao.getAllActiveFileIds()
            
            thumbnailDir.listFiles()?.forEach { file ->
                val fileId = file.nameWithoutExtension
                if (fileId !in currentFileIds) {
                    file.delete()
                }
            }
        }
    }
}
```

## Monitoring and Metrics

### 1. Performance Monitoring

#### 1.1 Custom Performance Metrics

```kotlin
@Singleton
class AccountFilePerformanceMonitor @Inject constructor() {
    
    fun measureLoadTime(operation: String, block: suspend () -> Unit) {
        val startTime = System.currentTimeMillis()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                block()
            } finally {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                logPerformanceMetric(operation, duration)
                
                if (duration > PERFORMANCE_THRESHOLD) {
                    logSlowOperation(operation, duration)
                }
            }
        }
    }
    
    private fun logPerformanceMetric(operation: String, duration: Long) {
        Log.d("Performance", "$operation took ${duration}ms")
        
        // Send to analytics if available
        Analytics.trackEvent("performance", mapOf(
            "operation" to operation,
            "duration_ms" to duration
        ))
    }
    
    companion object {
        private const val PERFORMANCE_THRESHOLD = 1000L // 1 second
    }
}
```

#### 1.2 Memory Usage Tracking

```kotlin
class MemoryMonitor @Inject constructor() {
    
    fun trackMemoryUsage(tag: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryPercent = (usedMemory * 100) / maxMemory
        
        Log.d("Memory", "$tag - Used: ${usedMemory / 1024 / 1024}MB (${memoryPercent}%)")
        
        if (memoryPercent > 80) {
            Log.w("Memory", "High memory usage detected: ${memoryPercent}%")
            // Trigger garbage collection
            System.gc()
        }
    }
}
```

### 2. Error Tracking

#### 2.1 Performance Error Detection

```kotlin
class PerformanceErrorDetector @Inject constructor(
    private val errorReporter: ErrorReporter
) {
    
    fun detectANRs() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                val startTime = System.currentTimeMillis()
                
                handler.postDelayed({
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    
                    if (duration > 5000) { // 5 second ANR threshold
                        errorReporter.reportANR(duration)
                    }
                    
                    handler.postDelayed(this, 1000) // Check every second
                }, 0)
            }
        })
    }
}
```

## Performance Testing

### 1. Automated Performance Tests

#### 1.1 Load Testing

```kotlin
@Test
fun loadTest_1000Files_performsWithinLimits() = runTest {
    // Generate 1000 test files
    val testFiles = generateTestFiles(1000)
    
    // Measure insertion time
    val insertionTime = measureTimeMillis {
        repository.insertFiles(testFiles)
    }
    
    // Measure query time
    val queryTime = measureTimeMillis {
        repository.getAllFiles().first()
    }
    
    // Assert performance requirements
    assertThat(insertionTime).isLessThan(5000) // 5 seconds max
    assertThat(queryTime).isLessThan(1000) // 1 second max
}
```

#### 1.2 Memory Leak Tests

```kotlin
@Test
fun memoryLeakTest_viewModelLifecycle() {
    val initialMemory = getUsedMemory()
    
    repeat(100) {
        val viewModel = AccountFileBrowserViewModel(...)
        // Simulate usage
        viewModel.loadFiles()
        // Clear references
        viewModel.onCleared()
    }
    
    System.gc()
    Thread.sleep(1000) // Allow GC to run
    
    val finalMemory = getUsedMemory()
    val memoryIncrease = finalMemory - initialMemory
    
    assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024) // 50MB max increase
}
```

## Summary

This comprehensive performance optimization plan addresses:

1. **Memory Efficiency**: Optimized data structures, object pooling, and lazy loading
2. **Database Performance**: Efficient queries, indexing, and caching strategies
3. **Network Optimization**: Request batching, deduplication, and adaptive loading
4. **UI Performance**: Stable composables, efficient LazyColumn, and optimized focus management
5. **Background Processing**: Intelligent sync and cache management
6. **Monitoring**: Performance metrics and error detection
7. **Testing**: Automated performance and memory leak tests

The plan ensures the Account File Browser performs well on Android TV devices while handling large file collections efficiently.