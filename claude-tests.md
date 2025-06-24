# Claude Testing Documentation

This file contains testing strategy, commands, and maintenance procedures for the RD Watch Android TV application. It is automatically maintained by Claude Code as tests are added or modified.

## Current Testing Status

**Status**: No tests currently configured - this represents a development opportunity

**Target Test Coverage**: 
- Unit Tests: 80%+ for business logic and data models
- Integration Tests: Key user flows and navigation
- UI Tests: Focus management and TV-specific interactions

## Testing Strategy

### Unit Tests
- **Target**: Data models, business logic, utility functions
- **Framework**: JUnit 5 with Mockito
- **Location**: `app/src/test/java/com/rdwatch/androidtv/`

### Integration Tests  
- **Target**: Navigation flows, D-pad interaction, data flow
- **Framework**: Android Instrumentation Tests
- **Location**: `app/src/androidTest/java/com/rdwatch/androidtv/`

### Compose UI Tests
- **Target**: UI component testing with focus simulation
- **Framework**: Compose Testing API
- **Considerations**: TV-specific focus behavior, D-pad navigation

### TV-Specific Tests
- **Target**: Leanback compatibility, remote control simulation
- **Framework**: Espresso with TV extensions
- **Considerations**: 10-foot UI, overscan, focus traversal

## Standard Android Test Commands

```bash
# Unit tests (when configured)
./gradlew test

# Integration tests (when configured)  
./gradlew connectedCheck

# Test specific module
./gradlew app:test

# Test with coverage
./gradlew test jacocoTestReport

# Run tests on specific device
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rdwatch.androidtv.SpecificTest
```

## Test Organization

### Recommended Directory Structure
```
app/src/test/java/com/rdwatch/androidtv/
├── data/
│   ├── MovieTest.kt                 # Data model tests
│   └── MovieListTest.kt             # Data provider tests
├── ui/
│   ├── MainActivityTest.kt          # Main UI tests
│   └── theme/
│       ├── ThemeTest.kt             # Theme configuration tests
│       └── TypeTest.kt              # Typography tests
└── utils/                           # Utility function tests

app/src/androidTest/java/com/rdwatch/androidtv/
├── integration/
│   ├── NavigationFlowTest.kt        # Navigation integration tests
│   └── FocusManagementTest.kt       # Focus behavior tests
├── ui/
│   ├── ComposeUITest.kt             # Compose UI integration tests
│   └── TVSpecificTest.kt            # TV-specific UI tests
└── leanback/                        # Legacy leanback tests
```

## Test Development Guidelines

### Writing Unit Tests

```kotlin
// Example unit test structure
@Test
fun `movie data model should validate correctly`() {
    // Given
    val movie = Movie(
        title = "Test Movie",
        description = "Test Description",
        cardImageUrl = "https://example.com/image.jpg"
    )
    
    // When
    val isValid = movie.isValid()
    
    // Then
    assertTrue(isValid)
}
```

### Writing Compose UI Tests

```kotlin
// Example Compose UI test with focus
@Test
fun `movie card should handle focus correctly`() {
    composeTestRule.setContent {
        RDWatchTheme {
            MovieCard(
                movie = sampleMovie,
                onMovieClick = { },
                modifier = Modifier.testTag("movie_card")
            )
        }
    }
    
    // Test focus behavior
    composeTestRule.onNodeWithTag("movie_card")
        .requestFocus()
        .assertIsFocused()
}
```

### Writing TV-Specific Tests

```kotlin
// Example TV-specific test
@Test
fun `should handle dpad navigation correctly`() {
    // Setup TV emulator or device
    // Test D-pad navigation between focusable elements
    // Verify focus traversal follows expected pattern
}
```

## Test Maintenance Rules

### Auto-Maintenance by Claude Code

When Claude Code makes changes to the codebase:

1. **Add New Tests**: When creating new functions, classes, or UI components
2. **Update Existing Tests**: When modifying existing functionality
3. **Document Test Changes**: Update this file with new test patterns or conventions
4. **Verify Test Coverage**: Ensure new code maintains target coverage levels

### Specific Maintenance Triggers

- **New Data Models**: Add corresponding unit tests
- **New UI Components**: Add Compose UI tests with focus testing
- **New Navigation**: Add integration tests for navigation flows
- **New Business Logic**: Add unit tests with edge cases
- **Bug Fixes**: Add regression tests to prevent reoccurrence

## Test Configuration

### Dependencies to Add

```kotlin
// In app/build.gradle.kts
dependencies {
    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    
    // Android testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // Compose testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
    
    // TV-specific testing
    androidTestImplementation("androidx.leanback:leanback-test:1.2.0")
}
```

### Test Runner Configuration

```xml
<!-- In AndroidManifest.xml (test) -->
<instrumentation
    android:name="androidx.test.runner.AndroidJUnitRunner"
    android:targetPackage="com.rdwatch.androidtv" />
```

## TV Testing Considerations

### Focus Testing
- Test focus traversal in all directions (up, down, left, right)
- Verify focus indicators are visible and clear
- Test focus behavior with different screen sizes

### D-Pad Simulation
- Test all D-pad directions and center button
- Verify back button behavior
- Test menu and home button interactions

### Performance Testing
- Test on lower-end TV devices
- Verify smooth scrolling and transitions
- Test memory usage with large content lists

## Continuous Integration

### GitHub Actions Configuration

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew test
      - name: Run lint
        run: ./gradlew lint
```

## Test Reporting

### Coverage Reports
- Use JaCoCo for coverage reporting
- Target minimum 80% coverage for new code
- Generate HTML reports for review

### Test Results
- Integrate with CI/CD pipeline
- Fail builds on test failures
- Generate test reports for PR reviews

## Maintenance Notes

*This file is automatically maintained by Claude Code. When adding or modifying tests:*

1. *Update test commands and configuration*
2. *Document new testing patterns*
3. *Keep test organization structure current*
4. *Update CI/CD configurations as needed*
5. *Record any TV-specific testing discoveries*

## Current Test Inventory

*No tests currently exist - this section will be auto-updated as tests are added*

**Unit Tests**: 0
**Integration Tests**: 0  
**UI Tests**: 0
**TV-Specific Tests**: 0

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](CLAUDE.md), [CLAUDE-architecture.md](CLAUDE-architecture.md)