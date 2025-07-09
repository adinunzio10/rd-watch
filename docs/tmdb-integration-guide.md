# TMDb Integration Guide

This guide provides comprehensive documentation for the TMDb (The Movie Database) integration in the RD Watch Android TV application.

## Overview

The TMDb integration enhances the content browsing experience by providing rich metadata for movies and TV shows, including:

- Detailed movie and TV show information
- Cast and crew details
- High-quality images (posters, backdrops, logos)
- Ratings and reviews
- Recommendations and similar content
- Trailers and videos
- Search capabilities

## Architecture

### Service Layer

The TMDb integration uses dedicated service interfaces for different content types:

```kotlin
// Movie-specific operations
interface TMDbMovieService {
    fun getMovieDetails(movieId: Int): Call<ApiResponse<TMDbMovieResponse>>
    fun getMovieCredits(movieId: Int): Call<ApiResponse<TMDbCreditsResponse>>
    fun getMovieRecommendations(movieId: Int, page: Int): Call<ApiResponse<TMDbRecommendationsResponse>>
    fun getPopularMovies(page: Int): Call<ApiResponse<TMDbRecommendationsResponse>>
    // ... additional endpoints
}

// TV-specific operations  
interface TMDbTVService {
    fun getTVDetails(tvId: Int): Call<ApiResponse<TMDbTVResponse>>
    fun getTVCredits(tvId: Int): Call<ApiResponse<TMDbCreditsResponse>>
    fun getTVRecommendations(tvId: Int, page: Int): Call<ApiResponse<TMDbRecommendationsResponse>>
    // ... additional endpoints
}

// Search operations
interface TMDbSearchService {
    fun searchMovies(query: String, page: Int): Call<ApiResponse<TMDbSearchResponse>>
    fun searchTVShows(query: String, page: Int): Call<ApiResponse<TMDbSearchResponse>>
    fun multiSearch(query: String, page: Int): Call<ApiResponse<TMDbMultiSearchResponse>>
    // ... additional endpoints
}
```

### Repository Layer

The repository layer implements the NetworkBoundResource pattern for offline-first data access:

```kotlin
class TMDbMovieRepositoryImpl @Inject constructor(
    private val tmdbMovieService: TMDbMovieService,
    private val tmdbMovieDao: TMDbMovieDao,
    private val tmdbSearchDao: TMDbSearchDao,
    private val contentDetailMapper: TMDbToContentDetailMapper
) : TMDbMovieRepository {

    override fun getMovieDetails(movieId: Int, forceRefresh: Boolean): Flow<Result<TMDbMovieResponse>> =
        networkBoundResource(
            loadFromDb = { tmdbMovieDao.getMovieById(movieId).map { it?.toMovieResponse() } },
            shouldFetch = { cached -> forceRefresh || shouldRefreshCache(cached) },
            createCall = { tmdbMovieService.getMovieDetails(movieId) },
            saveCallResult = { response -> tmdbMovieDao.insertMovie(response.toEntity()) }
        )
}
```

### Database Layer

TMDb data is persisted using Room entities with comprehensive type converters:

```kotlin
@Entity(tableName = "tmdb_movies")
@TypeConverters(Converters::class)
data class TMDbMovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val originalTitle: String,
    val overview: String?,
    val releaseDate: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Float,
    val voteCount: Int,
    val popularity: Float,
    val genres: List<String>?,
    val runtime: Int?,
    val lastUpdated: Long = System.currentTimeMillis()
)
```

## API Configuration

### API Key Setup

1. **Obtain TMDb API Key**: Register at https://www.themoviedb.org/settings/api
2. **Configure Build**: Add to `local.properties`:
   ```
   TMDB_API_KEY=your_api_key_here
   ```
3. **Build Configuration**: Add to `build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "TMDB_API_KEY", "\"${project.findProperty("TMDB_API_KEY") ?: ""}\"")
   ```

### API Key Injection

The API key is automatically injected into all TMDb requests:

```kotlin
@Singleton
class TMDbApiKeyInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
            .build()
        
        return chain.proceed(original.newBuilder().url(url).build())
    }
}
```

### Network Configuration

TMDb requests use a dedicated OkHttp client with optimized settings:

```kotlin
@Provides
@Singleton
@TMDbApi
fun provideTMDbOkHttpClient(
    @ApplicationContext context: Context,
    tmdbApiKeyInterceptor: TMDbApiKeyInterceptor
): OkHttpClient {
    val cacheSize = 20 * 1024 * 1024L // 20MB cache
    val cacheDir = File(context.cacheDir, "tmdb_cache")
    val cache = Cache(cacheDir, cacheSize)
    
    return OkHttpClient.Builder()
        .addInterceptor(tmdbApiKeyInterceptor)
        .cache(cache)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
```

## Caching Strategy

### Cache Levels

1. **HTTP Cache**: OkHttp caches API responses (20MB)
2. **Database Cache**: Room entities cache structured data
3. **Memory Cache**: Repository-level caching for frequently accessed data

### Cache Expiration

Different content types have different cache expiration policies:

```kotlin
companion object {
    private const val MOVIE_CACHE_HOURS = 24
    private const val CREDITS_CACHE_HOURS = 24
    private const val SEARCH_CACHE_MINUTES = 30
    private const val POPULAR_CACHE_HOURS = 1
}

private fun shouldRefreshCache(contentId: Int, type: String): Boolean {
    val timeoutMs = when (type) {
        "movie" -> MOVIE_CACHE_HOURS * 60 * 60 * 1000L
        "credits" -> CREDITS_CACHE_HOURS * 60 * 60 * 1000L
        "search" -> SEARCH_CACHE_MINUTES * 60 * 1000L
        "popular" -> POPULAR_CACHE_HOURS * 60 * 60 * 1000L
        else -> 0L
    }
    
    val lastUpdated = getLastUpdated(contentId, type)
    return lastUpdated?.let { 
        System.currentTimeMillis() - it > timeoutMs 
    } ?: true
}
```

### Cache Management

```kotlin
// Clear all TMDb cache
suspend fun clearCache() {
    tmdbMovieDao.deleteAllMovies()
    tmdbSearchDao.deleteAllSearchResults()
    tmdbSearchDao.deleteAllCredits()
}

// Clear specific content cache
suspend fun clearMovieCache(movieId: Int) {
    tmdbMovieDao.deleteMovieById(movieId)
    tmdbSearchDao.deleteCredits(movieId, "movie")
    tmdbSearchDao.deleteRecommendations(movieId, "movie")
}
```

## ContentDetail Integration

### TMDb ContentDetail Implementation

TMDb data integrates seamlessly with the ContentDetail system:

```kotlin
data class TMDbMovieContentDetail(
    private val tmdbMovie: TMDbMovieResponse,
    private val credits: TMDbCreditsResponse? = null,
    private val progress: ContentProgress = ContentProgress()
) : ContentDetail {
    
    override val id: String = tmdbMovie.id.toString()
    override val title: String = tmdbMovie.title
    override val description: String? = tmdbMovie.overview
    override val backgroundImageUrl: String? = buildBackdropUrl(tmdbMovie.backdropPath)
    override val cardImageUrl: String? = buildPosterUrl(tmdbMovie.posterPath)
    override val contentType: ContentType = ContentType.MOVIE
    
    override val metadata: ContentMetadata = ContentMetadata(
        year = extractYear(tmdbMovie.releaseDate),
        duration = formatRuntime(tmdbMovie.runtime),
        rating = formatRating(tmdbMovie.voteAverage),
        genre = tmdbMovie.genres.map { it.name },
        cast = credits?.cast?.take(5)?.map { it.name } ?: emptyList(),
        director = credits?.crew?.firstOrNull { it.job == "Director" }?.name,
        studio = tmdbMovie.productionCompanies.firstOrNull()?.name
    )
    
    override val actions: List<ContentAction> = buildActions()
}
```

### Image URL Construction

TMDb images use dynamic URLs with configurable sizes:

```kotlin
object TMDbImageUrls {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"
    
    // TV-optimized sizes
    const val BACKDROP_SIZE = "w1280"
    const val POSTER_SIZE = "w500"
    const val PROFILE_SIZE = "w185"
    
    fun buildBackdropUrl(path: String?): String? {
        return path?.let { "$BASE_URL$BACKDROP_SIZE$it" }
    }
    
    fun buildPosterUrl(path: String?): String? {
        return path?.let { "$BASE_URL$POSTER_SIZE$it" }
    }
    
    fun buildProfileUrl(path: String?): String? {
        return path?.let { "$BASE_URL$PROFILE_SIZE$it" }
    }
}
```

## Data Transformation

### Entity Mapping

Data transformation between API responses and database entities:

```kotlin
// API Response to Entity
fun TMDbMovieResponse.toEntity(): TMDbMovieEntity {
    return TMDbMovieEntity(
        id = id,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        releaseDate = releaseDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toFloat(),
        voteCount = voteCount,
        popularity = popularity.toFloat(),
        genres = genres.map { it.name },
        runtime = runtime,
        lastUpdated = System.currentTimeMillis()
    )
}

// Entity to API Response
fun TMDbMovieEntity.toMovieResponse(): TMDbMovieResponse {
    return TMDbMovieResponse(
        id = id,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        releaseDate = releaseDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage.toDouble(),
        voteCount = voteCount,
        popularity = popularity.toDouble(),
        genres = genres?.map { TMDbGenreResponse(name = it) } ?: emptyList(),
        runtime = runtime
    )
}
```

### ContentDetail Mapping

TMDb responses are mapped to ContentDetail implementations:

```kotlin
@Singleton
class TMDbToContentDetailMapper @Inject constructor() {
    
    fun mapMovieToContentDetail(
        movie: TMDbMovieResponse,
        credits: TMDbCreditsResponse? = null,
        progress: ContentProgress = ContentProgress()
    ): ContentDetail {
        return TMDbMovieContentDetail(
            tmdbMovie = movie,
            credits = credits,
            progress = progress
        )
    }
    
    fun mapTVToContentDetail(
        tvShow: TMDbTVResponse,
        credits: TMDbCreditsResponse? = null,
        progress: ContentProgress = ContentProgress()
    ): ContentDetail {
        return TMDbTVContentDetail(
            tmdbTVShow = tvShow,
            credits = credits,
            progress = progress
        )
    }
}
```

## Usage Examples

### Basic Movie Details

```kotlin
@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val tmdbMovieRepository: TMDbMovieRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState = _uiState.asStateFlow()
    
    fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {
            tmdbMovieRepository.getMovieContentDetail(movieId)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                contentDetail = result.data,
                                isLoading = false
                            )
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = result.exception.message,
                                isLoading = false
                            )
                        }
                        is Result.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                    }
                }
        }
    }
    
    fun loadCredits(movieId: Int) {
        viewModelScope.launch {
            tmdbMovieRepository.getMovieCredits(movieId)
                .collect { result ->
                    if (result is Result.Success) {
                        _uiState.value = _uiState.value.copy(
                            credits = result.data
                        )
                    }
                }
        }
    }
}
```

