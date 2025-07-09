# CLAUDE Architecture Documentation

This file contains detailed architecture information for the RD Watch Android TV application. It is automatically maintained by Claude Code as the codebase evolves.

## Application Structure

### Technology Stack

- **UI Framework**: Jetpack Compose with Material3
- **TV Framework**: Android Leanback (transitioning from)
- **Image Loading**: Glide 4.11.0
- **Navigation**: Focus-based D-pad navigation with Compose Navigation
- **Language**: Kotlin with Java 11 compatibility
- **Dependency Injection**: Hilt/Dagger
- **Database**: Room with SQLite
- **Network**: Retrofit with OkHttp
- **Async**: Coroutines with StateFlow/SharedFlow
- **External APIs**: TMDb API 3.0 for movie/TV metadata
- **Data Serialization**: Moshi with Kotlin reflection

### Current Architecture State

The app is in **transition** from traditional Android Leanback framework to modern Jetpack Compose:

- **New Code**: Uses Compose with Material3 (MainActivity, Theme, Type)
- **Legacy Code**: Traditional Leanback activities still present
- **Data**: Real-Debrid integration with dynamic content loading
- **File Browser**: Complete AccountFileBrowser system with TV-optimized UI

### Directory Structure

```
app/src/main/java/com/rdwatch/androidtv/
├── MainActivity.kt              # Main entry point with Compose UI
├── Movie.kt                     # Data model for video content
├── MovieList.kt                 # Static data provider
├── ui/
│   ├── theme/                  # Material3 theme configuration
│   │   ├── Theme.kt            # Theme definitions
│   │   └── Type.kt             # TV-optimized typography
│   ├── details/                # Content Details System
│   │   ├── components/         # Content detail UI components
│   │   │   ├── HeroSection.kt  # Media header with backdrop, poster, metadata
│   │   │   ├── InfoSection.kt  # Content information display
│   │   │   ├── ActionSection.kt # Action buttons (play, add to list, etc.)
│   │   │   └── RelatedSection.kt # Related content recommendations
│   │   ├── layouts/            # Layout containers
│   │   │   └── BaseDetailLayout.kt # Base layout for content detail screens
│   │   ├── models/             # Content detail data models
│   │   │   ├── ContentDetail.kt # Interface for content representation
│   │   │   ├── DetailUiState.kt # UI state for detail screens
│   │   │   ├── MovieContentDetail.kt # Movie-specific implementation
│   │   │   └── TMDbContentDetail.kt # TMDb-enhanced content details
│   │   └── MovieDetailsScreen.kt # Main movie detail screen
│   ├── filebrowser/            # Account File Browser System
│   │   ├── AccountFileBrowserScreen.kt     # Main UI screen
│   │   ├── AccountFileBrowserViewModel.kt  # State management
│   │   ├── components/         # Reusable UI components
│   │   │   ├── BulkSelectionMode.kt
│   │   │   ├── EnhancedFilterPanel.kt
│   │   │   ├── SortingUI.kt
│   │   │   └── FileBrowserEnhancedFeatures.kt
│   │   ├── models/             # Data models and state
│   │   │   ├── FileBrowserState.kt
│   │   │   ├── FileBrowserEvents.kt
│   │   │   ├── BulkOperationState.kt
│   │   │   └── RefreshState.kt
│   │   ├── repository/         # Data access layer
│   │   │   ├── FileBrowserRepository.kt
│   │   │   └── RealDebridFileBrowserRepository.kt
│   │   ├── cache/              # Caching system
│   │   │   └── FileBrowserCacheManager.kt
│   │   ├── mappers/            # Data transformation
│   │   │   └── FileBrowserMappers.kt
│   │   └── navigation/         # Navigation helpers
│   │       └── FileBrowserNavigationHelper.kt
│   ├── common/                 # Shared UI components
│   │   └── UiState.kt          # Common UI state definitions
│   └── focus/                  # TV focus management
│       └── TVFocusIndicator.kt
├── presentation/               # Navigation and app structure
│   ├── navigation/
│   │   ├── AppNavigation.kt
│   │   └── Screen.kt           # Navigation destinations
│   └── viewmodel/
│       └── BaseViewModel.kt    # Base ViewModel with error handling
├── repository/                 # Data repositories
│   ├── base/
│   │   ├── BaseRepository.kt
│   │   └── Result.kt           # Result wrapper for API responses
│   └── [other repositories]
├── network/                    # API integration
│   ├── api/
│   │   └── RealDebridApiService.kt
│   ├── models/
│   │   ├── TorrentInfo.kt
│   │   └── UserInfo.kt
│   └── interceptors/
│       └── AuthInterceptor.kt
├── auth/                       # Authentication system
│   ├── AuthManager.kt
│   ├── AuthRepository.kt
│   └── models/
│       └── AuthState.kt
├── di/                         # Dependency injection
│   ├── FileBrowserModule.kt
│   ├── NetworkModule.kt
│   └── [other modules]
└── [legacy leanback files]     # Being phased out
```

