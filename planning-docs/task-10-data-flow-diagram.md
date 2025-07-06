# Direct Account File Browser - Data Flow & Dependency Injection

## Data Flow Architecture

### 1. Data Flow Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UI Layer      │    │  Domain Layer   │    │  Data Layer     │
│  (Compose)      │    │  (Use Cases)    │    │ (Repository)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   ViewModel     │◄──►│    Use Cases    │◄──►│   Repository    │
│   (State Mgmt)  │    │ (Business Logic)│    │ (Data Sources)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       ▼
         │                       │               ┌─────────────────┐
         │                       │               │   Data Sources  │
         │                       │               │  ┌─────────────┐ │
         │                       │               │  │ Remote API  │ │
         │                       │               │  └─────────────┘ │
         │                       │               │  ┌─────────────┐ │
         │                       │               │  │ Local DB    │ │
         │                       │               │  └─────────────┘ │
         │                       │               │  ┌─────────────┐ │
         │                       │               │  │ Cache Layer │ │
         │                       │               │  └─────────────┘ │
         │                       │               └─────────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│   UI State      │    │  Domain Models  │
│  (Compose)      │    │  (Entities)     │
└─────────────────┘    └─────────────────┘
```

### 2. Detailed Data Flow

#### 2.1 File List Loading Flow

```
User Action (Load Files)
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            UI Layer                                         │
│  AccountFileBrowserScreen.kt                                                │
│  ├─ collectAsLazyPagingItems()                                             │
│  ├─ LazyColumn with file items                                             │
│  └─ Pull-to-refresh mechanism                                              │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ViewModel Layer                                      │
│  AccountFileBrowserViewModel.kt                                             │
│  ├─ getAccountFilesUseCase()                                               │
│  ├─ State management (loading, error, success)                            │
│  └─ Pagination handling                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Use Case Layer                                       │
│  GetAccountFilesUseCase.kt                                                  │
│  ├─ Business logic for file retrieval                                     │
│  ├─ Pagination parameters                                                  │
│  └─ Error handling                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Repository Layer                                      │
│  AccountFileRepositoryImpl.kt                                               │
│  ├─ Coordinate data sources                                                │
│  ├─ Cache management                                                       │
│  └─ Network/Local data decision                                            │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Data Sources                                        │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐ │
│  │   Remote API        │  │   Local Database    │  │   Cache Layer       │ │
│  │  RealDebridApiService│  │  AccountFileDao     │  │  FileBrowserCache   │ │
│  │  ├─ GET /torrents    │  │  ├─ getAllFiles()   │  │  ├─ TTL management  │ │
│  │  ├─ Pagination       │  │  ├─ searchFiles()   │  │  ├─ Cache keys      │ │
│  │  └─ Error handling   │  │  └─ upsertFiles()   │  │  └─ Cleanup         │ │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 2.2 File Operations Flow

