# Claude Development Documentation

This file contains debugging tips, common development tasks, and troubleshooting procedures for the RD Watch Android TV application. It is automatically maintained by Claude Code as development practices evolve.

## Android TV Emulator Setup

### Creating Android TV Emulator
1. Create Android TV emulator with API 21+
2. Enable developer options on emulator
3. Use ADB for debugging: `adb connect <tv-ip>:5555`

### Device Connection
```bash
# Connect to Android TV device over network
adb connect <tv-ip>:5555

# List connected devices
adb devices

# Install APK to specific device
adb -s <device-id> install app/build/outputs/apk/debug/app-debug.apk
```

## Debugging & Development Tips

### Common Issues and Solutions

#### Focus Problems
- **Issue**: Focus not working correctly or focus indicators not visible
- **Debug**: Use layout inspector to debug focus traversal
- **Solution**: Check `focusRequester` and `onFocusChanged` modifiers in Compose
- **Tools**: Android Studio Layout Inspector, Focus debugging

#### Overscan Issues
- **Issue**: UI elements cut off on TV screen edges
- **Debug**: Test on actual TV hardware, not just emulator
- **Solution**: Account for TV overscan areas in layouts
- **Best Practice**: Add safe area margins to important UI elements

#### Performance Issues
- **Issue**: Slow rendering, laggy animations
- **Debug**: Profile on lower-end TV devices
- **Solution**: Optimize image loading, reduce overdraw
- **Tools**: GPU Profiler, Memory Profiler

#### Remote Control Issues
- **Issue**: D-pad directions or buttons not responding
- **Debug**: Test all D-pad directions and buttons systematically
- **Solution**: Verify focus traversal patterns, check event handling
- **Testing**: Use physical remote, test edge cases

### Build Troubleshooting

#### Lint Errors
- **Current Status**: Set to non-blocking (`abortOnError = false`)
- **Resolution**: Use `./ktlint-summary.sh` for formatting and style checking
- **Best Practice**: Address lint warnings incrementally

#### Compose Issues
- **Common Problem**: Compose compiler version compatibility
- **Check**: Verify Compose BOM and compiler versions match
- **Update**: Keep Compose dependencies synchronized

#### TV-Specific Build Issues
- **Manifest Issues**: Verify leanback feature requirements
- **Permissions**: Ensure internet access permissions for streaming
- **Intent Filters**: Check `LEANBACK_LAUNCHER` category

## Common Development Tasks

### Adding New Video Content

1. **Update Data Model**
   ```kotlin
   // Update Movie.kt if new fields needed
   data class Movie(
       val title: String,
       val description: String,
       val cardImageUrl: String,
       // Add new fields here
   )
   ```

2. **Update Data Provider**
   ```kotlin
   // Add content to MovieList.kt
   val newMovies = listOf(
       Movie("New Title", "Description", "image-url"),
       // Add more movies
   )
   ```

3. **Update UI Components**
   - Modify Compose components in `MainActivity.kt`
   - Handle new content types in UI
   - Test focus navigation with new content

### Modifying UI Components

1. **Locate Components**
   - Main Compose components in `MainActivity.kt`
   - Theme components in `ui/theme/`

2. **Follow Guidelines**
   - Use Material3 design guidelines
   - Ensure TV accessibility (focus, sizing, contrast)
   - Test on actual TV device or emulator

3. **Testing Checklist**
   - D-pad navigation works correctly
   - Focus indicators are visible
   - Content fits within safe areas
   - Performance is acceptable

### Theme Customization

1. **Color Modifications**
   ```kotlin
   // Modify ui/theme/Theme.kt
   private val DarkColorScheme = darkColorScheme(
       primary = Color(0xFF...), // Update colors
       // Other theme colors
   )
   ```

2. **Typography Updates**
   ```kotlin
   // Modify ui/theme/Type.kt
   val Typography = Typography(
       displayLarge = TextStyle(
           fontSize = 57.sp, // TV-optimized sizes
           // Other text styles
       )
   )
   ```

3. **Testing Requirements**
   - Ensure sufficient contrast for TV viewing
   - Test in both light and dark themes
   - Verify readability at 10-foot distance

## Debugging Commands

### ADB Debugging
```bash
# View logs
adb logcat | grep "RDWatch"

# Clear logs
adb logcat -c

# Install and run
adb install -r app-debug.apk
adb shell am start -n com.rdwatch.androidtv/.MainActivity

# Screen capture
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png
```

### Gradle Debugging
```bash
# Debug build with verbose output
./gradlew build --info

# Debug specific task
./gradlew :app:assembleDebug --debug

# Profile build performance
./gradlew build --profile

# Dependency resolution issues
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

### Compose Debugging
```bash
# Enable Compose compiler metrics
./gradlew assembleDebug -Pandroidx.compose.compiler.metricsDestination=metrics/

# Compose compiler reports
./gradlew assembleDebug -Pandroidx.compose.compiler.reportsDestination=reports/
```

## Performance Optimization

### Image Loading Optimization
```kotlin
// Glide optimization for TV
Glide.with(context)
    .load(imageUrl)
    .override(Target.SIZE_ORIGINAL) // TV-appropriate sizing
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .into(imageView)
```

### Memory Management
```bash
# Monitor memory usage
adb shell dumpsys meminfo com.rdwatch.androidtv

# Force garbage collection
adb shell am force-stop com.rdwatch.androidtv
```

### Focus Performance
- Minimize focus chain complexity
- Use `focusGroup` for logical groupings
- Optimize focus animations

## Development Environment

### Android Studio Configuration
1. **Enable TV Preview**: Use TV-specific preview templates
2. **Remote Debugging**: Configure ADB over Wi-Fi
3. **Layout Inspector**: Essential for focus debugging
4. **Profiling**: CPU, Memory, and GPU profilers

### Recommended Plugins
- **Jetpack Compose Preview**: Real-time UI preview
- **ADB Idea**: ADB commands from IDE
- **TV Development**: Android TV specific tools

## Troubleshooting Checklist

### Build Issues
- [ ] Clean and rebuild project
- [ ] Check Gradle version compatibility
- [ ] Verify Android SDK version
- [ ] Update dependencies to latest stable versions

### Runtime Issues  
- [ ] Check device compatibility
- [ ] Verify network connectivity
- [ ] Test on different screen sizes
- [ ] Validate focus navigation

### Performance Issues
- [ ] Profile memory usage
- [ ] Check image loading performance
- [ ] Optimize layout complexity
- [ ] Test on lower-end devices

## Maintenance Notes

*This file is automatically maintained by Claude Code. When discovering new debugging techniques or common issues:*

1. *Document new debugging procedures*
2. *Update troubleshooting checklists*  
3. *Add performance optimization tips*
4. *Record solutions to recurring problems*
5. *Keep development environment setup current*

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](CLAUDE.md), [CLAUDE-architecture.md](CLAUDE-architecture.md), [claude-tests.md](claude-tests.md)