### Search Integration

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tmdbSearchRepository: TMDbSearchRepository
) : ViewModel() {
    
    private val _searchResults = MutableStateFlow<List<ContentDetail>>(emptyList())
    val searchResults = _searchResults.asStateFlow()
    
    fun searchContent(query: String) {
        viewModelScope.launch {
            tmdbSearchRepository.searchMovies(query)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val contentDetails = result.data.results.map { searchResult ->
                                TMDbSearchResultContentDetail(searchResult)
                            }
                            _searchResults.value = contentDetails
                        }
                        is Result.Error -> {
                            // Handle error
                        }
                        is Result.Loading -> {
                            // Show loading
                        }
                    }
                }
        }
    }
}
```

### Recommendations

```kotlin
fun loadRecommendations(movieId: Int) {
    viewModelScope.launch {
        tmdbMovieRepository.getMovieRecommendations(movieId, page = 1)
            .collect { result ->
                when (result) {
                    is Result.Success -> {
                        val recommendations = result.data.results.map { movie ->
                            TMDbMovieContentDetail(movie)
                        }
                        _uiState.value = _uiState.value.copy(
                            recommendations = recommendations
                        )
                    }
                    // Handle other states
                }
            }
    }
}
```

## Error Handling

### Network Error Handling

```kotlin
private fun handleApiError(throwable: Throwable): String {
    return when (throwable) {
        is HttpException -> when (throwable.code()) {
            401 -> "Invalid API key"
            404 -> "Content not found"
            429 -> "Rate limit exceeded"
            else -> "Network error: ${throwable.message()}"
        }
        is IOException -> "Network connection error"
        else -> "Unknown error: ${throwable.message}"
    }
}
```

### Graceful Degradation

```kotlin
networkBoundResource(
    loadFromDb = { tmdbMovieDao.getMovieById(movieId) },
    shouldFetch = { cached -> shouldRefreshCache(cached) },
    createCall = { tmdbMovieService.getMovieDetails(movieId) },
    saveCallResult = { response -> tmdbMovieDao.insertMovie(response.toEntity()) },
    onFetchFailed = { throwable ->
        // Log error but continue with cached data
        Log.e("TMDb", "Failed to fetch movie details", throwable)
    }
)
```

## Performance Optimization

### Image Loading

```kotlin
// Glide integration for TMDb images
@Composable
fun TMDbImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
```

### Pagination

```kotlin
fun loadNextPage(movieId: Int) {
    val currentPage = _uiState.value.currentPage
    val hasMorePages = currentPage < _uiState.value.totalPages
    
    if (hasMorePages && !_uiState.value.isLoadingMore) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            tmdbMovieRepository.getMovieRecommendations(movieId, currentPage + 1)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val newRecommendations = result.data.results.map { movie ->
                                TMDbMovieContentDetail(movie)
                            }
                            _uiState.value = _uiState.value.copy(
                                recommendations = _uiState.value.recommendations + newRecommendations,
                                currentPage = currentPage + 1,
                                isLoadingMore = false
                            )
                        }
                        // Handle other states
                    }
                }
        }
    }
}
```

## Testing

### Repository Testing

```kotlin
@Test
fun `getMovieDetails returns cached data when available`() = runTest {
    // Given
    val movieId = 123
    val cachedMovie = TMDbTestDataFactory.createTMDbMovieEntity(id = movieId)
    every { mockTmdbMovieDao.getMovieById(movieId) } returns flowOf(cachedMovie)
    every { mockTmdbMovieDao.getMovieLastUpdated(movieId) } returns System.currentTimeMillis()
    
    // When
    val result = repository.getMovieDetails(movieId).first()
    
    // Then
    assertTrue(result is Result.Success)
    assertEquals(movieId, result.data.id)
    verify(exactly = 0) { mockTmdbMovieService.getMovieDetails(any(), any(), any()) }
}
```

### ContentDetail Testing

```kotlin
@Test
fun `TMDbMovieContentDetail formats metadata correctly`() {
    // Given
    val tmdbMovie = TMDbTestDataFactory.createTMDbMovieResponse(
        id = 123,
        title = "Test Movie",
        runtime = 120,
        voteAverage = 8.5,
        releaseDate = "2023-01-15"
    )
    
    // When
    val contentDetail = TMDbMovieContentDetail(tmdbMovie)
    
    // Then
    assertEquals("Test Movie", contentDetail.title)
    assertEquals("2h 0m", contentDetail.metadata.duration)
    assertEquals("8.5", contentDetail.metadata.rating)
    assertEquals("2023", contentDetail.metadata.year)
}
```

## Troubleshooting

### Common Issues

1. **API Key Issues**
   - Verify API key is correctly configured in `local.properties`
   - Check if key is properly injected in requests
   - Validate key permissions on TMDb website

2. **Network Issues**
   - Check internet connectivity
   - Verify TMDb API status
   - Review rate limiting (40 requests per 10 seconds)

3. **Cache Issues**
   - Clear app cache if stale data persists
   - Use `forceRefresh = true` to bypass cache
   - Check cache expiration settings

4. **Image Loading Issues**
   - Verify image URLs are correctly constructed
   - Check network connectivity for image downloads
   - Validate image sizes for TV screens

### Debug Tools

```kotlin
// Enable detailed logging
class TMDbDebugInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        Log.d("TMDb", "Request: ${request.url}")
        
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()
        
        Log.d("TMDb", "Response: ${response.code} (${endTime - startTime}ms)")
        
        return response
    }
}
```

## Migration Guide

### From Static Data to TMDb

1. **Identify Static Content**: Locate hardcoded movie/TV data
2. **Map to TMDb IDs**: Find corresponding TMDb IDs for existing content
3. **Update Data Models**: Replace static models with TMDb entities
4. **Implement Repositories**: Add TMDb repository implementations
5. **Update UI Components**: Modify screens to use ContentDetail
6. **Test Integration**: Verify all functionality works with TMDb data

### ContentDetail Migration

```kotlin
// Before: Static movie data
data class StaticMovie(
    val id: String,
    val title: String,
    val description: String
)

// After: TMDb ContentDetail
val tmdbContentDetail = TMDbMovieContentDetail(
    tmdbMovie = tmdbMovieResponse,
    credits = tmdbCreditsResponse
)
```

This comprehensive guide covers all aspects of the TMDb integration, from basic usage to advanced optimization techniques. Use it as a reference when working with TMDb data in the application.