```
User Action (Delete Files)
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            UI Layer                                         │
│  ├─ Selection mode toggle                                                  │
│  ├─ Multi-select handling                                                  │
│  └─ Confirmation dialog                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ViewModel Layer                                      │
│  ├─ Selection state management                                             │
│  ├─ Bulk operations                                                        │
│  └─ Success/Error feedback                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Use Case Layer                                       │
│  DeleteAccountFilesUseCase.kt                                               │
│  ├─ Validation of file IDs                                                │
│  ├─ Batch processing                                                       │
│  └─ Result aggregation                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Repository Layer                                      │
│  ├─ API call for deletion                                                  │
│  ├─ Local cache invalidation                                              │
│  └─ Database update                                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3. Caching Strategy

#### 3.1 Multi-Level Caching

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Caching Levels                                    │
│                                                                             │
│  Level 1: In-Memory Cache (ViewModel)                                       │
│  ├─ Paging cache for current session                                       │
│  ├─ Search results cache                                                   │
│  └─ Recently accessed files                                                │
│                                                                             │
│  Level 2: Persistent Cache (Room Database)                                  │
│  ├─ File metadata with TTL                                                 │
│  ├─ Paginated result cache                                                 │
│  └─ User preferences                                                       │
│                                                                             │
│  Level 3: Network Cache (OkHttp)                                            │
│  ├─ HTTP response caching                                                  │
│  ├─ ETag support                                                           │
│  └─ Offline fallback                                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 3.2 Cache Invalidation Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Cache Invalidation Triggers                            │
│                                                                             │
│  Time-based Invalidation:                                                   │
│  ├─ File list TTL: 1 hour                                                  │
│  ├─ Storage usage TTL: 5 minutes                                           │
│  └─ Search results TTL: 10 minutes                                         │
│                                                                             │
│  Event-based Invalidation:                                                  │
│  ├─ File deletion → Invalidate file list                                   │
│  ├─ New file upload → Invalidate file list                                 │
│  └─ Sort/Filter change → Clear in-memory cache                             │
│                                                                             │
│  Manual Invalidation:                                                       │
│  ├─ Pull-to-refresh → Force refresh                                        │
│  ├─ App foreground → Check for updates                                     │
│  └─ Settings change → Clear all caches                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Dependency Injection Structure

### 1. Module Dependencies

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Hilt Module Graph                                  │
│                                                                             │
│  ApplicationModule                                                           │
│  ├─ @InstallIn(SingletonComponent::class)                                  │
│  └─ Core application dependencies                                           │
│                                                                             │
│  NetworkModule                                                               │
│  ├─ @InstallIn(SingletonComponent::class)                                  │
│  ├─ RealDebridApiService                                                   │
│  ├─ OkHttpClient                                                           │
│  └─ Retrofit                                                               │
│                                                                             │
│  DatabaseModule                                                              │
│  ├─ @InstallIn(SingletonComponent::class)                                  │
│  ├─ AppDatabase                                                            │
│  ├─ AccountFileDao                                                         │
│  └─ FileBrowserCacheDao                                                    │
│                                                                             │
│  RepositoryModule                                                            │
│  ├─ @InstallIn(SingletonComponent::class)                                  │
│  ├─ AccountFileRepository                                                  │
│  └─ FileBrowserCachingStrategy                                             │
│                                                                             │
│  ViewModelModule                                                             │
│  ├─ @InstallIn(ViewModelComponent::class)                                  │
│  └─ Use case dependencies                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2. Detailed Dependency Injection

#### 2.1 Network Module Enhancement

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun provideRealDebridApiService(
        @Named("authenticated") retrofit: Retrofit
    ): RealDebridApiService {
        return retrofit.create(RealDebridApiService::class.java)
    }

    @Provides
    @Named("account_files")
    fun provideAccountFileHttpClient(
        @Named("base") okHttpClient: OkHttpClient
    ): OkHttpClient {
        return okHttpClient.newBuilder()
            .addInterceptor(AccountFileInterceptor())
            .cache(Cache(
                directory = File(context.cacheDir, "account_files"),
                maxSize = 10 * 1024 * 1024 // 10MB
            ))
            .build()
    }
}
```

#### 2.2 Database Module Enhancement

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideAccountFileDao(database: AppDatabase): AccountFileDao {
        return database.accountFileDao()
    }

    @Provides
    fun provideFileBrowserCacheDao(database: AppDatabase): FileBrowserCacheDao {
        return database.fileBrowserCacheDao()
    }

    @Provides
    @Singleton
    fun provideFileBrowserCachingStrategy(
        cacheDao: FileBrowserCacheDao,
        @ApplicationContext context: Context
    ): FileBrowserCachingStrategy {
        return FileBrowserCachingStrategy(cacheDao, context)
    }
}
```

#### 2.3 Repository Module Enhancement

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAccountFileRepository(
        impl: AccountFileRepositoryImpl
    ): AccountFileRepository

    companion object {
        @Provides
        @Singleton
        fun provideAccountFileMapper(): AccountFileMapper {
            return AccountFileMapper()
        }

        @Provides
        fun provideAccountFilePagingSourceFactory(
            apiService: RealDebridApiService,
            accountFileDao: AccountFileDao,
            mapper: AccountFileMapper
        ): AccountFilePagingSourceFactory {
            return AccountFilePagingSourceFactory(apiService, accountFileDao, mapper)
        }
    }
}
```