## Content Details System Architecture

### Overview

The Content Details System provides a unified architecture for displaying detailed information about movies, TV shows, and other media content. It features a modular component-based design, TMDb integration, and TV-optimized UI patterns.

### Core Components

#### 1. HeroSection Component (`HeroSection.kt`)
- **Purpose**: Media header component with backdrop, floating poster, and metadata
- **Key Features**:
  - Backdrop image with subtle blur effect for better text readability
  - Floating poster image with shadow elevation and rounded corners
  - Content-type specific metadata display (movie vs TV show vs episode)
  - Rating badges with star icons and quality indicators (4K, HDR, HD)
  - Progress indicators for partially watched content
  - Enhanced typography hierarchy with proper contrast
  - TV-optimized action buttons with focus states

#### 2. BaseDetailLayout (`BaseDetailLayout.kt`)
- **Purpose**: Layout container for content detail screens
- **Key Features**:
  - Modular section-based layout system
  - Loading, error, and content states
  - TV-optimized overscan margin handling
  - Lazy loading with proper focus management
  - Integration with DetailUiState for reactive updates

#### 3. ContentDetail Model System (`models/`)
- **ContentDetail.kt**: Interface for unified content representation
- **DetailUiState.kt**: UI state management with progress tracking
- **TMDbContentDetail.kt**: TMDb-enhanced content implementations
- **Key Features**:
  - Content-type specific metadata handling
  - Action system for user interactions
  - Progress tracking for watch history
  - Extensible metadata system with custom fields

### Media Header Component Features

#### Visual Elements
- **Backdrop Image**: Full-width background with blur effect
- **Floating Poster**: Overlay positioned poster with shadow
- **Title Typography**: Large, bold text with proper contrast
- **Metadata Badges**: Color-coded quality and rating indicators
- **Action Buttons**: Primary play/resume buttons with focus states

#### Content-Type Variations
- **Movies**: Runtime, director, studio, year, rating
- **TV Shows**: Seasons, episodes, network, year, rating
- **Episodes**: Season/episode number, runtime, air date

#### Rating System
- **Star Rating**: Gold star icons with numeric ratings
- **Quality Badges**: Color-coded 4K (green), HDR (orange), HD (blue)
- **Custom Metadata**: Extensible system for additional info

### Integration Patterns

#### TMDb Integration
```kotlin
// TMDb-specific ContentDetail with enhanced metadata
data class TMDbMovieContentDetail(
    private val tmdbMovie: TMDbMovieResponse,
    private val credits: TMDbCreditsResponse? = null
) : ContentDetail {
    
    override val metadata: ContentMetadata = ContentMetadata(
        year = tmdbMovie.releaseDate?.take(4),
        duration = formatRuntime(tmdbMovie.runtime),
        rating = formatTMDbRating(tmdbMovie.voteAverage),
        cast = credits?.cast?.take(5)?.map { it.name } ?: emptyList(),
        director = credits?.crew?.find { it.job == "Director" }?.name
    )
}
```

#### Progress Tracking
```kotlin
// ContentProgress integration with HeroSection
data class ContentProgress(
    val watchPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val resumePosition: Long = 0L,
    val totalDuration: Long = 0L
)

// Usage in HeroSection
if (progress.isPartiallyWatched) {
    // Show "Resume Playing" button
    // Display progress indicator
}
```

#### TV-Optimized UI Patterns
```kotlin
// Enhanced action button with focus states
@Composable
private fun HeroActionButton(
    action: ContentAction,
    onClick: () -> Unit,
    isResume: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .tvFocusable(onFocusChanged = { isFocused = it.isFocused })
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                }
            ),
            shape = RoundedCornerShape(26.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isFocused) 8.dp else 4.dp
            )
        ) {
            // Button content with icon and text
        }
    }
}
```

### Testing Strategy

#### Component Testing
- **HeroSection**: Content display, metadata rendering, action handling
- **BaseDetailLayout**: State management, section visibility, navigation
- **ContentDetail**: Data transformation, content-type specifics

#### Integration Testing
- **TMDb Integration**: API data to ContentDetail transformation
- **Progress Tracking**: Watch history integration
- **Navigation**: Screen transitions and focus management

#### TV-Specific Testing
- **Focus Management**: D-pad navigation, focus restoration
- **Visual Regression**: Typography, layout, color contrast
- **Performance**: Image loading, blur effects, animations

