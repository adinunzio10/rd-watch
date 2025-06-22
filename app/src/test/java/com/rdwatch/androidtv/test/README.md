# Hilt Testing Infrastructure

This directory contains the testing infrastructure for Hilt dependency injection in the RD Watch Android TV application.

## Test Structure

### Base Test Classes

- **`HiltTestBase`**: Base class for unit tests with Hilt DI
- **`HiltInstrumentedTestBase`**: Base class for instrumented (UI) tests with Hilt DI

### Test Modules

Test modules replace production modules during testing:

- **`TestNetworkModule`**: Replaces `NetworkModule` with mock API services and test-friendly network configurations
- **`TestDatabaseModule`**: Replaces `DatabaseModule` with in-memory database for fast testing
- **`TestRepositoryModule`**: Replaces `RepositoryModule` with mock repository implementations
- **`TestFakeRepositoryModule`**: Alternative to `TestRepositoryModule` using fake implementations instead of mocks

### Fake Implementations

- **`FakeMovieRepository`**: Provides predictable test data without network/database dependencies

### Test Utilities

- **`MainDispatcherRule`**: JUnit rule for setting up coroutine dispatchers in tests
- **`TestConfiguration`**: Common test constants and configurations
- **`HiltTestRunner`**: Custom test runner for instrumented tests

## Usage Examples

### Unit Test with Hilt

```kotlin
@HiltAndroidTest
class MovieRepositoryTest : HiltTestBase() {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @Inject
    lateinit var repository: MovieRepository
    
    @Test
    fun testGetMovies() = runTest {
        // Test implementation
        val result = repository.getMovies()
        assertTrue(result.isSuccess)
    }
}
```

### Instrumented Test with Hilt

```kotlin
@HiltAndroidTest
class MainActivityTest : HiltInstrumentedTestBase() {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Inject
    lateinit var repository: MovieRepository
    
    @Test
    fun testMovieDisplay() {
        // Test UI with injected dependencies
        composeTestRule.setContent {
            // Your Compose UI
        }
        
        // Perform UI tests
    }
}
```

### Using Fake Repository

To use fake repository instead of mocks, exclude the `TestRepositoryModule` and include `TestFakeRepositoryModule`:

```kotlin
@UninstallModules(TestRepositoryModule::class)
@InstallModule(TestFakeRepositoryModule::class)
@HiltAndroidTest
class MovieRepositoryFakeTest : HiltTestBase() {
    
    @Inject
    lateinit var repository: MovieRepository
    
    @Inject
    lateinit var fakeRepository: FakeMovieRepository
    
    @Test
    fun testWithFakeData() = runTest {
        // Configure fake repository
        fakeRepository.setReturnError(false)
        
        // Test with predictable fake data
        val result = repository.getMovies()
        assertEquals(2, result.getOrNull()?.size)
    }
}
```

## Test Module Replacement

### Automatic Replacement with @TestInstallIn

Test modules automatically replace production modules:

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]  // Replaces this module in tests
)
object TestNetworkModule {
    // Test implementations
}
```

### Manual Module Management

For more control, use `@UninstallModules` and `@InstallModule`:

```kotlin
@UninstallModules(NetworkModule::class)
@HiltAndroidTest
class CustomNetworkTest : HiltTestBase() {
    // Custom test setup
}
```

## Best Practices

### Unit Tests
- Use `TestRepositoryModule` with mocks for isolated testing
- Use `MainDispatcherRule` for coroutine testing
- Verify dependencies are injected correctly
- Test business logic without UI dependencies

### Integration Tests
- Use `TestDatabaseModule` with in-memory database
- Use `TestNetworkModule` with mock or local server
- Test data flow between layers
- Verify dependency injection works end-to-end

### UI Tests
- Use `HiltInstrumentedTestBase` for Compose UI tests
- Inject repositories to control test data
- Use `TestFakeRepositoryModule` for predictable UI states
- Test user interactions with known data sets

## Common Issues

### Circular Dependencies
Test modules can help identify circular dependencies. If tests fail with circular dependency errors:
1. Check `@Provides` vs `@Binds` usage
2. Verify scope annotations are correct
3. Use qualifiers to distinguish similar dependencies

### Module Conflicts
If you see "multiple modules provide the same binding" errors:
1. Ensure `@TestInstallIn` replaces the correct module
2. Use `@UninstallModules` to explicitly remove conflicting modules
3. Check that test modules don't conflict with each other

### Context Issues
For tests requiring Android context:
1. Use `ApplicationProvider.getApplicationContext()` in test modules
2. Use `@ApplicationContext` qualifier in production code
3. Prefer instrumented tests for context-dependent features

## Running Tests

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Specific Test Classes
```bash
./gradlew test --tests="*MovieRepositoryTest*"
./gradlew connectedAndroidTest --tests="*MainActivityTest*"
```