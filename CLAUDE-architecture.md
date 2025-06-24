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
- **Data**: Simple static data provider with sample Google videos

### Directory Structure

```
app/src/main/java/com/rdwatch/androidtv/
├── MainActivity.kt              # Main entry point with Compose UI
├── Movie.kt                     # Data model for video content
├── MovieList.kt                 # Static data provider
├── ui/theme/
│   ├── Theme.kt                # Material3 theme configuration
│   └── Type.kt                 # TV-optimized typography
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

### Future TV-Specific Dependencies

Available when stable (currently alpha):
- **TV Foundation**: 1.0.0-alpha11
- **TV Material**: 1.0.0-alpha11

## Future Development Roadmap

### Immediate Opportunities

1. **Testing Infrastructure**: Add unit and integration tests
2. **Dynamic Content**: Replace static data with API integration
3. **Navigation**: Implement proper fragment/screen navigation
4. **Error Handling**: Improve error states and offline functionality

### Architectural Improvements

1. **MVVM Pattern**: Implement ViewModels and state management
2. **Dependency Injection**: Add Hilt or Koin
3. **Repository Pattern**: Abstract data access layer
4. **TV Compose Migration**: Full migration from Leanback when stable

## Maintenance Notes

*This file is automatically maintained by Claude Code. When making architectural changes:*

1. *Update dependency versions in this file*
2. *Document new patterns and conventions*  
3. *Update directory structure as needed*
4. *Keep roadmap current with project priorities*

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](CLAUDE.md), [claude-development.md](claude-development.md)