## AccountFileBrowser System Architecture

### Overview

The AccountFileBrowser system provides a TV-optimized interface for browsing and managing files from debrid services like Real-Debrid. It features a complete MVVM architecture with reactive data flow, TV-specific UI components, and comprehensive error handling.

### Core Components

#### 1. Screen Layer (`AccountFileBrowserScreen.kt`)
- **Purpose**: Main UI screen with TV-optimized Compose components
- **Key Features**:
  - D-pad navigation with focus management
  - Bulk selection mode for multi-file operations
  - Enhanced filtering and sorting controls
  - Pagination support for large file lists
  - Real-time status indicators for downloads/transfers

#### 2. ViewModel Layer (`AccountFileBrowserViewModel.kt`)
- **Purpose**: State management and business logic
- **Architecture**: Extends `BaseViewModel` with reactive state handling
- **Key Features**:
  - Reactive state management with `StateFlow`
  - One-time event handling with `SharedFlow`
  - Authentication verification and error recovery
  - Bulk operations with progress tracking
  - Navigation history management

#### 3. Repository Layer (`FileBrowserRepository.kt`)
- **Purpose**: Data access abstraction for multiple debrid services
- **Implementation**: `RealDebridFileBrowserRepository.kt`
- **Key Features**:
  - Reactive data flow with `Flow<Result<T>>`
  - Caching integration for performance
  - Search and filtering capabilities
  - Bulk operation support with progress callbacks
  - Error handling with typed exceptions

#### 4. Model Layer (`models/`)
- **FileBrowserState.kt**: Complete UI state representation
- **FileBrowserEvents.kt**: One-time UI events and navigation
- **BulkOperationState.kt**: Bulk operation progress tracking
- **RefreshState.kt**: Pull-to-refresh state management

### Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    AccountFileBrowserScreen                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   File List     │  │ Filter Panel    │  │ Bulk Controls   │ │
│  │   Components    │  │   Components    │  │   Components    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────┬───────────────────────────────────────┘
                          │ UI Events
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                AccountFileBrowserViewModel                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   State Flow    │  │   Events Flow   │  │  Error Handler  │ │
│  │ (FileBrowserState)│  │(FileBrowserEvent)│  │   (BaseVM)      │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────┬───────────────────────────────────────┘
                          │ Repository Calls
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    FileBrowserRepository                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   API Service   │  │   Cache Layer   │  │   Mappers       │ │
│  │ (RealDebridAPI) │  │ (FileBrowserCache)│  │ (Entity→Model) │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────┬───────────────────────────────────────┘
                          │ Network Calls
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Real-Debrid API                           │
│           (Torrents, Downloads, Streaming URLs)                │
└─────────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

#### 1. Reactive Architecture
- **StateFlow**: UI state management with automatic recomposition
- **SharedFlow**: One-time events (navigation, errors, confirmations)
- **Flow**: Reactive data streams from repository to UI

#### 2. TV-Optimized UI Patterns
- **Focus Management**: Custom `TVFocusIndicator` components
- **D-pad Navigation**: Proper focus ordering and key handling
- **Bulk Operations**: Long-press for selection, TV-friendly multi-select
- **Pagination**: Infinite scroll with load-more triggers

#### 3. Error Handling Strategy
- **Typed Exceptions**: `Result<T>` wrapper for API responses
- **Recovery Actions**: Automatic retry with exponential backoff
- **User Feedback**: Clear error messages with actionable suggestions

### API Integration Patterns

#### 1. Repository Pattern
```kotlin
interface FileBrowserRepository {
    fun getRootContent(): Flow<Result<List<FileItem>>>
    suspend fun getTorrentFiles(torrentId: String): Result<List<FileItem.File>>
    suspend fun getPlaybackUrl(fileId: String): Result<String>
    suspend fun deleteItems(itemIds: Set<String>): Result<Unit>
    suspend fun downloadFiles(fileIds: Set<String>): Result<Unit>
}
```

#### 2. Data Transformation
```kotlin
// API Response → Domain Model
fun TorrentInfo.toFileItem(): FileItem.Torrent
fun UnrestrictLink.toFileItem(): FileItem.File

// Domain Model → UI Model
fun FileItem.toDisplayModel(): DisplayFileItem
```

#### 3. Caching Strategy
```kotlin
class FileBrowserCacheManager {
    suspend fun getCachedContent(key: String): List<FileItem>?
    suspend fun cacheContent(key: String, content: List<FileItem>)
    suspend fun invalidateCache(key: String)
}
```

### Navigation Integration

#### 1. Screen Definition
```kotlin
@Serializable
data class AccountFileBrowser(val accountType: String = "realdebrid") : Screen()
```

