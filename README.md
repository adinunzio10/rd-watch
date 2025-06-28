# RD Watch - Android TV Streaming Application

A modern Android TV application built with Jetpack Compose, designed for 10-foot viewing experience with D-pad navigation and optimized for Android TV devices.

## 📺 Features

- **Modern UI**: Built with Jetpack Compose and Material3 design system
- **TV-Optimized**: Proper focus management and D-pad navigation
- **Content Browsing**: Grid-based content discovery with horizontal scrolling categories  
- **Video Streaming**: Integration-ready for video content playback
- **Offline Support**: Room database for local content caching
- **Clean Architecture**: Repository pattern with dependency injection

## 🏗️ Architecture

### Technology Stack

- **UI Framework**: Jetpack Compose with Material3
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room with SQLite
- **Networking**: Retrofit + OkHttp + Moshi
- **Image Loading**: Coil
- **Language**: Kotlin
- **Min SDK**: 23 (Android 6.0)
- **Target SDK**: 35 (Android 15)

### Project Structure

```
app/src/main/java/com/rdwatch/androidtv/
├── MainActivity.kt              # Main Compose UI entry point
├── Movie.kt                     # Data model for video content
├── MovieList.kt                 # Static content provider
├── data/
│   ├── local/                   # Room database components
│   ├── remote/                  # Retrofit API services  
│   └── repository/              # Repository implementations
├── di/                          # Hilt dependency injection modules
├── ui/theme/                    # Material3 theme configuration
└── presentation/               # Compose UI screens and navigation
```

## 🚀 Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK with API level 23+
- Android TV emulator or physical Android TV device
- Java 11+

### Building the Project

```bash
# Clone the repository
git clone [repository-url]
cd rd-watch

# Build the project
./gradlew build

# Install on connected Android TV device/emulator
./gradlew installDebug

# Run lint checks
./gradlew lint

# Clean build
./gradlew clean
```

### Development Setup

1. **Android TV Emulator**:
   - Create an Android TV emulator with API 23+
   - Enable developer options
   - Use ADB for debugging: `adb connect <tv-ip>:5555`

2. **Dependencies**:
   ```bash
   # Check for dependency updates
   ./gradlew dependencyUpdates
   
   # View dependency tree
   ./gradlew dependencies
   ```

## 🎮 Android TV Specific Features

### Focus Management
- Full D-pad navigation support
- Focus-aware UI components with proper traversal
- `FocusRequester` and `focusable()` modifiers for Compose components

### TV Optimizations
- **Orientation**: Locked to landscape for TV viewing
- **Typography**: Large fonts optimized for 10-foot viewing distance
- **Layout**: Overscan-safe design with proper margins
- **Colors**: High contrast for TV displays

### Android TV Integration
- `LEANBACK_LAUNCHER` intent filter for Android TV launcher
- `android.software.leanback` feature requirement
- Internet permissions for streaming content
- Compose-based TV navigation and focus management

## 📱 Current Implementation

### Pure Compose UI
- **MainActivity.kt**: Modern Jetpack Compose implementation
- Material3 design system with TV-specific adaptations
- Complete navigation system with Compose Navigation
- Proper focus management and D-pad navigation
- Horizontal scrolling content categories
- Multiple screens: Home, Browse, Details, Search, Settings, Profile

*Note: This app is built entirely with Jetpack Compose - no legacy Leanback components.*

## 🔧 Development Commands

### Build & Run
```bash
./gradlew build                 # Build project
./gradlew installDebug          # Install debug build
./gradlew clean                 # Clean build
./gradlew lint                  # Run lint checks
```

### Git & GitHub CLI
```bash
git status                      # Check repository status
gh repo view                    # View repository info
gh issue list                   # List open issues
gh pr list                      # List pull requests
```

### Task Management
This project integrates with Task Master AI for development workflow:

```bash
# View current tasks
task-master list

# Get next task to work on  
task-master next

# Mark task complete
task-master set-status --id=<id> --status=done
```

## 🧪 Testing

*Testing infrastructure is planned but not yet implemented.*

Recommended test structure:
- **Unit Tests**: Data models and business logic
- **Compose Tests**: UI component testing with focus simulation
- **Integration Tests**: Navigation flows and D-pad interaction
- **TV-Specific Tests**: Focus navigation and remote control simulation

```bash
# Future test commands
./gradlew test              # Unit tests
./gradlew connectedCheck    # Instrumentation tests
```

## 📊 Content Management

### Current Data Source
- Static content in `MovieList.kt` with Google sample videos
- Ready for dynamic content integration via Repository pattern

### Sample Content
- Zeitgeist 2010 Year in Review
- Google Demo Slam: 20ft Search
- Gmail Blue introduction
- Google Fiber to the Pole
- Google Nose introduction

### Future Integration
- API-driven content loading via Retrofit
- Local caching with Room database
- Dynamic categories and personalization

## 🛠️ Development Guidelines

### Adding New Screens
1. Create Composable functions following TV design patterns
2. Implement proper focus management with `FocusRequester`
3. Use Material3 TV-optimized components
4. Test with D-pad navigation
5. Consider overscan and safe areas

### Code Style
- Follow Android coding conventions
- Use Kotlin idiomatic patterns
- Implement proper dependency injection with Hilt
- Follow Material3 design guidelines for TV

### Common Development Tasks

#### Adding Video Content
1. Update `Movie.kt` data model if needed
2. Modify content source (currently `MovieList.kt`)
3. Update UI components for new content types
4. Test focus navigation

#### Modifying UI Components
1. Locate Compose screens in `presentation/` directory
2. Follow Material3 design guidelines
3. Ensure TV accessibility (focus, sizing, contrast)
4. Test on actual TV device or emulator

## 🚧 Roadmap

### Immediate Opportunities
- [x] Complete migration from Leanback to Compose
- [ ] Implement comprehensive testing infrastructure
- [ ] Add dynamic content loading via APIs
- [ ] Implement proper screen navigation
- [ ] Enhance error handling and offline functionality

### Future Enhancements
- [ ] MVVM pattern with ViewModels
- [ ] User authentication and personalization
- [ ] Content recommendations
- [ ] Offline viewing capabilities
- [ ] Performance optimizations for lower-end devices

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b task/feature-name`)
3. Commit changes (`git commit -m 'Add feature'`)
4. Push to branch (`git push origin task/feature-name`)
5. Create a Pull Request

For detailed development workflow, see [CLAUDE.md](CLAUDE.md).

## 📞 Support

- Create an issue for bug reports or feature requests
- Check existing documentation in [CLAUDE.md](CLAUDE.md)
- Review Android TV development guidelines

---

**Note**: This application is optimized for Android TV devices with remote control navigation. For best experience, test on actual Android TV hardware or properly configured emulators.
