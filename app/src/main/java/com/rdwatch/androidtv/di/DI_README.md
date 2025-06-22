# Dependency Injection Architecture

This document outlines the dependency injection structure using Hilt for the RD Watch Android TV application.

## Module Structure

### Core Modules

#### `CoreModule`
- **Scope**: `SingletonComponent`
- **Purpose**: Provides core application dependencies
- **Dependencies**:
  - `DispatcherProvider` - Thread switching abstraction
  - `RetryHandler` - Retry logic for operations
  - `ErrorHandler` - Centralized error handling (via constructor injection)
  - `ErrorMessageProvider` - User-friendly error messages (via constructor injection)

#### `DatabaseModule`
- **Scope**: `SingletonComponent`
- **Purpose**: Provides Room database and DAO instances
- **Dependencies**: All DAO implementations with `@MainDatabase` qualifier

#### `NetworkModule`
- **Scope**: `SingletonComponent`
- **Purpose**: Provides network-related dependencies
- **Dependencies**: Retrofit, OkHttp, API services

#### `RepositoryModule`
- **Scope**: `SingletonComponent`
- **Purpose**: Binds repository interfaces to implementations
- **Dependencies**: 
  - Repository implementations
  - `RepositoryDependencies` wrapper for common dependencies

### Feature Modules

#### `ViewModelModule`
- **Scope**: `ViewModelComponent`
- **Purpose**: Provides ViewModel instances with proper scoping
- **Note**: ViewModels are automatically scoped to their respective components

#### `NavigationModule`
- **Scope**: `SingletonComponent`
- **Purpose**: Provides navigation-related dependencies
- **Dependencies**: `DeepLinkHandler`

## Scopes and Qualifiers

### Custom Scopes
- `@SessionScoped` - For user session-related dependencies
- `@FeatureScoped` - For feature-specific dependencies

### Qualifiers
- `@MainDatabase` - Primary app database
- `@MainViewModel`, `@DetailViewModel`, etc. - ViewModel qualifiers
- `@ApplicationScope`, `@ViewModelScope`, `@RepositoryScope` - Component qualifiers

## Architecture Integration

### BaseViewModel Integration
```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: ExampleRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val errorHandler: ErrorHandler
) : BaseViewModel<ExampleUiState>() {
    // Implementation
}
```

### Repository Integration
```kotlin
@Singleton
class ExampleRepositoryImpl @Inject constructor(
    private val dao: ExampleDao,
    private val apiService: ExampleApiService,
    private val dependencies: RepositoryDependencies
) : BaseRepositoryImpl<ExampleEntity, Long>(), ExampleRepository {
    // Implementation using dispatcherProvider and errorHandler from dependencies
}
```

### Error Handling Integration
All components receive `ErrorHandler` and `ErrorMessageProvider` through dependency injection, ensuring consistent error handling across the application.

## Testing

### Test Modules
For testing, create test-specific modules that replace production dependencies:

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreModule::class]
)
object TestCoreModule {
    @Provides
    @Singleton
    fun provideTestDispatcherProvider(): DispatcherProvider = TestDispatcherProvider()
}
```

## Usage Guidelines

1. **ViewModels**: Always use `@HiltViewModel` annotation
2. **Repositories**: Inject through interfaces, not concrete classes
3. **Error Handling**: Use injected `ErrorHandler` for consistent error processing
4. **Threading**: Use injected `DispatcherProvider` for thread switching
5. **Scoping**: Use appropriate scopes to avoid memory leaks and ensure proper lifecycle management

## Future Extensions

- Add use case layer with dedicated module
- Implement feature-specific modules for better separation
- Add analytics and logging dependencies
- Implement data source abstraction layer