#### 2. Navigation Events
```kotlin
sealed class FileBrowserEvent {
    data class NavigateToPlayer(val url: String, val title: String) : FileBrowserEvent()
    data class ShowFileDetails(val file: FileItem) : FileBrowserEvent()
    object NavigateBack : FileBrowserEvent()
}
```

### TV-Specific Considerations

#### 1. Focus Management
- Custom `TVFocusIndicator` wrapper for visual feedback
- Focus restoration after dialog dismissal
- Proper focus ordering for D-pad navigation

#### 2. Performance Optimization
- Lazy loading with pagination
- Image loading optimization for TV screens
- Efficient list updates with DiffUtil

#### 3. User Experience
- Clear visual hierarchy with proper contrast
- Large touch targets for D-pad selection
- Consistent animation patterns
- Accessibility support for screen readers

## Key Configuration Files

### Build Configuration

- **app/build.gradle.kts**: Main app module build configuration
- **gradle/libs.versions.toml**: Version catalog for all dependencies
- **settings.gradle.kts**: Project structure and repositories

### Android Configuration

- **AndroidManifest.xml**: TV-specific permissions and intent filters
- **proguard-rules.pro**: Code obfuscation rules (currently default)

### Development Tools

- **package.json**: Task Master AI integration for project management
- **.mcp.json**: MCP server configuration for Claude Code

## Android TV Specific Considerations

### Focus Management

- All UI components must support D-pad navigation
- Focus states are critical for user experience
- Use `focusRequester` and `onFocusChanged` modifiers in Compose

### Screen Layout

- **Orientation**: Locked to landscape
- **Safe Area**: Account for TV overscan areas
- **Typography**: Larger font sizes for 10-foot viewing distance
- **Colors**: High contrast for TV displays

### Leanback Integration

- **Intent Filter**: `LEANBACK_LAUNCHER` category for Android TV launcher
- **Features**: `android.software.leanback` required
- **Permissions**: Internet access for streaming content

## Development Patterns

### Adding New Screens

1. Create Composable functions following TV design patterns
2. Implement proper focus management
3. Use Material3 TV-optimized components
4. Test with D-pad navigation
5. Consider overscan and safe areas

### Data Management

**Current**: Static data in `MovieList.kt`

**Future considerations**:
- Repository pattern for data access
- ViewModels for state management
- Retrofit for API communication
- Room for local caching

### Common Development Tasks

#### Adding Video Content

1. Update `Movie.kt` data model if needed
2. Add content to `MovieList.kt` or implement dynamic data source
3. Update UI components to handle new content types
4. Test with focus navigation

#### Modifying UI Components

1. Locate Compose components in `MainActivity.kt`
2. Follow Material3 design guidelines
3. Ensure TV accessibility (focus, sizing, contrast)
4. Test on actual TV device or emulator

#### Theme Customization

1. Modify colors in `ui/theme/Theme.kt`
2. Update typography in `ui/theme/Type.kt`
3. Ensure sufficient contrast for TV viewing
4. Test in both light and dark themes

## Dependencies & Versions

### Core Dependencies

- **Jetpack Compose BOM**: 2024.12.01
- **Compose Compiler**: 1.5.15
- **Android Leanback**: 1.2.0
- **AndroidX Core KTX**: 1.16.0
- **Activity Compose**: 1.9.3
- **Glide**: 4.11.0

### AccountFileBrowser Dependencies

- **Hilt/Dagger**: Dependency injection for ViewModels and Repositories
- **Retrofit**: RESTful API client for Real-Debrid integration
- **OkHttp**: HTTP client with interceptors for authentication
- **Room**: Local database for caching and offline support
- **Navigation Compose**: Type-safe navigation with Compose
- **Coroutines**: Asynchronous programming with Flow and StateFlow

### Future TV-Specific Dependencies

Available when stable (currently alpha):
- **TV Foundation**: 1.0.0-alpha11
- **TV Material**: 1.0.0-alpha11

## AccountFileBrowser Usage Examples

### Basic Integration

#### 1. Navigation Setup
```kotlin
// In AppNavigation.kt
composable<Screen.AccountFileBrowser> { backStackEntry ->
    val accountType = backStackEntry.toRoute<Screen.AccountFileBrowser>().accountType
    AccountFileBrowserScreen(
        onBackPressed = { navController.navigateUp() },
        onFileClick = { file ->
            navController.navigate(Screen.VideoPlayer(file.streamUrl ?: "", file.name))
        }
    )
}
```

