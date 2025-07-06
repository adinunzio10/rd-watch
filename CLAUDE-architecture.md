# CLAUDE Architecture Documentation

This file contains detailed architecture information for the RD Watch Android TV application. It is automatically maintained by Claude Code as the codebase evolves.

## Application Structure

### Technology Stack

- **UI Framework**: Jetpack Compose with Material3
- **TV Framework**: Android Leanback (transitioning from)
- **Image Loading**: Glide 4.11.0
- **Navigation**: Focus-based D-pad navigation
- **Language**: Kotlin with Java 11 compatibility

### Current Architecture State

The app is in **transition** from traditional Android Leanback framework to modern Jetpack Compose:

- **New Code**: Uses Compose with Material3 (MainActivity, Theme, Type)
- **Legacy Code**: Traditional Leanback activities still present
- **Data**: Comprehensive data layer with Real Debrid API integration, Room database, local caching, and account file management

### Directory Structure

```
app/src/main/java/com/rdwatch/androidtv/
├── MainActivity.kt              # Main entry point with Compose UI
├── Movie.kt                     # Data model for video content
├── MovieList.kt                 # Static data provider
├── auth/                        # OAuth2 authentication system
│   ├── AuthManager.kt          # Authentication flow management
│   ├── AuthRepository.kt       # Authentication data access
│   └── ui/AuthenticationScreen.kt # Authentication UI
├── data/                        # Data layer with Room database
│   ├── AppDatabase.kt          # Room database configuration
│   ├── entities/               # Database entities
│   ├── dao/                    # Data access objects
│   └── repository/             # Repository implementations
├── network/                     # Network layer
│   ├── api/RealDebridApiService.kt # Real Debrid API client
│   ├── interceptors/           # Network interceptors
│   └── models/                 # API response models
├── ui/                          # UI layer
│   ├── browse/                 # Content browsing and account files
│   │   ├── BrowseScreen.kt     # Content browser UI
│   │   ├── AccountFileBrowserScreen.kt # Account file browser UI
│   │   └── BrowseViewModel.kt  # Browser state management
│   ├── home/HomeScreen.kt      # Main home screen with navigation
│   ├── search/SearchScreen.kt  # Global search with file integration
│   └── theme/                  # Material3 theme configuration
├── di/                          # Dependency injection modules
├── BrowseErrorActivity.kt       # Legacy leanback error handling
├── DetailsActivity.kt           # Legacy leanback detail view  
├── PlaybackActivity.kt          # Legacy leanback video playback
└── [other legacy leanback files]
```

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

## Feature Architecture

### Account File Browser

The Direct Account File Browser is a comprehensive feature that allows users to browse their Real Debrid account files directly:

#### Key Components:
- **AccountFileBrowserScreen.kt**: Main UI with file list, sorting, and filtering
- **AccountFileBrowserViewModel.kt**: State management with pagination support
- **AccountFileRepository.kt**: Data access with multi-level caching
- **AccountFileEntity.kt**: Database entity for file metadata
- **AccountFilePagingSource.kt**: Efficient pagination for large file collections

#### Performance Features:
- **Multi-level Caching**: Memory, disk, and database caching with TTL
- **Adaptive Loading**: Network-aware page sizes and image quality
- **Progressive Loading**: Incremental file loading with user feedback
- **Memory Optimization**: Object pooling and efficient data structures

#### Integration Points:
- **Global Search**: Account files integrated into search results
- **ExoPlayer**: Direct file playback from account
- **Navigation**: Seamless integration with existing app navigation
- **Settings**: Configurable sorting, filtering, and caching options

## Development Patterns

### Adding New Screens

1. Create Composable functions following TV design patterns
2. Implement proper focus management
3. Use Material3 TV-optimized components
4. Test with D-pad navigation
5. Consider overscan and safe areas

### Data Management

**Architecture**: MVVM pattern with clean architecture principles

**Implemented**:
- Repository pattern for data access with caching strategies
- ViewModels for UI state management with reactive streams
- Use cases for business logic encapsulation
- Multi-level caching (memory, disk, database)
- Hilt dependency injection throughout the application
- Room database with migrations and type converters
- Retrofit for API communication with Real Debrid
- Paging3 for efficient large dataset handling
- OAuth2 authentication flow with token management
- Comprehensive error handling and retry mechanisms

**Real Debrid Integration**:
- Account file browser with direct torrent access
- File filtering, sorting, and bulk operations
- Offline caching with TTL for improved performance
- Authentication handling with automatic token refresh

### Common Development Tasks

#### Working with Real Debrid Account File Browser

1. **Adding New Features**:
   - Extend `BrowseAccountViewModel` for new functionality
   - Update `AccountFile` data model for new properties
   - Add new API endpoints to `RealDebridApiService`
   - Implement repository methods for data access
   - Create UI components following TV design patterns

2. **Modifying File Display**:
   - Update `FileListItem` component for new display requirements
   - Modify filtering logic in `FileFilter` data class
   - Update sorting options in `FileSorting` enum
   - Test with large file lists for performance

3. **API Integration Changes**:
   - Update network models in `models/` directory
   - Modify API service methods
   - Update data mappers for new response formats
   - Handle new error cases in error handling layer

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

### Real Debrid Integration Dependencies

- **Retrofit**: 2.9.0 (API communication)
- **OkHttp**: 4.10.0 (HTTP client with interceptors)
- **Room**: 2.5.0 (Local database and caching)
- **Hilt**: 2.48 (Dependency injection)
- **Gson**: 2.10.1 (JSON serialization)
- **Coroutines**: 1.7.3 (Asynchronous programming)
- **Paging 3**: 3.2.1 (Pagination support)
- **DataStore**: 1.0.0 (Secure token storage)

### Future TV-Specific Dependencies

Available when stable (currently alpha):
- **TV Foundation**: 1.0.0-alpha11
- **TV Material**: 1.0.0-alpha11

## Future Development Roadmap

### Completed Features ✅

1. **MVVM Architecture**: ✅ Complete with ViewModels and reactive state management
2. **Dependency Injection**: ✅ Hilt implementation throughout the application
3. **Repository Pattern**: ✅ Comprehensive data access layer with caching
4. **Real Debrid Integration**: ✅ Complete API integration with authentication
5. **Account File Browser**: ✅ Direct file browsing with pagination and performance optimization
6. **Navigation System**: ✅ Jetpack Navigation with type-safe routing
7. **Database Layer**: ✅ Room database with migrations and comprehensive entities

### Current Development Priorities

1. **Enhanced Testing**: Expand unit, integration, and UI test coverage
2. **Performance Optimization**: Continued optimization for large file collections
3. **Feature Enhancement**: Advanced file operations and preview capabilities
4. **TV UX Improvements**: Enhanced D-pad navigation and TV-specific interactions

### Future Enhancements

1. **TV Compose Migration**: Full migration from Leanback when TV Compose reaches stable
2. **Advanced Features**: Subtitle management, download queue, offline playback
3. **AI/ML Integration**: Smart content recommendations and categorization
4. **Multi-Device Sync**: Cross-device content synchronization and progress tracking

## Maintenance Notes

*This file is automatically maintained by Claude Code. When making architectural changes:*

1. *Update dependency versions in this file*
2. *Document new patterns and conventions*  
3. *Update directory structure as needed*
4. *Keep roadmap current with project priorities*

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](CLAUDE.md), [claude-development.md](claude-development.md)