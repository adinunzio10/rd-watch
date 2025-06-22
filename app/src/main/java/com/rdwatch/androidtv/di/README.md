# Dependency Injection - Custom Qualifiers and Scopes

This document describes the custom qualifiers and scopes implemented for the RD Watch Android TV application.

## Custom Qualifiers

### Network Qualifiers (`NetworkQualifiers.kt`)

- **@MainApi**: Used for the primary API endpoint that serves movie data
- **@CdnApi**: For content delivery network API that serves images and video content
- **@AnalyticsApi**: For analytics endpoints that track user interactions
- **@AuthenticatedClient**: OkHttpClient with authentication interceptors for secure endpoints
- **@PublicClient**: OkHttpClient for public endpoints that don't require authentication
- **@CachingClient**: OkHttpClient with caching enabled for media content

### Database Qualifiers (`DatabaseQualifiers.kt`)

- **@MainDatabase**: The primary application database for core app data
- **@CacheDatabase**: Temporary cache database for short-lived data
- **@UserDatabase**: Database for user-specific preferences and settings
- **@DownloadsDatabase**: Database for managing downloaded content metadata

### Repository Qualifiers (`RepositoryQualifiers.kt`)

- **@LocalRepository**: Repository implementations that work only with local data (offline mode)
- **@RemoteRepository**: Repository implementations that work only with remote APIs (online mode)
- **@CacheRepository**: Repository for managing temporary/cache data

## Custom Scopes

### TV-Specific Scopes (`TvScopes.kt`)

- **@PlaybackScoped**: Components live as long as a video playback session
  - Use for: Video player services, playback tracking, media session handlers
- **@BrowseScoped**: Components live as long as a content browsing session
  - Use for: Content discovery services, search managers, recommendation engines
- **@UserSessionScoped**: Components live as long as a user session
  - Use for: User preferences, authentication tokens, personalization data

## Usage Examples

### Injecting Different OkHttpClients

```kotlin
@Inject
@AuthenticatedClient
lateinit var authenticatedHttpClient: OkHttpClient

@Inject
@PublicClient
lateinit var publicHttpClient: OkHttpClient

@Inject
@CachingClient
lateinit var cachingHttpClient: OkHttpClient
```

### Using Different Databases

```kotlin
@Inject
@MainDatabase
lateinit var mainDatabase: AppDatabase

@Inject
@CacheDatabase
lateinit var cacheDatabase: CacheDatabase
```

### Repository Injection

```kotlin
@Inject
@LocalRepository
lateinit var localMovieRepository: MovieRepository

@Inject
@RemoteRepository
lateinit var remoteMovieRepository: MovieRepository
```

## Best Practices

1. **Use Qualifiers When You Have Multiple Instances**: Only use qualifiers when you need to distinguish between different instances of the same type.

2. **Keep Qualifier Names Descriptive**: Names should clearly indicate the purpose or configuration of the qualified dependency.

3. **Document Usage**: Always document what each qualifier is intended for and when to use it.

4. **Scope Appropriately**: Choose scopes that match the actual lifecycle of your components to avoid memory leaks.

5. **Avoid Over-Engineering**: Don't create qualifiers for dependencies that will only ever have one instance.

## Future Enhancements

- Add qualifiers for different video qualities (@HighQuality, @LowQuality)
- Create scopes for different TV input sources
- Add qualifiers for different analytics providers
- Implement qualifiers for A/B testing configurations