#### 2. ViewModel Usage
```kotlin
// In AccountFileBrowserViewModel
class AccountFileBrowserViewModel @Inject constructor(
    private val fileBrowserRepository: FileBrowserRepository
) : BaseViewModel<FileBrowserState>() {
    
    // Reactive state management
    val uiState: StateFlow<FileBrowserState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<FileBrowserEvent>()
    val events: SharedFlow<FileBrowserEvent> = _events.asSharedFlow()
    
    // Load content reactively
    private fun loadRootContent() {
        viewModelScope.launch {
            fileBrowserRepository.getRootContent()
                .collect { result ->
                    when (result) {
                        is Result.Success -> updateState { 
                            copy(contentState = UiState.Success(result.data))
                        }
                        is Result.Error -> updateState {
                            copy(contentState = UiState.Error(result.exception.message))
                        }
                    }
                }
        }
    }
}
```

#### 3. Repository Implementation
```kotlin
// In RealDebridFileBrowserRepository
class RealDebridFileBrowserRepository @Inject constructor(
    private val apiService: RealDebridApiService,
    private val cacheManager: FileBrowserCacheManager
) : FileBrowserRepository {
    
    override fun getRootContent(): Flow<Result<List<FileItem>>> = flow {
        emit(Result.Loading)
        
        try {
            // Check cache first
            val cachedContent = cacheManager.getCachedContent("root")
            if (cachedContent != null) {
                emit(Result.Success(cachedContent))
            }
            
            // Fetch from API
            val torrents = apiService.getTorrents()
            val fileItems = torrents.map { it.toFileItem() }
            
            // Cache the result
            cacheManager.cacheContent("root", fileItems)
            emit(Result.Success(fileItems))
            
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
```

### Advanced Features

#### 1. Bulk Operations with Progress Tracking
```kotlin
// Multi-select with real-time progress
fun downloadSelectedFiles() {
    val selectedIds = uiState.value.selectedItems
    if (selectedIds.isEmpty()) return
    
    launchSafely {
        fileBrowserRepository.bulkDownloadFiles(selectedIds) { fileId, progress ->
            // Update progress in real-time
            updateState { 
                copy(bulkOperationState = BulkOperationState(
                    type = BulkOperationType.DOWNLOAD,
                    progress = progress,
                    currentItem = fileId
                ))
            }
        }
    }
}
```

#### 2. TV-Optimized UI Components
```kotlin
// Custom TV focus indicator
@Composable
fun TVFocusIndicator(
    isFocused: Boolean,
    content: @Composable () -> Unit
) {
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(if (isFocused) 2.dp else 4.dp)
    ) {
        content()
    }
}

// File item with TV-specific interactions
@Composable
fun SelectableFileItem(
    item: FileItem,
    isSelected: Boolean,
    onSelect: (FileItem) -> Unit,
    onLongPress: (FileItem) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    TVFocusIndicator(isFocused = isFocused) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusable(
                    onFocusChanged = { isFocused = it.isFocused },
                    onLongPress = { onLongPress(item) }
                )
                .clickable { onSelect(item) },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            // File item content with icon, name, size, status
        }
    }
}
```

### Testing Patterns

#### 1. ViewModel Testing
```kotlin
@Test
fun `when loading content succeeds, should update state correctly`() = runTest {
    // Given
    val mockFileItems = listOf(createMockFileItem())
    every { mockRepository.getRootContent() } returns flowOf(Result.Success(mockFileItems))
    
    // When
    val viewModel = AccountFileBrowserViewModel(mockRepository)
    
    // Then
    verify { mockRepository.getRootContent() }
    assertEquals(UiState.Success(mockFileItems), viewModel.uiState.value.contentState)
}
```

#### 2. Repository Testing
```kotlin
@Test
fun `when API call succeeds, should cache and return results`() = runTest {
    // Given
    val mockTorrents = listOf(createMockTorrent())
    coEvery { mockApiService.getTorrents() } returns mockTorrents
    
    // When
    val result = repository.getRootContent().first()
    
    // Then
    assertTrue(result is Result.Success)
    verify { mockCacheManager.cacheContent("root", any()) }
}
```

## TMDb Integration Architecture

### Overview

The TMDb (The Movie Database) integration provides comprehensive movie and TV show metadata to enhance the content experience. The system implements an offline-first approach with intelligent caching and seamless ContentDetail integration.

### TMDb Service Layer Structure

```
network/
├── api/
│   ├── TMDbMovieService.kt         # Movie-specific endpoints
│   ├── TMDbTVService.kt            # TV show-specific endpoints
│   └── TMDbSearchService.kt        # Search and discovery endpoints
├── models/tmdb/
│   ├── TMDbMovieResponse.kt        # Movie data structures
│   ├── TMDbTVResponse.kt           # TV show data structures
│   ├── TMDbCreditsResponse.kt      # Cast and crew data
│   ├── TMDbSearchResponse.kt       # Search results
│   └── [additional response models]
└── interceptors/
    └── TMDbApiKeyInterceptor.kt    # API key injection
```

