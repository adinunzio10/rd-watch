# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RD Watch** is an Android TV application built with Jetpack Compose and Android Leanback library. The app is designed for Android TV devices with remote control navigation and 10-foot UI experience.

## Development Commands

### Build & Run

```bash
# Build the project
./gradlew build

# Install debug build to connected Android TV device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# Lint check
./gradlew lint
```

### Testing

```bash
# Currently no tests configured - this is a development opportunity
# Standard Android test commands would be:
# ./gradlew test              # Unit tests
# ./gradlew connectedCheck    # Instrumentation tests
```

### Dependency Management

```bash
# Check for dependency updates
./gradlew dependencyUpdates

# View dependency tree
./gradlew dependencies
```

## Architecture & Code Structure

### Application Structure

- **Package**: `com.rdwatch.androidtv`
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 35 (Android 15)
- **Build Tool**: Gradle with Kotlin DSL

### Key Directories

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

## Key Configuration Files

### Build Configuration

- **app/build.gradle.kts**: Main app module build configuration
- **gradle/libs.versions.toml**: Version catalog for all dependencies
- **settings.gradle.kts**: Project structure and repositories

### Android Configuration

- **AndroidManifest.xml**: TV-specific permissions and intent filters
- **proguard-rules.pro**: Code obfuscation rules (currently default)

### Development Tools

- **package.json**: Task Master AI integration for project management. Instructions are found in [AGENTS](AGENTS.md)
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

Current: Static data in `MovieList.kt`
Future considerations:

- Repository pattern for data access
- ViewModels for state management
- Retrofit for API communication
- Room for local caching

### Testing Strategy (Not Yet Implemented)

Recommended test structure:

- **Unit Tests**: Data models, business logic
- **Compose Tests**: UI component testing with focus simulation
- **Integration Tests**: Navigation flows, D-pad interaction
- **TV-Specific Tests**: Leanback compatibility, remote control simulation

## Common Development Tasks

### Adding Video Content

1. Update `Movie.kt` data model if needed
2. Add content to `MovieList.kt` or implement dynamic data source
3. Update UI components to handle new content types
4. Test with focus navigation

### Modifying UI Components

1. Locate Compose components in `MainActivity.kt`
2. Follow Material3 design guidelines
3. Ensure TV accessibility (focus, sizing, contrast)
4. Test on actual TV device or emulator

### Theme Customization

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

## Debugging & Development Tips

### Android TV Emulator Setup

1. Create Android TV emulator with API 21+
2. Enable developer options
3. Use ADB for debugging: `adb connect <tv-ip>:5555`

### Common Issues

- **Focus Problems**: Use layout inspector to debug focus traversal
- **Overscan**: Test on actual TV hardware, not just emulator
- **Performance**: Profile on lower-end TV devices
- **Remote Control**: Test all D-pad directions and buttons

### Build Troubleshooting

- **Lint Errors**: Currently set to non-blocking (`abortOnError = false`)
- **Compose Issues**: Check Compose compiler version compatibility
- **TV-Specific**: Verify leanback feature requirements in manifest

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

## Task Master AI Integration

This project includes Task Master AI for development workflow management. Key commands:

```bash
# View current tasks
task-master list

# Get next task to work on
task-master next

# Mark task complete
task-master set-status --id=<id> --status=done

# Add new development task
task-master add-task --prompt="description"
```

See the MCP integration in `.mcp.json` for Claude Code task management tools.