#### 2.4 Use Case Module

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetAccountFilesUseCase(
        repository: AccountFileRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetAccountFilesUseCase {
        return GetAccountFilesUseCase(repository, dispatcher)
    }

    @Provides
    fun provideDeleteAccountFilesUseCase(
        repository: AccountFileRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): DeleteAccountFilesUseCase {
        return DeleteAccountFilesUseCase(repository, dispatcher)
    }

    @Provides
    fun provideGetStorageUsageUseCase(
        repository: AccountFileRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GetStorageUsageUseCase {
        return GetStorageUsageUseCase(repository, dispatcher)
    }

    @Provides
    fun provideSearchAccountFilesUseCase(
        repository: AccountFileRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): SearchAccountFilesUseCase {
        return SearchAccountFilesUseCase(repository, dispatcher)
    }
}
```

### 3. Scoping Strategy

#### 3.1 Singleton Components

```kotlin
// Long-lived components - Single instance per application
@Singleton
class AccountFileRepositoryImpl @Inject constructor(...)

@Singleton
class FileBrowserCachingStrategy @Inject constructor(...)

@Singleton
class RealDebridApiService @Inject constructor(...)
```

#### 3.2 ViewModel Components

```kotlin
// ViewModel-scoped components - New instance per ViewModel
@ViewModelScoped
class GetAccountFilesUseCase @Inject constructor(...)

@ViewModelScoped
class DeleteAccountFilesUseCase @Inject constructor(...)
```

#### 3.3 Activity Components

```kotlin
// Activity-scoped components - New instance per Activity
@ActivityScoped
class AccountFileNavigationHelper @Inject constructor(...)
```

### 4. Qualifier Annotations

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AccountFileApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AccountFileCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AccountFileDb
```

### 5. Testing Support

#### 5.1 Test Modules

```kotlin
@Module
@InstallIn(SingletonComponent::class)
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
abstract class TestRepositoryModule {

    @Binds
    abstract fun bindAccountFileRepository(
        impl: FakeAccountFileRepository
    ): AccountFileRepository
}
```

#### 5.2 Fake Implementations

```kotlin
class FakeAccountFileRepository : AccountFileRepository {
    private val fakeFiles = mutableListOf<AccountFileEntity>()
    
    override fun getFiles(
        page: Int,
        limit: Int,
        sortBy: SortOption,
        filter: FilterOption
    ): Flow<PagingData<AccountFileEntity>> {
        // Return fake data for testing
        return flowOf(PagingData.from(fakeFiles))
    }
    
    // Other fake implementations...
}
```

## Performance Considerations

### 1. Memory Management

```kotlin
// In AccountFileBrowserViewModel
override fun onCleared() {
    super.onCleared()
    // Clean up any resources
    pagingDataCache.clear()
    selectedFiles.clear()
}
```

### 2. Background Processing

```kotlin
// Use appropriate dispatchers
@IoDispatcher
private val ioDispatcher: CoroutineDispatcher

@DefaultDispatcher  
private val defaultDispatcher: CoroutineDispatcher
```

### 3. Error Handling

```kotlin
// Comprehensive error handling in repository
sealed class AccountFileError : Exception() {
    object NetworkError : AccountFileError()
    object CacheError : AccountFileError()
    object ApiError : AccountFileError()
    data class UnknownError(val throwable: Throwable) : AccountFileError()
}
```

This comprehensive data flow and dependency injection plan ensures:

1. **Clean Architecture**: Clear separation of concerns
2. **Testability**: Easy to mock and test components
3. **Performance**: Efficient caching and memory management
4. **Scalability**: Easy to extend and modify
5. **Maintainability**: Well-organized dependency graph
6. **Error Handling**: Comprehensive error management
7. **Android TV Optimization**: Specific considerations for TV platform