### Repository Pattern Implementation

The TMDb integration follows the NetworkBoundResource pattern for offline-first data access:

```kotlin
// Repository interface with caching support
interface TMDbMovieRepository {
    fun getMovieDetails(movieId: Int, forceRefresh: Boolean = false): Flow<Result<TMDbMovieResponse>>
    fun getMovieContentDetail(movieId: Int): Flow<Result<ContentDetail>>
    fun getMovieCredits(movieId: Int): Flow<Result<TMDbCreditsResponse>>
    fun getMovieRecommendations(movieId: Int, page: Int = 1): Flow<Result<TMDbRecommendationsResponse>>
    // ... additional methods
}

// Implementation with NetworkBoundResource
class TMDbMovieRepositoryImpl @Inject constructor(
    private val tmdbMovieService: TMDbMovieService,
    private val tmdbMovieDao: TMDbMovieDao,
    private val tmdbSearchDao: TMDbSearchDao,
    private val contentDetailMapper: TMDbToContentDetailMapper
) : TMDbMovieRepository {
    
    override fun getMovieDetails(movieId: Int, forceRefresh: Boolean): Flow<Result<TMDbMovieResponse>> = 
        networkBoundResource(
            loadFromDb = { tmdbMovieDao.getMovieById(movieId) },
            shouldFetch = { cached -> forceRefresh || shouldRefreshCache(cached) },
            createCall = { tmdbMovieService.getMovieDetails(movieId) },
            saveCallResult = { response -> tmdbMovieDao.insertMovie(response.toEntity()) }
        )
}
```

### Database Schema

TMDb data is stored in Room entities with comprehensive caching support:

```kotlin
@Entity(tableName = "tmdb_movies")
data class TMDbMovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String?,
    val releaseDate: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Float,
    val voteCount: Int,
    val genres: List<String>?,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "tmdb_credits")
data class TMDbCreditsEntity(
    @PrimaryKey val id: String, // contentId + contentType
    val contentId: Int,
    val contentType: String,
    val cast: List<TMDbCastMemberEntity>,
    val crew: List<TMDbCrewMemberEntity>,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "tmdb_search_results")
data class TMDbSearchResultEntity(
    @PrimaryKey val id: String, // query + page + type
    val query: String,
    val page: Int,
    val results: List<TMDbSearchItemEntity>,
    val lastUpdated: Long = System.currentTimeMillis()
)
```

### ContentDetail Integration

TMDb data seamlessly integrates with the ContentDetail system through specialized implementations:

```kotlin
// TMDb-specific ContentDetail implementation
data class TMDbMovieContentDetail(
    private val tmdbMovie: TMDbMovieResponse,
    private val credits: TMDbCreditsResponse? = null,
    private val progress: ContentProgress = ContentProgress()
) : ContentDetail {
    
    override val id: String = tmdbMovie.id.toString()
    override val title: String = tmdbMovie.title
    override val description: String? = tmdbMovie.overview
    override val backgroundImageUrl: String? = getBackdropUrl(tmdbMovie.backdropPath)
    override val cardImageUrl: String? = getPosterUrl(tmdbMovie.posterPath)
    override val contentType: ContentType = ContentType.MOVIE
    
    override val metadata: ContentMetadata = ContentMetadata(
        year = formatYear(tmdbMovie.releaseDate),
        duration = formatRuntime(tmdbMovie.runtime),
        rating = formatRating(tmdbMovie.voteAverage),
        genre = tmdbMovie.genres.map { it.name },
        cast = credits?.cast?.take(5)?.map { it.name } ?: emptyList(),
        director = credits?.crew?.firstOrNull { it.job == "Director" }?.name
    )
    
    override val actions: List<ContentAction> = createContentActions()
}
```

### Caching Strategy

The TMDb integration implements a sophisticated caching strategy optimized for TV usage:

#### Cache Timelines
- **Movie Details**: 24 hours
- **Credits**: 24 hours  
- **Recommendations**: 24 hours
- **Search Results**: 30 minutes (frequently changing)
- **Popular/Trending**: 1 hour (frequently changing)

#### Cache Types
- **Primary Cache**: Movie and TV show details
- **Secondary Cache**: Credits, images, videos
- **Ephemeral Cache**: Search results and trending content
- **Pagination Cache**: Independent caching per page

