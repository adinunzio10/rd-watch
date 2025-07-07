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

## Future Development Roadmap

### Completed Features

1. **AccountFileBrowser System**: Complete file browser with TV-optimized UI
2. **MVVM Architecture**: Implemented with reactive state management
3. **Dependency Injection**: Hilt integration with modular architecture
4. **Repository Pattern**: Abstract data access with caching support
5. **Navigation**: Type-safe Compose Navigation integration

### Immediate Opportunities

1. **Testing Infrastructure**: Expand unit and integration test coverage
2. **Multi-Account Support**: Support for Premiumize, AllDebrid
3. **Offline Mode**: Enhanced caching and offline file access
4. **Performance Optimization**: Image preloading and list virtualization

### Architectural Improvements

1. **TV Compose Migration**: Full migration from Leanback when TV Material is stable
2. **Advanced Caching**: Implement multi-layer caching with expiration policies
3. **Background Sync**: WorkManager integration for background downloads
4. **Analytics**: Usage tracking and performance monitoring

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

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](CLAUDE.md), [claude-development.md](claude-development.md)