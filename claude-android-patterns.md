# Claude Android Patterns

Android TV app specific debugging patterns, common issues, and development best practices.

## Room Database Debugging

### Common Entity Mapping Issues

**Problem Pattern: Data Loss in Entity Conversion**

```kotlin
// ❌ WRONG: Loses data during conversion
fun TMDbTVResponse.toEntity(): TMDbTVEntity {
    return TMDbTVEntity(
        seasons = seasons.map { it.name }  // Only stores names!
    )
}

fun TMDbTVEntity.toTVResponse(): TMDbTVResponse {
    return TMDbTVResponse(
        seasons = seasons?.map { TMDbSeasonResponse(name = it) } ?: emptyList()
        // Creates objects with default values: id=0, episodes=[]
    )
}
```

**✅ CORRECT: Preserve essential data**

```kotlin
fun TMDbTVResponse.toEntity(): TMDbTVEntity {
    return TMDbTVEntity(
        seasons = seasons.map { "${it.id}:${it.seasonNumber}:${it.name}:${it.episodeCount}" }
    )
}

fun TMDbTVEntity.toTVResponse(): TMDbTVResponse {
    return TMDbTVResponse(
        seasons = seasons?.map { seasonString ->
            val parts = seasonString.split(":")
            if (parts.size >= 4) {
                TMDbSeasonResponse(
                    id = parts[0].toIntOrNull() ?: 0,
                    seasonNumber = parts[1].toIntOrNull() ?: 0,
                    name = parts[2],
                    episodeCount = parts[3].toIntOrNull() ?: 0
                )
            } else {
                TMDbSeasonResponse(name = seasonString) // Backward compatibility
            }
        } ?: emptyList()
    )
}
```

### Database Debugging Checklist

- [ ] Verify entity fields preserve essential data
- [ ] Check conversion functions for data loss
- [ ] Add logging to track save/load operations
- [ ] Test backward compatibility with existing data

## TMDb API Integration Patterns

### Proper API Call Logging

```kotlin
// Add comprehensive logging for API debugging
override fun getSeasonDetails(tvId: Int, seasonNumber: Int): Flow<Result<TMDbSeasonResponse>> =
    networkBoundResource(
        loadFromDb = {
            tmdbTVDao.getTVShowById(tvId).map { tvEntity ->
                val cachedSeason = tvEntity?.toTVResponse()?.seasons?.find { it.seasonNumber == seasonNumber }
                android.util.Log.d("TMDbRepository", "Loaded cached season $seasonNumber for TV $tvId: episodes=${cachedSeason?.episodes?.size ?: 0}")
                cachedSeason ?: TMDbSeasonResponse()
            }
        },
        shouldFetch = { cachedSeason ->
            val shouldFetch = forceRefresh || cachedSeason == null || cachedSeason.episodes.isEmpty()
            android.util.Log.d("TMDbRepository", "shouldFetch season $seasonNumber for TV $tvId: $shouldFetch")
            shouldFetch
        },
        createCall = {
            android.util.Log.d("TMDbRepository", "API CALL: Fetching season $seasonNumber details for TV $tvId")
            awaitApiResponse(tmdbTVService.getSeasonDetails(tvId, seasonNumber, language))
        },
        saveCallResult = { seasonResponse ->
            android.util.Log.d("TMDbRepository", "API RESULT: Saving season $seasonNumber for TV $tvId with ${seasonResponse.episodes.size} episodes")
            // Save logic...
        }
    )
```

### API Call Debugging Patterns

- Track the complete API lifecycle: Load → ShouldFetch → Call → Save
- Log input parameters and output data sizes
- Verify cache hit/miss logic with detailed conditions
- Monitor for duplicate or excessive API calls

## Coroutine and Flow Management

### Race Condition Prevention