#### Cache Management
```kotlin
// Cache expiration logic
private fun shouldRefreshCache(contentId: Int, type: String): Boolean {
    val lastUpdated = when (type) {
        "movie" -> tmdbMovieDao.getMovieLastUpdated(contentId)
        "credits" -> tmdbSearchDao.getCreditsLastUpdated(contentId)
        else -> null
    }
    return lastUpdated?.let { 
        System.currentTimeMillis() - it > CACHE_TIMEOUT_MS 
    } ?: true
}

// Cache clearing methods
suspend fun clearCache() // Clear all TMDb cached data
suspend fun clearMovieCache(movieId: Int) // Clear specific movie cache
```

### API Configuration

TMDb API integration is configured through dedicated DI modules:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TMDbModule {
    
    @Provides
    @Singleton
    @TMDbApi
    fun provideTMDbOkHttpClient(
        tmdbApiKeyInterceptor: TMDbApiKeyInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(tmdbApiKeyInterceptor)
            .cache(Cache(cacheDir, 20 * 1024 * 1024L)) // 20MB cache
            .build()
    }
    
    @Provides
    @Singleton
    fun provideTMDbMovieService(@TMDbApi retrofit: Retrofit): TMDbMovieService {
        return retrofit.create(TMDbMovieService::class.java)
    }
}
```

### API Key Management

TMDb API keys are securely managed through:

```kotlin
class TMDbApiKeyInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
            .build()
        
        val request = original.newBuilder()
            .url(url)
            .build()
            
        return chain.proceed(request)
    }
}
```

### Data Flow: API → Repository → ContentDetail

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   Movie Detail  │  │   Search Screen │  │ Content Browser │ │
│  │     Screen      │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────┬───────────────────────────────────────┘
                          │ ContentDetail
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Repository Layer                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │TMDbMovieRepository│  │ TMDbTVRepository│  │TMDbSearchRepository│ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────┬───────────────────────────────────────┘
                          │ NetworkBoundResource
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Database Layer                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ TMDbMovieEntity │  │ TMDbCreditsEntity│  │TMDbSearchEntity │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────┬───────────────────────────────────────┘
                          │ Room DAOs
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Network Layer                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │TMDbMovieService │  │  TMDbTVService  │  │TMDbSearchService│ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────┬───────────────────────────────────────┘
                          │ Retrofit + OkHttp
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      TMDb API v3                               │
│            (Movies, TV Shows, Search, Credits)                 │
└─────────────────────────────────────────────────────────────────┘
```

### Error Handling and Resilience

The TMDb integration implements comprehensive error handling:

```kotlin
// Repository error handling
networkBoundResource(
    loadFromDb = { /* Cache lookup */ },
    shouldFetch = { /* Cache validation */ },
    createCall = { /* API call */ },
    saveCallResult = { /* Cache update */ },
    onFetchFailed = { throwable ->
        // Log error, handle rate limiting, fallback to cache
        when (throwable) {
            is HttpException -> handleHttpError(throwable)
            is IOException -> handleNetworkError(throwable)
            else -> handleUnknownError(throwable)
        }
    }
)

// API response handling
fun <T> handleApiResponse(response: Response<ApiResponse<T>>): ApiResponse<T> {
    return if (response.isSuccessful) {
        response.body() ?: ApiResponse.Error(Exception("Empty response"))
    } else {
        ApiResponse.Error(Exception("HTTP ${response.code()}: ${response.message()}"))
    }
}
```

### Testing Strategy

The TMDb integration includes comprehensive testing coverage:

#### Unit Tests
- **Repository Tests**: NetworkBoundResource behavior, caching logic
- **Service Tests**: API endpoint mapping, response parsing
- **Mapper Tests**: ContentDetail transformation accuracy
- **Entity Tests**: Database schema validation

#### Integration Tests
- **Database Tests**: Room entity relationships, query optimization
- **Network Tests**: API integration, error scenarios
- **Cache Tests**: Expiration policies, cache invalidation
- **DI Tests**: Module configuration validation

#### Test Coverage Areas
- Cache hit/miss scenarios
- API error handling
- Data transformation accuracy
- Performance benchmarks
- Network failure resilience

### Performance Optimization

The TMDb integration is optimized for TV performance:

#### Network Optimization
- **Connection Pooling**: Shared OkHttp client
- **Request Deduplication**: Avoid duplicate concurrent requests
- **Compression**: Gzip/Brotli support
- **Timeout Configuration**: Optimized for TV networks

#### Memory Management
- **Image Caching**: Glide integration with TMDb image URLs
- **Data Pagination**: Efficient large dataset handling
- **Cache Size Limits**: Configurable cache boundaries
- **Background Processing**: Non-blocking data updates

#### TV-Specific Optimizations
- **Preloading**: Related content prefetching
- **Image Sizes**: TV-optimized image dimensions
- **Focus Performance**: Smooth D-pad navigation
- **Offline Graceful Degradation**: Cached content fallback

### Usage Examples

#### Basic Movie Details
```kotlin
// ViewModel usage
class MovieDetailViewModel @Inject constructor(
    private val tmdbMovieRepository: TMDbMovieRepository
) : ViewModel() {
    
    fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {
            tmdbMovieRepository.getMovieContentDetail(movieId)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val contentDetail = result.data
                            // Update UI with ContentDetail
                        }
                        is Result.Error -> {
                            // Handle error
                        }
                        is Result.Loading -> {
                            // Show loading indicator
                        }
                    }
                }
        }
    }
}
```

#### Search Integration
```kotlin
// Search with caching
fun searchMovies(query: String) {
    viewModelScope.launch {
        tmdbSearchRepository.searchMovies(query)
            .collect { result ->
                when (result) {
                    is Result.Success -> {
                        val searchResults = result.data.results
                        // Convert to ContentDetail list
                        val contentDetails = searchResults.map { searchResult ->
                            contentDetailMapper.mapSearchResultToContentDetail(searchResult)
                        }
                        // Update UI
                    }
                    // Handle other states
                }
            }
    }
}
```

#### ContentDetail Integration
```kotlin
// ContentDetail screen usage
@Composable
fun ContentDetailScreen(
    contentDetail: ContentDetail,
    onActionClick: (ContentAction) -> Unit
) {
    when (contentDetail) {
        is TMDbMovieContentDetail -> {
            // Access TMDb-specific data
            val tmdbMovie = contentDetail.getTMDbMovie()
            val credits = contentDetail.getCredits()
            
            // Display TMDb-enhanced content
            ContentDetailLayout(
                contentDetail = contentDetail,
                onActionClick = onActionClick,
                customSections = listOf(
                    "cast" to { CastSection(credits?.cast ?: emptyList()) },
                    "crew" to { CrewSection(credits?.crew ?: emptyList()) }
                )
            )
        }
        else -> {
            // Standard ContentDetail rendering
            ContentDetailLayout(contentDetail, onActionClick)
        }
    }
}
```

## Future Development Roadmap

### Completed Features

1. **AccountFileBrowser System**: Complete file browser with TV-optimized UI
2. **MVVM Architecture**: Implemented with reactive state management
3. **Dependency Injection**: Hilt integration with modular architecture
4. **Repository Pattern**: Abstract data access with caching support
5. **Navigation**: Type-safe Compose Navigation integration
6. **TMDb Integration**: Complete movie/TV metadata service with offline-first caching
7. **ContentDetail System**: Unified content representation with TMDb enhancement

### Immediate Opportunities

1. **Testing Infrastructure**: Expand unit and integration test coverage
2. **Multi-Account Support**: Support for Premiumize, AllDebrid
3. **Offline Mode**: Enhanced caching and offline file access
4. **Performance Optimization**: Image preloading and list virtualization
5. **TMDb Discovery**: Implement trending, discovery, and personalized recommendations
6. **Content Matching**: Smart matching between file names and TMDb content

### Architectural Improvements

1. **TV Compose Migration**: Full migration from Leanback when TV Material is stable
2. **Advanced Caching**: Implement multi-layer caching with expiration policies
3. **Background Sync**: WorkManager integration for background downloads
4. **Analytics**: Usage tracking and performance monitoring
5. **TMDb Sync**: Background synchronization of popular content metadata
6. **Content Enrichment**: Enhanced metadata with reviews, ratings, and recommendations

## Maintenance Notes

*This file is automatically maintained by Claude Code. When making architectural changes:*

1. *Update dependency versions in this file*
2. *Document new patterns and conventions*  
3. *Update directory structure as needed*
4. *Keep roadmap current with project priorities*
5. *Update AccountFileBrowser documentation when adding new features*

### AccountFileBrowser Maintenance

When modifying the AccountFileBrowser system:

1. **Screen Layer**: Update UI patterns and focus management documentation
2. **ViewModel Layer**: Document new state properties and event types
3. **Repository Layer**: Update API integration patterns and caching strategies
4. **Model Layer**: Document new data models and transformation patterns
5. **Testing**: Update testing examples with new features

### TMDb Integration Maintenance

When modifying the TMDb integration:

1. **Service Layer**: Update API endpoint documentation and response models
2. **Repository Layer**: Document new caching strategies and data access patterns
3. **Database Layer**: Update entity schemas and migration documentation
4. **Mapper Layer**: Document ContentDetail transformation patterns
5. **Testing**: Update test coverage and integration examples
6. **Performance**: Update optimization strategies and benchmarks

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](CLAUDE.md), [claude-development.md](claude-development.md)