```kotlin
@HiltViewModel
class TVDetailsViewModel @Inject constructor() : BaseViewModel<TVDetailsUiState>() {

    // ✅ Proper job management
    private var seasonLoadingJob: Job? = null
    private val onDemandSeasonJobs = mutableMapOf<Int, Job>()
    private val activeSeasonRequests = mutableSetOf<Int>()

    fun loadSeasonsWithEpisodes(tmdbId: Int, tvShowDetail: TVShowContentDetail) {
        // Cancel existing job to prevent race conditions
        seasonLoadingJob?.cancel()

        seasonLoadingJob = viewModelScope.launch {
            // Check for duplicate requests
            if (activeSeasonRequests.contains(seasonNumber)) {
                android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber already being loaded, skipping duplicate request")
                return@launch
            }

            activeSeasonRequests.add(seasonNumber)

            try {
                // Use timeout to prevent hanging
                val result = withTimeoutOrNull(30000) {
                    tmdbTVRepository.getSeasonDetails(tmdbId, seasonNumber)
                        .first { result ->
                            // Wait for actual results, not Loading states
                            result !is Result.Loading
                        }
                }
                // Handle result...
            } finally {
                activeSeasonRequests.remove(seasonNumber)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up jobs when ViewModel is destroyed
        seasonLoadingJob?.cancel()
        onDemandSeasonJobs.values.forEach { it.cancel() }
        onDemandSeasonJobs.clear()
        activeSeasonRequests.clear()
    }
}
```

### Flow Collection Best Practices

- Use `first { !is Result.Loading }` instead of `take(1)` for proper result waiting
- Implement request deduplication to prevent concurrent identical requests
- Add timeout protection with `withTimeoutOrNull()`
- Always clean up jobs in `onCleared()`

## Jetpack Compose State Management

### ViewModel State Synchronization

```kotlin
// ✅ Proper state updates that maintain selection
private fun updateTVShowWithSeasons(originalTvShow: TVShowContentDetail, seasons: List<TVSeason>) {
    val updatedTvShow = originalTvShow.withSeasons(seasons)
    _tvShowState.value = updatedTvShow

    // Maintain existing selection if possible
    val currentSeason = _selectedSeason.value
    if (currentSeason?.episodes?.isEmpty() == true) {
        val updatedSeason = seasons.find { it.seasonNumber == currentSeason.seasonNumber }
        if (updatedSeason?.episodes?.isNotEmpty() == true) {
            _selectedSeason.value = updatedSeason
            _selectedEpisode.value = updatedSeason.episodes.firstOrNull()
        }
    }

    updateState { copy(tvShow = updatedTvShow) }
}
```

### State Management Debugging

- Log state transitions with before/after values
- Verify state updates maintain UI consistency
- Check for state update loops or infinite recomposition
- Monitor selection state across data updates

## Common Android TV Debugging Scenarios

### 1. Data Not Loading

**Investigation Steps:**

1. Check API call logging for request/response patterns
2. Verify database entity mapping preserves necessary data
3. Look for race conditions in concurrent requests
4. Confirm shouldFetch logic correctly triggers API calls

### 2. UI Not Updating

**Investigation Steps:**

1. Verify ViewModel state updates trigger recomposition
2. Check for state update loops preventing UI refresh
3. Confirm StateFlow/LiveData subscriptions are active
4. Look for selection state not syncing with data updates

### 3. Performance Issues

**Investigation Steps:**

1. Monitor for excessive API calls or database queries
2. Check for memory leaks in job management
3. Verify proper cleanup in ViewModel.onCleared()
4. Look for inefficient recomposition patterns

## Debugging Tools and Commands

### Essential Gradle Commands

```bash
./gradlew compileDebugKotlin    # Quick compilation check
./ktlint-summary.sh             # KtLint format and check
./gradlew assembleDebug         # Full build verification
```

### Log Filtering Patterns

```bash

### Common Debugging Additions
```kotlin
// Temporary debugging for complex state issues
android.util.Log.d("DEBUG", "=== State Debug ===")
android.util.Log.d("DEBUG", "Current state: ${currentState}")
android.util.Log.d("DEBUG", "Expected: ${expectedState}")
android.util.Log.d("DEBUG", "Condition met: ${condition}")
```

This document serves as a reference for Android-specific debugging patterns and common issues encountered in Android TV app development.
